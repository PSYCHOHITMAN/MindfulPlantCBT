package com.mindfulplant.cbt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A lightweight, standalone mood check-in (separate from a full thought
 * record), e.g. for a quick daily "how am I doing" tap.
 */
@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
    @PrimaryKey val moodId: String,
    val userId: String,
    val moodScore: Int, // 1-5
    val note: String? = null,
    val loggedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending"
)
