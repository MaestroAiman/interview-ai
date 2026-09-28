package com.pfa.interview.data.remote.dto

data class LoginResponse(
    val token: String,
    val userId: String,
    val name: String,
    val role: String = "USER"
)
