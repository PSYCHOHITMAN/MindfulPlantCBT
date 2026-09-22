package com.mindfulplant.cbt.data.remote.dto

// Requests/responses for the two auth endpoints (Planning and Design,
// Section 10: /register and /login). Kept intentionally minimal.

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String // sent over HTTPS; the API hashes this with bcrypt before storing it
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val userId: String,
    val fullName: String,
    val email: String,
    val token: String // JWT, stored securely and sent as a Bearer token
)
