package com.pfa.interview.data.remote.dto

data class AdminUserUpdateRequest(
    val name: String,
    val email: String,
    val targetPosition: String?,
    val role: String
)
