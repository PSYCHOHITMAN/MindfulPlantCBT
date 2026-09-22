package com.mindfulplant.cbt.domain

import com.mindfulplant.cbt.data.local.entity.ThoughtRecordEntity

enum class Trend { UP, DOWN, STEADY, NOT_ENOUGH_DATA }

data class InsightsResult(
    val trend: Trend,
    val message: String,
    val mostLoggedDistortion: String?,
    val mostLoggedDistortionCount: Int,
    val distortionTip: String?,
    /** Average mood-after across the analysed window, out of 5. Null if no entries. */
    val avgMoodAfter: Double?,
    /** Average (moodAfter - moodBefore) across the analysed window - the mood "lift" from doing the exercise itself. */
    val avgMoodLift: Double?,
    val streak: Int,
    /** How many entries the stats above are based on (up to WINDOW_SIZE). */
    val analysedCount: Int
)

/**
 * Rule-based logic for the Insights screen (Planning and Design, Section
 * 9.1). Deliberately simple and fully offline/on-device - no machine
 * learning or external analysis service:
 *
 *  - Compares the average moodAfter of the last 7 entries against the
 *    7 before that. A change of +/- 0.3 or more counts as a trend.
 *  - Needs at least 4 entries before it will show a trend at all.
 *  - Also reports whichever cognitive distortion was logged most often
 *    in the most recent 7 entries, plus a short CBT-style prompt for it.
 *  - avgMoodAfter / avgMoodLift / streak feed the Insights stat tiles.
 *
 * This is a pure function (no Android framework dependencies), which is
 * exactly what makes it easy to unit test - see InsightsEngineTest.
 */
object InsightsEngine {

    private const val TREND_THRESHOLD = 0.3
    private const val MIN_ENTRIES_FOR_TREND = 4
    private const val WINDOW_SIZE = 7

    private val DISTORTION_TIPS = mapOf(
        "Catastrophising" to "Ask yourself: what's the most likely outcome, not just the worst one?",
        "All-or-nothing thinking" to "Look for the middle ground — few things are purely good or bad.",
        "Mind-reading" to "When you catch yourself assuming others' thoughts, ask: what's the actual evidence?",
        "Overgeneralising" to "One event doesn't set the rule — look for the exceptions.",
        "Emotional reasoning" to "Feeling it doesn't make it true — check the facts separately.",
        "Should statements" to "Try swapping 'should' for 'it would be nice if' — notice the pressure ease.",
        "Personalising" to "Ask what else, outside you, might explain what happened."
    )

    /** [records] must already be sorted most-recent-first. */
    fun analyse(records: List<ThoughtRecordEntity>): InsightsResult {
        val streak = StreakCalculator.calculate(records)

        if (records.size < MIN_ENTRIES_FOR_TREND) {
            return InsightsResult(
                trend = Trend.NOT_ENOUGH_DATA,
                message = "Keep journaling to see your trend - a few more entries and we'll show you how you're doing.",
                mostLoggedDistortion = null,
                mostLoggedDistortionCount = 0,
                distortionTip = null,
                avgMoodAfter = records.takeIf { it.isNotEmpty() }?.map { it.moodAfter }?.average(),
                avgMoodLift = records.takeIf { it.isNotEmpty() }?.map { (it.moodAfter - it.moodBefore).toDouble() }?.average(),
                streak = streak,
                analysedCount = records.size
            )
        }

        val recentWindow = records.take(WINDOW_SIZE)
        val previousWindow = records.drop(WINDOW_SIZE).take(WINDOW_SIZE)

        val recentAvg = recentWindow.map { it.moodAfter }.average()
        val trend: Trend
        val message: String

        if (previousWindow.isEmpty()) {
            // Not enough history yet for a comparison, but enough for a first read.
            trend = Trend.STEADY
            message = "You're building a great journaling habit. Keep it up!"
        } else {
            val previousAvg = previousWindow.map { it.moodAfter }.average()
            val delta = recentAvg - previousAvg

            when {
                delta >= TREND_THRESHOLD -> {
                    trend = Trend.UP
                    message = "Your mood has improved this week - keep up your journaling streak!"
                }
                delta <= -TREND_THRESHOLD -> {
                    trend = Trend.DOWN
                    message = if (isPersistentDecline(records)) {
                        "Things have felt heavier for a couple of weeks now. Be kind to yourself, and consider talking to a counsellor or someone you trust."
                    } else {
                        "Things have felt a bit heavier lately. Be kind to yourself, and consider talking to someone you trust."
                    }
                }
                else -> {
                    trend = Trend.STEADY
                    message = "Your mood has been fairly steady this week."
                }
            }
        }

        val (distortion, count) = mostLoggedDistortion(recentWindow)
        val avgMoodAfter = recentWindow.map { it.moodAfter }.average()
        val avgMoodLift = recentWindow.map { (it.moodAfter - it.moodBefore).toDouble() }.average()

        return InsightsResult(
            trend = trend,
            message = message,
            mostLoggedDistortion = distortion,
            mostLoggedDistortionCount = count,
            distortionTip = distortion?.let { DISTORTION_TIPS[it] },
            avgMoodAfter = avgMoodAfter,
            avgMoodLift = avgMoodLift,
            streak = streak,
            analysedCount = recentWindow.size
        )
    }

    /** True if the last 14 entries also show a downward trend, not just the last 7. */
    private fun isPersistentDecline(records: List<ThoughtRecordEntity>): Boolean {
        if (records.size < WINDOW_SIZE * 3) return false
        val window1 = records.take(WINDOW_SIZE).map { it.moodAfter }.average()
        val window2 = records.drop(WINDOW_SIZE).take(WINDOW_SIZE).map { it.moodAfter }.average()
        val window3 = records.drop(WINDOW_SIZE * 2).take(WINDOW_SIZE).map { it.moodAfter }.average()
        return window1 < window2 && window2 < window3
    }

    private fun mostLoggedDistortion(window: List<ThoughtRecordEntity>): Pair<String?, Int> {
        val counts = window.groupingBy { it.distortionType }.eachCount()
        val top = counts.maxByOrNull { it.value } ?: return null to 0
        return top.key to top.value
    }
}
