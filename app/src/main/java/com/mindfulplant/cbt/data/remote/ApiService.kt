package com.mindfulplant.cbt.data.remote

import com.mindfulplant.cbt.data.remote.dto.*
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * The app's REST API contract. Part 1 scoped three routes (/register, /login,
 * /records); Part 3 adds /sso (Google sign-in) and /profile/photo (blob
 * storage for the profile photo). Offline sync is built on top of /records.
 */
interface ApiService {

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("sso")
    suspend fun sso(@Body request: SsoRequest): Response<AuthResponse>

    @POST("records")
    suspend fun saveRecord(
        @Header("Authorization") bearerToken: String,
        @Body record: ThoughtRecordDto
    ): Response<SaveRecordResponse>

    @GET("records")
    suspend fun getRecords(
        @Header("Authorization") bearerToken: String
    ): Response<List<ThoughtRecordDto>>

    // --- Profile photo (blob storage). The body is the raw JPEG bytes. ---

    @PUT("profile/photo")
    suspend fun uploadProfilePhoto(
        @Header("Authorization") bearerToken: String,
        @Body image: RequestBody
    ): Response<Unit>

    // Pass the ETag from the last download; the server answers 304 if the photo is unchanged.
    @GET("profile/photo")
    suspend fun getProfilePhoto(
        @Header("Authorization") bearerToken: String,
        @Header("If-None-Match") etag: String?
    ): Response<ResponseBody>

    @DELETE("profile/photo")
    suspend fun deleteProfilePhoto(
        @Header("Authorization") bearerToken: String
    ): Response<Unit>
}
