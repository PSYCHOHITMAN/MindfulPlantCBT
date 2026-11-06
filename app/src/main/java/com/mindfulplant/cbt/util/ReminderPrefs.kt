package com.mindfulplant.cbt.util

import android.content.Context

/**
 * Persists the user's chosen daily-reminder time across app restarts, so
 * both the Settings screen and the Home stat pill can show the real saved
 * value rather than a hardcoded default. The actual notification is
 * scheduled by [ReminderScheduler] (WorkManager) using these values.
 */
class ReminderPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)

    var hour: Int
        get() = prefs.getInt(KEY_HOUR, DEFAULT_HOUR)
        set(value) = prefs.edit().putInt(KEY_HOUR, value).apply()

    var minute: Int
        get() = prefs.getInt(KEY_MINUTE, DEFAULT_MINUTE)
        set(value) = prefs.edit().putInt(KEY_MINUTE, value).apply()

    fun formatted(): String = String.format("%02d:%02d", hour, minute)

    fun set(hour: Int, minute: Int) {
        prefs.edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply()
    }

    companion object {
        private const val KEY_HOUR = "reminder_hour"
        private const val KEY_MINUTE = "reminder_minute"
        private const val DEFAULT_HOUR = 20
        private const val DEFAULT_MINUTE = 0
    }
}
