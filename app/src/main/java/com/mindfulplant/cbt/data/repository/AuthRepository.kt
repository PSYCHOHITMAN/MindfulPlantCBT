package com.mindfulplant.cbt.data.repository

import com.mindfulplant.cbt.R
import com.mindfulplant.cbt.data.local.dao.UserDao
import com.mindfulplant.cbt.data.local.entity.UserEntity
import com.mindfulplant.cbt.data.remote.ApiService
import com.mindfulplant.cbt.data.remote.dto.LoginRequest
import com.mindfulplant.cbt.data.remote.dto.RegisterRequest
import com.mindfulplant.cbt.data.remote.dto.SsoRequest
import com.mindfulplant.cbt.util.SessionManager
import com.mindfulplant.cbt.util.UiText
import com.mindfulplant.cbt.util.uiText
import org.json.JSONObject
import retrofit2.Response

/** Simple result wrapper so the UI can show a clear success/error message. */
sealed class AuthResult {
    data class Success(val userId: String) : AuthResult()
    data class Error(val message: UiText) : AuthResult()
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
                        ?: uiText(R.string.err_register_failed, response.code())
                )
            }
        } catch (e: Exception) {
            AuthResult.Error(uiText(R.string.err_no_connection))
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
                AuthResult.Error(responseMessage(response) ?: uiText(R.string.err_login_incorrect))
            }
        } catch (e: Exception) {
            AuthResult.Error(uiText(R.string.err_no_connection))
        }
    }

    /** Exchanges a Google ID token for our own API session (JWT). */
    suspend fun ssoLogin(idToken: String): AuthResult {
        return try {
            val response = apiService.sso(SsoRequest(idToken))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                userDao.upsert(
                    UserEntity(
                        userId = body.userId,
                        fullName = body.fullName,
                        email = body.email,
                        ssoProvider = "google"
                    )
                )
                sessionManager.userId = body.userId
                sessionManager.authToken = body.token
                AuthResult.Success(body.userId)
            } else {
                AuthResult.Error(
                    responseMessage(response)
                        ?: uiText(R.string.err_google_failed_code, response.code())
                )
            }
        } catch (e: Exception) {
            AuthResult.Error(uiText(R.string.err_no_connection))
        }
    }

    private fun responseMessage(response: Response<*>): UiText? {
        return runCatching {
            response.errorBody()?.string()
                ?.let { JSONObject(it).optString("message").takeIf(String::isNotBlank) }
        }.getOrNull()?.let { UiText.Raw(it) }
    }

    fun logout() = sessionManager.clear()
}
