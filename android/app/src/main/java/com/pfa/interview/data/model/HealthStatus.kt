package com.pfa.interview.data.model

data class HealthStatus(
    val provider: String = "",
    val status: String,
    val message: String,
    val latencyMs: Long,
    val model: String,
    val url: String? = null
)
