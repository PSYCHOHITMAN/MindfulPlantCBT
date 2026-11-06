package com.mindfulplant.cbt

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import com.mindfulplant.cbt.data.local.AppDatabase
import com.mindfulplant.cbt.notifications.NotificationHelper
import com.mindfulplant.cbt.sync.SyncScheduler
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

        watchConnectivity()
    }

    /**
     * Automatic sync: every time the phone gets a connection (including when
     * the app starts online), queue a background sync. SyncWorker does nothing
     * if nobody is logged in, and the queued job is WorkManager's, so it still
     * runs if the connection returns while the app is closed.
     */
    private fun watchConnectivity() {
        try {
            val connectivity = getSystemService(ConnectivityManager::class.java)
            connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    SyncScheduler.enqueue(this@MindfulPlantApp)
                }
            })
        } catch (e: Exception) {
            Log.w("MindfulPlantApp", "Could not watch connectivity", e)
        }
    }
}
