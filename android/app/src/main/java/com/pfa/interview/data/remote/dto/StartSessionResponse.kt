package com.pfa.interview.data.remote.dto

import com.pfa.interview.data.model.Question

data class StartSessionResponse(
    val sessionId: String,
    val firstQuestion: Question
)
