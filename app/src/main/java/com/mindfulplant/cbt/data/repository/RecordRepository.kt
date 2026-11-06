package com.mindfulplant.cbt.data.repository

import com.mindfulplant.cbt.R
import com.mindfulplant.cbt.util.UiText
import com.mindfulplant.cbt.util.uiText
import kotlinx.coroutines.CancellationException
import com.mindfulplant.cbt.data.local.dao.ThoughtRecordDao
import com.mindfulplant.cbt.data.local.entity.ThoughtRecordEntity
import com.mindfulplant.cbt.data.remote.ApiService
import com.mindfulplant.cbt.data.remote.dto.ThoughtRecordDto
import com.mindfulplant.cbt.util.SessionManager
import kotlinx.coroutines.flow.Flow
import java.util.UUID

sealed class SyncResult {
    data class Success(val syncedCount: Int, val failedCount: Int = 0) : SyncResult()
    data class Error(val message: UiText) : SyncResult()
}

/**
 * Everything to do with reading/writing thought records.
 *
 * Offline-first sync (Planning and Design, Section 8.1): records are ALWAYS
 * saved to Room first (so the app works fully offline) and marked "pending".
 * syncPendingRecords() pushes them to the API. That runs automatically through
 * SyncWorker as soon as the phone is back online, or when the user taps
 * "Sync now". pullRecords() downloads records the phone doesn't have yet, so a
 * reinstall or a second device shows the user's history. There is no
 * conflict-merging logic, by design: records are never edited after saving,
 * and the API upserts by recordId, so repeating a sync is always safe.
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
            ?: return SyncResult.Error(uiText(R.string.err_not_logged_in))

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

    /**
     * Downloads the user's records from the API and adds the ones this phone
     * doesn't have. Records already on the phone (including pending ones) are
     * left untouched. Returns false if the server couldn't be reached.
     */
    suspend fun pullRecords(userId: String): Boolean {
        val token = sessionManager.authToken ?: return false
        return try {
            val response = apiService.getRecords("Bearer $token")
            if (!response.isSuccessful) return false
            val missing = response.body().orEmpty().map { it.toEntity(userId) }
            recordDao.insertAllIfAbsent(missing)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    private fun ThoughtRecordDto.toEntity(userId: String) = ThoughtRecordEntity(
        recordId = recordId,
        userId = userId,
        situation = situation,
        automaticThought = automaticThought,
        distortionType = distortionType,
        balancedReframe = balancedReframe,
        moodBefore = moodBefore,
        moodAfter = moodAfter,
        createdAt = createdAt,
        syncStatus = "synced"
    )

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
