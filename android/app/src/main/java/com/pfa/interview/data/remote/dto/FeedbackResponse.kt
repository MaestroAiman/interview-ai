package com.pfa.interview.data.remote.dto

data class FeedbackResponse(
    val relevanceScore: Float,
    val clarityScore: Float,
    val sentimentScore: Float,
    val comments: String,
    val improvedAnswer: String = ""
)
