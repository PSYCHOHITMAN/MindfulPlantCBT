package com.mindfulplant.cbt.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mindfulplant.cbt.data.local.dao.MoodEntryDao
import com.mindfulplant.cbt.data.local.dao.ThoughtRecordDao
import com.mindfulplant.cbt.data.local.dao.UserDao
import com.mindfulplant.cbt.data.local.entity.MoodEntryEntity
import com.mindfulplant.cbt.data.local.entity.ThoughtRecordEntity
import com.mindfulplant.cbt.data.local.entity.UserEntity

/**
 * The single Room database for the app. This is what makes the app
 * "offline-first": every screen reads from and writes to this database
 * directly, and syncing to the API is a separate, explicit step
 * (see RecordRepository.syncPendingRecords).
 */
@Database(
    entities = [UserEntity::class, ThoughtRecordEntity::class, MoodEntryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun thoughtRecordDao(): ThoughtRecordDao
    abstract fun moodEntryDao(): MoodEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mindful_plant_cbt.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
