package com.pfa.interview.data.model

data class Question(
    val id: String = "",
    val sessionId: String = "",
    val content: String = "",
    val order: Int = 0,
    val type: String = "",
    val category: String? = null,
    val aiDifficulty: String? = null,
    val estimatedDurationSeconds: Int = 120,
    val expectedKeywords: List<String> = emptyList()
)
