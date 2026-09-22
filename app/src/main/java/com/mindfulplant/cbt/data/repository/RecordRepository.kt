package com.mindfulplant.cbt.data.repository

import com.mindfulplant.cbt.data.local.dao.ThoughtRecordDao
import com.mindfulplant.cbt.data.local.entity.ThoughtRecordEntity
import com.mindfulplant.cbt.data.remote.ApiService
import com.mindfulplant.cbt.data.remote.dto.ThoughtRecordDto
import com.mindfulplant.cbt.util.SessionManager
import kotlinx.coroutines.flow.Flow
import java.util.UUID

sealed class SyncResult {
    data class Success(val syncedCount: Int, val failedCount: Int = 0) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

/**
 * Everything to do with reading/writing thought records.
 *
 * This implements the simplified, one-way "Sync now" approach from the
 * Planning and Design document (Section 8.1): records are ALWAYS saved to
 * Room first (so the app works fully offline), marked "pending", and only
 * pushed to the API when syncPendingRecords() is explicitly called - either
 * by the user tapping "Sync now", or automatically when the app detects it
 * has a connection. There is no conflict-merging logic, by design: the
 * project assumes a single device per user, so a straightforward
 * last-write-wins is enough.
 */
class RecordRepository(
    private val apiService: ApiService,
    private val recordDao: ThoughtRecordDao,
    private val sessionManager: SessionManager
) {

    fun observeRecords(userId: String): Flow<List<ThoughtRecordEntity>> =
        recordDao.observeRecordsForUser(userId)

    /** Always writes locally first - this is what makes the app offline-first. */
    suspend fun saveRecordLocally(
        userId: String,
        situation: String,
        automaticThought: String,
        distortionType: String,
        balancedReframe: String,
        moodBefore: Int,
        moodAfter: Int
    ): ThoughtRecordEntity {
        val record = ThoughtRecordEntity(
            recordId = UUID.randomUUID().toString(),
            userId = userId,
            situation = situation,
            automaticThought = automaticThought,
            distortionType = distortionType,
            balancedReframe = balancedReframe,
            moodBefore = moodBefore,
            moodAfter = moodAfter,
            syncStatus = "pending"
        )
        recordDao.insert(record)
        return record
    }

    /** Pushes every "pending" record for this user to the API. */
    suspend fun syncPendingRecords(userId: String): SyncResult {
        val token = sessionManager.authToken
            ?: return SyncResult.Error("Not logged in.")

        val pending = recordDao.getPendingRecords(userId)
        if (pending.isEmpty()) return SyncResult.Success(0)

        var syncedCount = 0
        var failedCount = 0
        for (record in pending) {
            try {
                val response = apiService.saveRecord(
                    bearerToken = "Bearer $token",
                    record = record.toDto()
                )
                if (response.isSuccessful) {
                    recordDao.updateSyncStatus(record.recordId, "synced")
                    syncedCount++
                } else {
                    // Keep it pending so a temporary API outage or expired
                    // deployment can be retried from the History screen.
                    recordDao.updateSyncStatus(record.recordId, "pending")
                    failedCount++
                }
            } catch (e: Exception) {
                // No connection, or the API is unreachable - leave it as
                // "pending" so the next "Sync now" tap picks it up again.
                recordDao.updateSyncStatus(record.recordId, "pending")
                failedCount++
            }
        }
        return SyncResult.Success(syncedCount, failedCount)
    }

    private fun ThoughtRecordEntity.toDto() = ThoughtRecordDto(
        recordId = recordId,
        userId = userId,
        situation = situation,
        automaticThought = automaticThought,
        distortionType = distortionType,
        balancedReframe = balancedReframe,
        moodBefore = moodBefore,
        moodAfter = moodAfter,
        createdAt = createdAt
    )
}
