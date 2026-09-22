package com.mindfulplant.cbt.domain

import com.mindfulplant.cbt.data.local.entity.ThoughtRecordEntity

/**
 * Consecutive-day streak calculation (Planning and Design, Section 8.2:
 * "Streak counter"). Pulled out into its own object so Home and Insights
 * both compute the exact same number from the same records, instead of
 * two copies of the same logic drifting apart.
 */
object StreakCalculator {

    private const val DAY_IN_MILLIS = 86_400_000L

    /** [records] does not need to be pre-sorted. */
    fun calculate(records: List<ThoughtRecordEntity>): Int {
        if (records.isEmpty()) return 0

        val entryDays = records
            .map { it.createdAt / DAY_IN_MILLIS }
            .toSortedSet()
            .reversed()

        val today = System.currentTimeMillis() / DAY_IN_MILLIS
        if (entryDays.first() != today && entryDays.first() != today - 1) return 0

        var streak = 1
        var expectedDay = entryDays.first() - 1
        for (day in entryDays.drop(1)) {
            if (day == expectedDay) {
                streak++
                expectedDay--
            } else if (day < expectedDay) {
                break
            }
        }
        return streak
    }
}
