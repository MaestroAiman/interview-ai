package com.pfa.interview.data.model

data class SessionSummary(
    val overallScore: Float = 0f,
    val globalAssessment: String = "",
    val topStrengths: List<String> = emptyList(),
    val priorityImprovements: List<String> = emptyList(),
    val recommendedResources: List<String> = emptyList(),
    val readinessLevel: String = "almost_ready"
)
