package com.mindfulplant.cbt.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mindfulplant.cbt.util.ReminderScheduler

/** Fires at the user's reminder time, shows the notification, then schedules tomorrow's. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        NotificationHelper.showReminder(context)
        ReminderScheduler.schedule(context)
    }
}
