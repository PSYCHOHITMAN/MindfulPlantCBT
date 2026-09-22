package com.mindfulplant.cbt.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache of the logged-in user's profile.
 * Mirrors the "User" entity from the Planning and Design data model
 * (Section 12 of the PoE document).
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val fullName: String,
    val email: String,
    // Only ever populated for SSO users (e.g. "google"); null for
    // email/password accounts. The password itself is never stored on
    // the device at all - only the API stores a bcrypt hash, server-side.
    val ssoProvider: String?,
    val preferredLanguage: String = "en", // "en" | "zu" | "st"
    val notificationsEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
