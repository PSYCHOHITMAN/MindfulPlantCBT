package com.mindfulplant.cbt.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mindfulplant.cbt.notifications.ReminderReceiver
import java.util.Calendar

/**
 * Schedules the daily check-in reminder with AlarmManager.
 *
 * WorkManager only guarantees "no earlier than", so on a sleeping phone the
 * reminder could arrive minutes late. An exact, Doze-friendly alarm fires at
 * the chosen time. When it fires, [ReminderReceiver] shows the notification
 * and calls [schedule] again for the next day. [com.mindfulplant.cbt.notifications.BootReceiver]
 * re-creates the alarm after a reboot or app update.
 */
object ReminderScheduler {

    private const val REQUEST_CODE = 2001

    /** (Re)schedules the reminder for the saved time. Safe to call repeatedly. */
    fun schedule(context: Context) {
        val appContext = context.applicationContext
        val prefs = ReminderPrefs(appContext)
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        val triggerAt = System.currentTimeMillis() + millisUntilNext(prefs.hour, prefs.minute)
        val pendingIntent = pendingIntent(appContext)

        if (canScheduleExact(appContext)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            // Exact alarms not allowed by the user: still fires while idle, but
            // Android may delay it slightly.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        appContext.getSystemService(AlarmManager::class.java).cancel(pendingIntent(appContext))
    }

    /** On Android 12+ the user can switch "Alarms & reminders" off for the app (default off on 14+). */
    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    internal fun millisUntilNext(hour: Int, minute: Int, now: Calendar = Calendar.getInstance()): Long {
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
