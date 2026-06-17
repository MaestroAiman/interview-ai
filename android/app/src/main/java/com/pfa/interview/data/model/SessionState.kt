package com.pfa.interview.data.model

data class SessionState(
    val session: InterviewSession,
    val questions: List<Question>,
    val answers: List<Answer>,
    val feedbacks: List<Feedback>,
    val totalQuestions: Int,
    val answeredCount: Int
) {
    fun isCompleted() = session.status == "COMPLETED"

    fun hasFeedbackFor(questionId: String?) =
        questionId != null && feedbacks.any { it.questionId == questionId }

    fun feedbackFor(questionId: String?) =
        feedbacks.firstOrNull { it.questionId == questionId }

    fun toSummary() = SessionSummary(
        overallScore = session.overallScore,
        globalAssessment = session.globalAssessment ?: "",
        topStrengths = session.topStrengths,
        priorityImprovements = session.priorityImprovements,
        recommendedResources = session.recommendedResources,
        readinessLevel = session.readinessLevel ?: "almost_ready"
    )
}
