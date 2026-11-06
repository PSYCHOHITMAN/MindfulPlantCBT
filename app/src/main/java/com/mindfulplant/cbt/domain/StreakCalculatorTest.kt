package com.mindfulplant.cbt.domain

import com.mindfulplant.cbt.data.local.entity.ThoughtRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for StreakCalculator. Pure Kotlin, so they run on the plain JVM
 * in GitHub Actions (see .github/workflows/android-ci.yml) - no emulator needed.
 *
 * Entries are created relative to "now" in whole-day steps, which is the same
 * unit StreakCalculator buckets on (createdAt / 86_400_000).
 */
class StreakCalculatorTest {

    private val dayMillis = 86_400_000L

    private fun record(daysAgo: Int, id: String = "rec-$daysAgo") =
        ThoughtRecordEntity(
            recordId = id,
            userId = "user-1",
            situation = "test situation",
            automaticThought = "test thought",
            distortionType = "catastrophising",
            balancedReframe = "test reframe",
            moodBefore = 2,
            moodAfter = 3,
            createdAt = System.currentTimeMillis() - daysAgo * dayMillis
        )

    private fun records(vararg daysAgo: Int) = daysAgo.mapIndexed { i, d -> record(d, "rec-$i-$d") }

    @Test
    fun `no entries gives a streak of 0`() {
        assertEquals(0, StreakCalculator.calculate(emptyList()))
    }

    @Test
    fun `a single entry today gives a streak of 1`() {
        assertEquals(1, StreakCalculator.calculate(records(0)))
    }

    @Test
    fun `a single entry yesterday still gives a streak of 1`() {
        assertEquals(1, StreakCalculator.calculate(records(1)))
    }

    @Test
    fun `latest entry two days ago means the streak has lapsed`() {
        assertEquals(0, StreakCalculator.calculate(records(2, 3, 4)))
    }

    @Test
    fun `five consecutive days ending today gives 5`() {
        assertEquals(5, StreakCalculator.calculate(records(0, 1, 2, 3, 4)))
    }

    @Test
    fun `consecutive days ending yesterday are still counted`() {
        assertEquals(3, StreakCalculator.calculate(records(1, 2, 3)))
    }

    @Test
    fun `a missed day ends the streak at the gap`() {
        // today, yesterday, 2 days ago, then a gap (3 days ago missing), then older entries
        assertEquals(3, StreakCalculator.calculate(records(0, 1, 2, 4, 5)))
    }

    @Test
    fun `several entries on the same day count as one day`() {
        assertEquals(2, StreakCalculator.calculate(records(0, 0, 0, 1, 1)))
    }

    @Test
    fun `input order does not matter`() {
        val shuffled = records(3, 0, 2, 1)
        assertEquals(4, StreakCalculator.calculate(shuffled))
    }

    @Test
    fun `old entries with a gap before today do not create a streak`() {
        // Entry today, then nothing until 10 days ago: the streak is just today.
        assertEquals(1, StreakCalculator.calculate(records(0, 10, 11, 12)))
    }
}
