package com.mindfulplant.cbt.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Small wrapper around SharedPreferences for the logged-in user's ID and
 * auth token. For a production app this would use EncryptedSharedPreferences
 * (androidx.security:security-crypto) - noted here as a follow-up rather
 * than required for the prototype.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mindful_plant_session", Context.MODE_PRIVATE)

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var authToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    val isLoggedIn: Boolean
        get() = userId != null

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_TOKEN = "auth_token"
    }
}
