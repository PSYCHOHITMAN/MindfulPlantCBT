package com.mindfulplant.cbt.domain

import com.mindfulplant.cbt.data.local.entity.ThoughtRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the rule boundaries in InsightsEngine that InsightsEngineTest doesn't:
 * the +/-0.3 threshold, the message chosen for each outcome, the persistent
 * decline rule (three falling windows), and which entries feed the stat tiles.
 *
 * Records are passed most-recent-first, as the engine expects.
 */
class InsightsEngineRulesTest {

    private val dayMillis = 86_400_000L

    private fun record(
        moodAfter: Int,
        daysAgo: Int,
        moodBefore: Int = 2,
        distortion: String = "catastrophising"
    ) = ThoughtRecordEntity(
        recordId = "rec-$daysAgo-$moodAfter-$distortion",
        userId = "user-1",
        situation = "test situation",
        automaticThought = "test thought",
        distortionType = distortion,
        balancedReframe = "test reframe",
        moodBefore = moodBefore,
        moodAfter = moodAfter,
        createdAt = System.currentTimeMillis() - daysAgo * dayMillis
    )

    /** One window of 7 entries, starting [startDay] days ago, with the given moodAfter values. */
    private fun window(startDay: Int, vararg moods: Int) =
        moods.mapIndexed { i, mood -> record(mood, startDay + i) }

    private fun flatWindow(startDay: Int, mood: Int) = window(startDay, *IntArray(7) { mood })

    // --- not enough data -------------------------------------------------

    @Test
    fun `no entries returns NOT_ENOUGH_DATA with empty stats`() {
        val result = InsightsEngine.analyse(emptyList())

        assertEquals(Trend.NOT_ENOUGH_DATA, result.trend)
        assertEquals(InsightMessage.NOT_ENOUGH_DATA, result.message)
        assertNull(result.mostLoggedDistortion)
        assertEquals(0, result.mostLoggedDistortionCount)
        assertNull(result.avgMoodAfter)
        assertNull(result.avgMoodLift)
        assertEquals(0, result.streak)
        assertEquals(0, result.analysedCount)
    }

    @Test
    fun `three entries still report averages even though no trend is shown`() {
        val records = listOf(
            record(moodAfter = 5, daysAgo = 0, moodBefore = 2),
            record(moodAfter = 4, daysAgo = 1, moodBefore = 2),
            record(moodAfter = 3, daysAgo = 2, moodBefore = 3)
        )

        val result = InsightsEngine.analyse(records)

        assertEquals(Trend.NOT_ENOUGH_DATA, result.trend)
        assertEquals(4.0, result.avgMoodAfter!!, 0.0001)
        // lifts are 3, 2 and 0 -> average 5/3
        assertEquals(5.0 / 3.0, result.avgMoodLift!!, 0.0001)
        assertEquals(3, result.analysedCount)
        assertNull(result.mostLoggedDistortion)
    }

    // --- first read, no previous window ---------------------------------

    @Test
    fun `exactly four entries gives BUILDING_HABIT`() {
        val records = (0..3).map { record(moodAfter = 3, daysAgo = it) }

        val result = InsightsEngine.analyse(records)

        assertEquals(Trend.STEADY, result.trend)
        assertEquals(InsightMessage.BUILDING_HABIT, result.message)
        assertEquals(4, result.analysedCount)
    }

    @Test
    fun `a full window of seven with nothing before it is still BUILDING_HABIT`() {
        val result = InsightsEngine.analyse(flatWindow(startDay = 0, mood = 4))

        assertEquals(InsightMessage.BUILDING_HABIT, result.message)
        assertEquals(7, result.analysedCount)
    }

    // --- +/-0.3 threshold -------------------------------------------------

    @Test
    fun `an improvement of 3 over 7 entries (about 0 point 43) is UP and IMPROVED`() {
        val recent = window(0, 4, 4, 4, 3, 3, 3, 3) // avg 24/7, delta 3/7 over the previous window
        val previous = flatWindow(7, 3)

        val result = InsightsEngine.analyse(recent + previous)

        assertEquals(Trend.UP, result.trend)
        assertEquals(InsightMessage.IMPROVED, result.message)
    }

    @Test
    fun `a rise of 2 over 7 entries (about 0 point 29) is below the threshold and STEADY`() {
        val recent = window(0, 4, 4, 3, 3, 3, 3, 3) // avg 23/7, delta 2/7
        val previous = flatWindow(7, 3)

        val result = InsightsEngine.analyse(recent + previous)

        assertEquals(Trend.STEADY, result.trend)
        assertEquals(InsightMessage.STEADY, result.message)
    }

    @Test
    fun `a fall of 2 over 7 entries (about 0 point 29) is below the threshold and STEADY`() {
        val recent = window(0, 2, 2, 3, 3, 3, 3, 3)
        val previous = flatWindow(7, 3)

        val result = InsightsEngine.analyse(recent + previous)

        assertEquals(Trend.STEADY, result.trend)
        assertEquals(InsightMessage.STEADY, result.message)
    }

    @Test
    fun `a fall of 3 over 7 entries (about 0 point 43) is DOWN`() {
        val recent = window(0, 2, 2, 2, 3, 3, 3, 3)
        val previous = flatWindow(7, 3)

        val result = InsightsEngine.analyse(recent + previous)

        assertEquals(Trend.DOWN, result.trend)
    }

    // --- DECLINE vs PERSISTENT_DECLINE -----------------------------------

    @Test
    fun `a drop with only two windows of history is DECLINE not PERSISTENT_DECLINE`() {
        val recent = flatWindow(0, 2)
        val previous = flatWindow(7, 4)

        val result = InsightsEngine.analyse(recent + previous)

        assertEquals(Trend.DOWN, result.trend)
        assertEquals(InsightMessage.DECLINE, result.message)
    }

    @Test
    fun `three falling windows give PERSISTENT_DECLINE`() {
        val records = flatWindow(0, 2) + flatWindow(7, 3) + flatWindow(14, 4)

        val result = InsightsEngine.analyse(records)

        assertEquals(Trend.DOWN, result.trend)
        assertEquals(InsightMessage.PERSISTENT_DECLINE, result.message)
    }

    @Test
    fun `a drop that follows a rise is DECLINE because the three windows are not all falling`() {
        // newest window 2, middle window 4, oldest window 3: dropped recently, but not for three windows running
        val records = flatWindow(0, 2) + flatWindow(7, 4) + flatWindow(14, 3)

        val result = InsightsEngine.analyse(records)

        assertEquals(Trend.DOWN, result.trend)
        assertEquals(InsightMessage.DECLINE, result.message)
    }

    // --- what feeds the stat tiles ---------------------------------------

    @Test
    fun `averages and distortion only use the most recent seven entries`() {
        val recent = (0..6).map { record(moodAfter = 4, daysAgo = it, moodBefore = 2, distortion = "mind-reading") }
        val older = (7..13).map { record(moodAfter = 1, daysAgo = it, moodBefore = 1, distortion = "catastrophising") }

        val result = InsightsEngine.analyse(recent + older)

        assertEquals(7, result.analysedCount)
        assertEquals(4.0, result.avgMoodAfter!!, 0.0001)
        assertEquals(2.0, result.avgMoodLift!!, 0.0001)
        assertEquals("mind-reading", result.mostLoggedDistortion)
        assertEquals(7, result.mostLoggedDistortionCount)
    }

    @Test
    fun `the result carries the current day streak`() {
        val records = (0..3).map { record(moodAfter = 3, daysAgo = it) }

        val result = InsightsEngine.analyse(records)

        assertEquals(4, result.streak)
    }
}
