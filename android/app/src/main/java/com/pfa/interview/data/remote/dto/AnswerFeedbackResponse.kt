package com.pfa.interview.data.remote.dto

import com.pfa.interview.data.model.Feedback
import com.pfa.interview.data.model.Question
import com.pfa.interview.data.model.SessionSummary

data class AnswerFeedbackResponse(
    val feedback: Feedback,
    val nextQuestion: Question?,
    val sessionSummary: SessionSummary?
)
