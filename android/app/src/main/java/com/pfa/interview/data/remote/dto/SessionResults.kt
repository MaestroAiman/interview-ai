package com.pfa.interview.data.remote.dto

data class QuestionReview(
    val question: String,
    val answer: String,
    val feedback: String,
    val score: Float
)

data class SessionResults(
    val sessionId: String,
    val type: String,
    val position: String,
    val difficulty: String,
    val overallScore: Float,
    val relevanceAvg: Float,
    val clarityAvg: Float,
    val sentimentAvg: Float,
    val questionReviews: List<QuestionReview>
)
