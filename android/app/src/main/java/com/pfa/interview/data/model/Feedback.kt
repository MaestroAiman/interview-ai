package com.pfa.interview.data.model

data class Feedback(
    val id: String = "",
    val questionId: String = "",
    val relevanceScore: Float = 0f,
    val clarityScore: Float = 0f,
    val sentimentScore: Float = 0f,
    val comments: String = "",
    val improvedAnswer: String = "",
    val depthScore: Float = 0f,
    val vocabularyScore: Float = 0f,
    val examplesScore: Float = 0f,
    val globalScore: Float = 0f,
    val levelAssessment: String? = null,
    val keyStrengths: List<String> = emptyList(),
    val criticalGaps: List<String> = emptyList(),
    val positivePoints: String? = null,
    val improvementPoints: String? = null,
    val concreteAdvice: String? = null,
    val exampleAnswer: String? = null,
    val nextDifficulty: String? = null
)
