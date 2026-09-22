package com.mindfulplant.cbt.data.local.dao

import androidx.room.*
import com.mindfulplant.cbt.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUser(userId: String): UserEntity?

    @Query("UPDATE users SET preferredLanguage = :language WHERE userId = :userId")
    suspend fun updateLanguage(userId: String, language: String)

    @Query("UPDATE users SET notificationsEnabled = :enabled WHERE userId = :userId")
    suspend fun updateNotificationsEnabled(userId: String, enabled: Boolean)
}
