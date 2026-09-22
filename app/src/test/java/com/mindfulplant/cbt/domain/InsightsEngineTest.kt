package com.mindfulplant.cbt.domain

import com.mindfulplant.cbt.data.local.entity.ThoughtRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for InsightsEngine. These run automatically in GitHub Actions
 * on every push (see .github/workflows/android-ci.yml), and don't need an
 * emulator or any Android framework classes - InsightsEngine is pure Kotlin.
 */
class InsightsEngineTest {

    private fun record(moodAfter: Int, distortion: String = "catastrophising", daysAgo: Int = 0) =
        ThoughtRecordEntity(
            recordId = "rec-$daysAgo-$moodAfter-$distortion",
            userId = "user-1",
            situation = "test situation",
            automaticThought = "test thought",
            distortionType = distortion,
            balancedReframe = "test reframe",
            moodBefore = 2,
            moodAfter = moodAfter,
            createdAt = System.currentTimeMillis() - daysAgo * 86_400_000L
        )

    @Test
    fun `fewer than 4 entries returns NOT_ENOUGH_DATA`() {
        val records = listOf(record(3, daysAgo = 0), record(3, daysAgo = 1))

        val result = InsightsEngine.analyse(records)

        assertEquals(Trend.NOT_ENOUGH_DATA, result.trend)
    }

    @Test
    fun `higher recent mood scores than previous window returns UP`() {
        // Most recent 7 entries average mood 4, previous 7 average mood 2.
        val recent = (0..6).map { record(moodAfter = 4, daysAgo = it) }
        val previous = (7..13).map { record(moodAfter = 2, daysAgo = it) }

        val result = InsightsEngine.analyse(recent + previous)

        assertEquals(Trend.UP, result.trend)
    }

    @Test
    fun `lower recent mood scores than previous window returns DOWN`() {
        val recent = (0..6).map { record(moodAfter = 2, daysAgo = it) }
        val previous = (7..13).map { record(moodAfter = 4, daysAgo = it) }

        val result = InsightsEngine.analyse(recent + previous)

        assertEquals(Trend.DOWN, result.trend)
    }

    @Test
    fun `similar mood scores across windows returns STEADY`() {
        val recent = (0..6).map { record(moodAfter = 3, daysAgo = it) }
        val previous = (7..13).map { record(moodAfter = 3, daysAgo = it) }

        val result = InsightsEngine.analyse(recent + previous)

        assertEquals(Trend.STEADY, result.trend)
    }

    @Test
    fun `most frequent distortion in recent window is identified correctly`() {
        val records = listOf(
            record(moodAfter = 3, distortion = "catastrophising", daysAgo = 0),
            record(moodAfter = 3, distortion = "catastrophising", daysAgo = 1),
            record(moodAfter = 3, distortion = "mind-reading", daysAgo = 2),
            record(moodAfter = 3, distortion = "catastrophising", daysAgo = 3),
        )

        val result = InsightsEngine.analyse(records)

        assertEquals("catastrophising", result.mostLoggedDistortion)
        assertEquals(3, result.mostLoggedDistortionCount)
    }
}
