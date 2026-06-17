package com.pfa.interview.data.remote.dto

data class UserAdminDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val targetPosition: String?,
    val createdAt: String?
)
