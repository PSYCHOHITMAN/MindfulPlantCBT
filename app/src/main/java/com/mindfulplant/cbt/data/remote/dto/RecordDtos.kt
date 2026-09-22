package com.mindfulplant.cbt.data.remote.dto

// Request/response shape for the /records endpoint (save + get).

data class ThoughtRecordDto(
    val recordId: String,
    val userId: String,
    val situation: String,
    val automaticThought: String,
    val distortionType: String,
    val balancedReframe: String,
    val moodBefore: Int,
    val moodAfter: Int,
    val createdAt: Long
)

data class SaveRecordResponse(
    val recordId: String,
    val success: Boolean
)
