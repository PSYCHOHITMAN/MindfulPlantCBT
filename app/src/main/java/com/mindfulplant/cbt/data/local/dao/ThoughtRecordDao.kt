package com.mindfulplant.cbt.data.local.dao

import androidx.room.*
import com.mindfulplant.cbt.data.local.entity.ThoughtRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThoughtRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ThoughtRecordEntity)

    /** Used when downloading from the server: never overwrite a record already on the phone. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfAbsent(records: List<ThoughtRecordEntity>)

    /** All records for a user, most recent first - powers Mood History. */
    @Query("SELECT * FROM thought_records WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeRecordsForUser(userId: String): Flow<List<ThoughtRecordEntity>>

    /** One-shot fetch (not reactive) - used by the Insights trend calculation. */
    @Query("SELECT * FROM thought_records WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getRecordsForUser(userId: String): List<ThoughtRecordEntity>

    @Query("SELECT * FROM thought_records WHERE syncStatus = 'pending' AND userId = :userId")
    suspend fun getPendingRecords(userId: String): List<ThoughtRecordEntity>

    @Query("UPDATE thought_records SET syncStatus = :status WHERE recordId = :recordId")
    suspend fun updateSyncStatus(recordId: String, status: String)
}
