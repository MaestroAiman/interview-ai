package com.pfa.interview.data.local

import com.pfa.interview.data.model.Question
import com.pfa.interview.data.model.SessionSummary
import javax.inject.Inject
import javax.inject.Singleton

data class PendingSession(
    val sessionId: String,
    val firstQuestion: Question,
    val totalQuestions: Int,
    val sessionSummary: SessionSummary? = null
)

@Singleton
class CurrentSessionHolder @Inject constructor() {
    private var pendingSession: PendingSession? = null
    private var lastSummary: SessionSummary? = null

    fun set(session: PendingSession) {
        pendingSession = session
    }

    fun consume(): PendingSession? {
        val s = pendingSession
        pendingSession = null
        return s
    }

    fun setSummary(s: SessionSummary) {
        lastSummary = s
    }

    fun consumeSummary(): SessionSummary? {
        val s = lastSummary
        lastSummary = null
        return s
    }
}
