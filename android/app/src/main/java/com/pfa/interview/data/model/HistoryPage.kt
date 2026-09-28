package com.pfa.interview.data.model

data class HistoryPage(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val sessions: List<InterviewSession>
)
