package com.mindfulplant.cbt.data.local.dao

import androidx.room.*
import com.mindfulplant.cbt.data.local.entity.MoodEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodEntryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MoodEntryEntity)

    @Query("SELECT * FROM mood_entries WHERE userId = :userId ORDER BY loggedAt DESC")
    fun observeEntriesForUser(userId: String): Flow<List<MoodEntryEntity>>

    @Query("SELECT * FROM mood_entries WHERE syncStatus = 'pending' AND userId = :userId")
    suspend fun getPendingEntries(userId: String): List<MoodEntryEntity>

    @Query("UPDATE mood_entries SET syncStatus = :status WHERE moodId = :moodId")
    suspend fun updateSyncStatus(moodId: String, status: String)
}
