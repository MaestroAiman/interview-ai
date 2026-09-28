package com.pfa.interview.data.remote.dto

data class SubmitAnswerRequest(
    val sessionId: String,
    val questionId: String,
    val answerText: String
)
