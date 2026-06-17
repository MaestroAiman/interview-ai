package com.pfa.interview.data.remote.dto

data class AdminDashboardDto(
    val totalUsers: Int,
    val completedCount: Long,
    val inProgressCount: Long,
    val platformAvgScore: Float,
    val sessions: List<SessionRowDto>
) {
    data class SessionRowDto(
        val id: String,
        val userId: String,
        val position: String?,
        val type: String?,
        val difficulty: String?,
        val overallScore: Float,
        val status: String?,
        val startedAt: String?
    )
}
