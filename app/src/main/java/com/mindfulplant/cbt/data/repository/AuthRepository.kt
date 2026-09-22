package com.mindfulplant.cbt.data.repository

import com.mindfulplant.cbt.data.local.dao.UserDao
import com.mindfulplant.cbt.data.local.entity.UserEntity
import com.mindfulplant.cbt.data.remote.ApiService
import com.mindfulplant.cbt.data.remote.dto.LoginRequest
import com.mindfulplant.cbt.data.remote.dto.RegisterRequest
import com.mindfulplant.cbt.util.SessionManager
import org.json.JSONObject
import retrofit2.Response

/** Simple result wrapper so the UI can show a clear success/error message. */
sealed class AuthResult {
    data class Success(val userId: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Handles register/login against the REST API, then caches the resulting
 * user profile locally and stores the session (userId + token).
 *
 * IMPORTANT: the password is sent as plain text here, over HTTPS. That is
 * intentional and correct - HTTPS/TLS is what encrypts it in transit, and
 * the API hashes it with bcrypt before ever storing it. Hashing on the
 * client first would NOT work: bcrypt generates a new random salt every
 * time it runs, so hashing the same password twice produces two different
 * strings, and login would never be able to match what was stored at
 * registration. Encryption-at-rest belongs on the server, not the client.
 *
 * SSO (Google Sign-In) is handled separately via Firebase Authentication
 * directly in the UI layer (see ui/login/LoginFragment) - Firebase manages
 * that flow itself, so it doesn't need to go through this repository.
 */
class AuthRepository(
    private val apiService: ApiService,
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {

    suspend fun register(fullName: String, email: String, password: String): AuthResult {
        return try {
            val response = apiService.register(RegisterRequest(fullName, email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                userDao.upsert(
                    UserEntity(
                        userId = body.userId,
                        fullName = body.fullName,
                        email = body.email,
                        ssoProvider = null
                    )
                )
                sessionManager.userId = body.userId
                sessionManager.authToken = body.token
                AuthResult.Success(body.userId)
            } else {
                AuthResult.Error(
                    responseMessage(response)
                        ?: "Registration failed (${response.code()}). Please try again."
                )
            }
        } catch (e: Exception) {
            AuthResult.Error("Couldn't reach the server. Check your connection.")
        }
    }

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                userDao.upsert(
                    UserEntity(
                        userId = body.userId,
                        fullName = body.fullName,
                        email = body.email,
                        ssoProvider = null
                    )
                )
                sessionManager.userId = body.userId
                sessionManager.authToken = body.token
                AuthResult.Success(body.userId)
            } else {
                AuthResult.Error(responseMessage(response) ?: "Incorrect email or password.")
            }
        } catch (e: Exception) {
            AuthResult.Error("Couldn't reach the server. Check your connection.")
        }
    }

    private fun responseMessage(response: Response<*>): String? {
        return runCatching {
            response.errorBody()?.string()
                ?.let { JSONObject(it).optString("message").takeIf(String::isNotBlank) }
        }.getOrNull()
    }

    fun logout() = sessionManager.clear()
}
