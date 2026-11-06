package com.mindfulplant.cbt.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mindfulplant.cbt.MindfulPlantApp
import com.mindfulplant.cbt.data.remote.RetrofitClient
import com.mindfulplant.cbt.data.repository.ProfilePhotoRepository
import com.mindfulplant.cbt.data.repository.RecordRepository
import com.mindfulplant.cbt.data.repository.SyncResult
import com.mindfulplant.cbt.util.SessionManager
import kotlinx.coroutines.CancellationException

/**
 * The automatic sync, run by WorkManager once a connection is available:
 *  1. push every record saved while offline ("pending") to the API,
 *  2. pull any records the phone doesn't have yet (new install / second device),
 *  3. finish any pending profile photo upload or download.
 *
 * If the server can't be reached the work is retried a few times with a
 * growing delay; the next reconnect or app start schedules a fresh attempt.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sessionManager = SessionManager(applicationContext)
        val userId = sessionManager.userId ?: return Result.success() // logged out: nothing to sync

        val app = applicationContext as MindfulPlantApp
        val repository = RecordRepository(
            apiService = RetrofitClient.apiService,
            recordDao = app.database.thoughtRecordDao(),
            sessionManager = sessionManager
        )

        val push = repository.syncPendingRecords(userId)
        val pushFailed = push is SyncResult.Error ||
            (push is SyncResult.Success && push.failedCount > 0)
        val pulled = repository.pullRecords(userId)

        try {
            ProfilePhotoRepository(applicationContext).sync()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The photo is a nicety; it retries on the next sync.
        }

        return if ((pushFailed || !pulled) && runAttemptCount < MAX_ATTEMPTS) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}
