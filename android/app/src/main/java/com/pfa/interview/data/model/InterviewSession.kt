package com.pfa.interview.data.model

data class InterviewSession(
    val id: String = "",
    val userId: String = "",
    val type: String = "",
    val position: String = "",
    val difficulty: String = "",
    val status: String = "",
    val overallScore: Float = 0f,
    val questionCount: Int = 0,
    val startedAt: String = "",
    val endedAt: String? = null,
    val globalAssessment: String? = null,
    val topStrengths: List<String> = emptyList(),
    val priorityImprovements: List<String> = emptyList(),
    val recommendedResources: List<String> = emptyList(),
    val readinessLevel: String? = null
)
