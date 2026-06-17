package com.pfa.interview.data.remote.dto

import com.pfa.interview.data.model.Feedback
import com.pfa.interview.data.model.Question

data class AnswerResponse(
    val feedback: Feedback,
    val nextQuestion: Question?
)
