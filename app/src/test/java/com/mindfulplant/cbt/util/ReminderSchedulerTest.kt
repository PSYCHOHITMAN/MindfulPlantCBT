package com.mindfulplant.cbt.util

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSchedulerTest {

    private fun at(hour: Int, minute: Int) = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 3, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }

    @Test
    fun laterToday_returnsDelayUntilTodaysTime() {
        val delay = ReminderScheduler.millisUntilNext(20, 0, at(18, 30))
        assertEquals(90 * 60 * 1000L, delay)
    }

    @Test
    fun timeAlreadyPassed_returnsDelayUntilTomorrow() {
        val delay = ReminderScheduler.millisUntilNext(8, 0, at(9, 0))
        assertEquals(23 * 60 * 60 * 1000L, delay)
    }

    @Test
    fun exactlyNow_schedulesForTomorrow() {
        val delay = ReminderScheduler.millisUntilNext(20, 0, at(20, 0))
        assertEquals(24 * 60 * 60 * 1000L, delay)
    }
}
