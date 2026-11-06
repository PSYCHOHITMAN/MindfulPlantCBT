package com.mindfulplant.cbt.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Queues a background sync that WorkManager runs as soon as the phone has a
 * connection. If the phone is offline, the job simply waits and starts the
 * moment the network comes back, even if the app was closed in the meantime.
 *
 * It is safe to call this often: syncing is idempotent (the API upserts by
 * recordId), and [ExistingWorkPolicy.APPEND_OR_REPLACE] queues a new run
 * behind one that is already in progress instead of running both at once.
 */
object SyncScheduler {

    private const val UNIQUE_NAME = "record_sync"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            // If the server is unreachable (e.g. still waking up), try again with growing gaps.
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}
