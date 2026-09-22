package com.mindfulplant.cbt.data.remote

import com.mindfulplant.cbt.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * The app's REST API contract - kept to exactly three routes, as scoped in
 * the Planning and Design document (Section 10, API Design): /register,
 * /login, and /records. Everything else (offline sync included) is built
 * on top of these same endpoints rather than adding new ones.
 */
interface ApiService {

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("records")
    suspend fun saveRecord(
        @Header("Authorization") bearerToken: String,
        @Body record: ThoughtRecordDto
    ): Response<SaveRecordResponse>

    @GET("records")
    suspend fun getRecords(
        @Header("Authorization") bearerToken: String
    ): Response<List<ThoughtRecordDto>>
}
