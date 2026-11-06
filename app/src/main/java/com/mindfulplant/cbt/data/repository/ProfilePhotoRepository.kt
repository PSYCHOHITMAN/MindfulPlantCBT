package com.mindfulplant.cbt.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.mindfulplant.cbt.data.remote.ApiService
import com.mindfulplant.cbt.data.remote.RetrofitClient
import com.mindfulplant.cbt.util.ImageProcessor
import com.mindfulplant.cbt.util.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * Profile photo = blob storage. The image is stored as binary in MongoDB via
 * the API (PUT / GET / DELETE /profile/photo) and cached on the phone so it
 * shows instantly and works offline.
 *
 * Offline-first, like thought records: a new photo is saved on the phone first
 * and flagged "pending upload". [sync] sends it when the connection is back.
 * On a fresh install, or on a second device, [sync] downloads the stored photo.
 */
class ProfilePhotoRepository(
    context: Context,
    private val apiService: ApiService = RetrofitClient.apiService
) {
    sealed class SetResult {
        object Uploaded : SetResult()
        object SavedOffline : SetResult()
        object Invalid : SetResult()
    }

    private val appContext = context.applicationContext
    private val sessionManager = SessionManager(appContext)
    private val prefs = appContext.getSharedPreferences("profile_photo", Context.MODE_PRIVATE)

    private val userId: String? get() = sessionManager.userId
    private fun file(): File? = userId?.let { File(appContext.filesDir, "profile_$it.jpg") }
    private fun pendingUploadKey() = "pending_upload_$userId"
    private fun pendingDeleteKey() = "pending_delete_$userId"
    private fun etagKey() = "etag_$userId"
    private fun bearer(): String? = sessionManager.authToken?.let { "Bearer $it" }

    /** The cached photo, or null if the user hasn't set one. Small (512 px), so cheap to decode. */
    fun loadLocal(): Bitmap? =
        file()?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }

    /** Resize + save on the phone, then try to upload. */
    suspend fun setPhoto(uri: Uri): SetResult = withContext(Dispatchers.IO) {
        val bytes = ImageProcessor.toAvatarJpeg(appContext, uri) ?: return@withContext SetResult.Invalid
        val target = file() ?: return@withContext SetResult.Invalid
        target.writeBytes(bytes)
        // Flag first: if the upload is interrupted, the next sync() finishes the job.
        prefs.edit()
            .putBoolean(pendingUploadKey(), true)
            .putBoolean(pendingDeleteKey(), false)
            .remove(etagKey())
            .apply()
        if (uploadPending()) SetResult.Uploaded else SetResult.SavedOffline
    }

    /** Remove the photo from the phone and (when online) from the server. */
    suspend fun remove() = withContext(Dispatchers.IO) {
        file()?.delete()
        prefs.edit()
            .putBoolean(pendingUploadKey(), false)
            .putBoolean(pendingDeleteKey(), true)
            .remove(etagKey())
            .apply()
        deleteRemote()
    }

    /**
     * Finishes any pending upload/delete; otherwise pulls the server copy.
     * Returns true when a different photo was just saved on the phone, so the
     * caller knows to redraw the avatar.
     */
    suspend fun sync(): Boolean = withContext(Dispatchers.IO) {
        val target = file() ?: return@withContext false
        val auth = bearer() ?: return@withContext false

        if (prefs.getBoolean(pendingDeleteKey(), false)) {
            deleteRemote()
            return@withContext false
        }
        if (prefs.getBoolean(pendingUploadKey(), false)) {
            uploadPending()
            return@withContext false
        }

        try {
            // Only send the ETag if we still have the file it belongs to.
            val etag = if (target.exists()) prefs.getString(etagKey(), null) else null
            val response = apiService.getProfilePhoto(auth, etag)
            if (response.isSuccessful) {
                val bytes = response.body()?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    target.writeBytes(bytes)
                    prefs.edit().putString(etagKey(), response.headers()["ETag"]).apply()
                    return@withContext true
                }
            }
            false // 304 (unchanged), 404 (no photo yet) or an error: nothing to redraw
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    /** Call on logout so the next person to sign in on this phone doesn't see this photo. */
    fun clearLocal() {
        file()?.delete()
        prefs.edit()
            .remove(pendingUploadKey())
            .remove(pendingDeleteKey())
            .remove(etagKey())
            .apply()
    }

    private suspend fun uploadPending(): Boolean {
        val source = file()?.takeIf { it.exists() } ?: return true
        val auth = bearer() ?: return false
        return try {
            val body = source.readBytes().toRequestBody("image/jpeg".toMediaType())
            val response = apiService.uploadProfilePhoto(auth, body)
            if (response.isSuccessful) {
                prefs.edit().putBoolean(pendingUploadKey(), false).apply()
                true
            } else false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun deleteRemote(): Boolean {
        val auth = bearer() ?: return false
        return try {
            val response = apiService.deleteProfilePhoto(auth)
            if (response.isSuccessful || response.code() == 404) {
                prefs.edit().putBoolean(pendingDeleteKey(), false).apply()
                true
            } else false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }
}
