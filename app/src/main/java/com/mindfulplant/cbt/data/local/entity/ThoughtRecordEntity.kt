package com.mindfulplant.cbt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single CBT thought record. This is the app's core data type.
 *
 * syncStatus tracks the simplified, one-way offline sync approach from the
 * Planning and Design document: entries are always written here first
 * ("pending"), then pushed to the API when the user taps "Sync now"
 * (becoming "synced", or "failed" if the push didn't succeed).
 */
@Entity(tableName = "thought_records")
data class ThoughtRecordEntity(
    @PrimaryKey val recordId: String,
    val userId: String,
    val situation: String,
    val automaticThought: String,
    val distortionType: String,
    val balancedReframe: String,
    val moodBefore: Int, // 1-5
    val moodAfter: Int,  // 1-5
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending" // "pending" | "synced" | "failed"
)
