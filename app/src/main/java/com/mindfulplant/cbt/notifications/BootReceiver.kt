package com.mindfulplant.cbt.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mindfulplant.cbt.util.ReminderScheduler

/**
 * Alarms are cleared when the phone restarts or the app is updated, and the
 * exact-alarm permission can be toggled by the user. Re-create the daily
 * reminder in each of those cases.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ReminderScheduler.schedule(context)
    }
}
