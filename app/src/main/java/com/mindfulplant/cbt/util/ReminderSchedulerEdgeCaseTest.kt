package com.mindfulplant.cbt.util

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

/** Extra edge cases for ReminderScheduler.millisUntilNext, alongside ReminderSchedulerTest. */
class ReminderSchedulerEdgeCaseTest {

    private val minute = 60 * 1000L

    private fun at(hour: Int, minute: Int) = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 3, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }

    @Test
    fun oneMinuteBeforeTarget_returnsOneMinute() {
        assertEquals(1 * minute, ReminderScheduler.millisUntilNext(20, 0, at(19, 59)))
    }

    @Test
    fun oneMinuteAfterTarget_returnsAlmostADay() {
        assertEquals((24 * 60 - 1) * minute, ReminderScheduler.millisUntilNext(20, 0, at(20, 1)))
    }

    @Test
    fun midnightTargetFromJustBeforeMidnight_returnsOneMinute() {
        assertEquals(1 * minute, ReminderScheduler.millisUntilNext(0, 0, at(23, 59)))
    }

    @Test
    fun doesNotChangeTheCalendarPassedIn() {
        val now = at(18, 30)
        val before = now.timeInMillis

        ReminderScheduler.millisUntilNext(20, 0, now)

        assertEquals(before, now.timeInMillis)
    }
}
