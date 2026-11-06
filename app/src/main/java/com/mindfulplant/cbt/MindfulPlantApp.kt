package com.mindfulplant.cbt

import android.app.Application
import com.mindfulplant.cbt.data.local.AppDatabase
import com.mindfulplant.cbt.notifications.NotificationHelper
import com.mindfulplant.cbt.util.ReminderScheduler

/**
 * Application class. Holds a single, app-wide instance of the Room
 * database so every screen shares the same offline-first data source.
 */
class MindfulPlantApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        // Firebase is initialised automatically via the google-services
        // plugin, as long as google-services.json is present in app/.

        NotificationHelper.createChannel(this)
        // Keep the daily reminder scheduled for the saved time.
        ReminderScheduler.schedule(this)
    }
}
