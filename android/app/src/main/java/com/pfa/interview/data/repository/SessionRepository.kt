package com.pfa.interview.data.repository

import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.model.SessionState
import com.pfa.interview.data.model.SessionSummary
import com.pfa.interview.data.remote.dto.AnswerFeedbackResponse
import com.pfa.interview.data.remote.dto.AnswerResponse
import com.pfa.interview.data.remote.dto.SessionResults
import com.pfa.interview.data.remote.dto.StartSessionResponse

interface SessionRepository {
    suspend fun startSession(
        type: String,
        position: String,
        difficulty: String,
        questionCount: Int
    ): Resource<StartSessionResponse>

    suspend fun submitAnswer(
        sessionId: String,
        questionId: String,
        answerText: String
    ): Resource<AnswerResponse>

    suspend fun getResults(sessionId: String): Resource<SessionResults>

    suspend fun startAdaptiveSession(
        type: String,
        position: String,
        difficulty: String,
        questionCount: Int
    ): Resource<StartSessionResponse>

    suspend fun submitAdaptiveAnswer(
        sessionId: String,
        questionId: String,
        answerText: String
    ): Resource<AnswerFeedbackResponse>

    suspend fun getSessionState(sessionId: String): Resource<SessionState>

    suspend fun endSession(sessionId: String): Resource<SessionSummary>
}
