package com.pfa.interview.data.model

data class HistoryDetails(
    val session: InterviewSession,
    val questions: List<Question>,
    val answers: List<Answer>,
    val feedbacks: List<Feedback>
)
