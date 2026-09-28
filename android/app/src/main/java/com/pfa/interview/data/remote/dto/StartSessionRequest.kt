package com.pfa.interview.data.remote.dto

data class StartSessionRequest(
    val type: String,
    val position: String,
    val difficulty: String,
    val questionCount: Int
)
