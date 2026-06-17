package com.pfa.interview.data.remote.dto

data class AdminUserCreateRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String,
    val targetPosition: String?
)
