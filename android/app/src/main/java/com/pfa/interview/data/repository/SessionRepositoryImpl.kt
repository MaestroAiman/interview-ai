package com.pfa.interview.data.repository

import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.model.HistoryDetails
import com.pfa.interview.data.model.SessionState
import com.pfa.interview.data.model.SessionSummary
import com.pfa.interview.data.remote.api.InterviewApi
import com.pfa.interview.data.remote.api.SessionApi
import com.pfa.interview.data.remote.dto.AnswerFeedbackResponse
import com.pfa.interview.data.remote.dto.AnswerResponse
import com.pfa.interview.data.remote.dto.QuestionReview
import com.pfa.interview.data.remote.dto.SessionResults
import com.pfa.interview.data.remote.dto.StartSessionRequest
import com.pfa.interview.data.remote.dto.StartSessionResponse
import com.pfa.interview.data.remote.dto.SubmitAnswerRequest
import retrofit2.Response
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionApi: SessionApi,
    private val interviewApi: InterviewApi
) : SessionRepository {

    override suspend fun startSession(
        type: String,
        position: String,
        difficulty: String,
        questionCount: Int
    ): Resource<StartSessionResponse> {
        return try {
            val response = sessionApi.startSession(
                StartSessionRequest(type, position, difficulty, questionCount)
            )
            if (response.isSuccessful) Resource.Success(response.body()!!)
            else Resource.Error("Failed to start session (${response.code()})")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun submitAnswer(
        sessionId: String,
        questionId: String,
        answerText: String
    ): Resource<AnswerResponse> {
        return try {
            val response = sessionApi.submitAnswer(
                SubmitAnswerRequest(sessionId, questionId, answerText)
            )
            if (response.isSuccessful) Resource.Success(response.body()!!)
            else Resource.Error("Failed to submit answer (${response.code()})")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun getResults(sessionId: String): Resource<SessionResults> {
        return when (val r = safeCall { interviewApi.getHistoryDetails(sessionId) }) {
            is Resource.Success -> Resource.Success(r.data.toSessionResults())
            is Resource.Error -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun startAdaptiveSession(
        type: String,
        position: String,
        difficulty: String,
        questionCount: Int
    ): Resource<StartSessionResponse> = safeCall {
        interviewApi.startAdaptiveSession(StartSessionRequest(type, position, difficulty, questionCount))
    }

    override suspend fun submitAdaptiveAnswer(
        sessionId: String,
        questionId: String,
        answerText: String
    ): Resource<AnswerFeedbackResponse> = safeCall {
        interviewApi.submitAdaptiveAnswer(
            sessionId,
            mapOf("questionId" to questionId, "answerText" to answerText)
        )
    }

    override suspend fun getSessionState(sessionId: String): Resource<SessionState> = safeCall {
        interviewApi.getSessionState(sessionId)
    }

    override suspend fun endSession(sessionId: String): Resource<SessionSummary> = safeCall {
        interviewApi.endSession(sessionId)
    }

    private fun HistoryDetails.toSessionResults(): SessionResults {
        val reviews = questions.sortedBy { it.order }.map { q ->
            val answer = answers.firstOrNull { it.questionId == q.id }
            val fb = feedbacks.firstOrNull { it.questionId == q.id }
            QuestionReview(
                question = q.content,
                answer = answer?.answerText ?: "",
                feedback = fb?.comments ?: "",
                score = fb?.globalScore ?: 0f
            )
        }
        return SessionResults(
            sessionId = session.id,
            type = session.type,
            position = session.position,
            difficulty = session.difficulty,
            overallScore = session.overallScore,
            relevanceAvg = feedbacks.map { it.relevanceScore }.average()
                .takeIf { feedbacks.isNotEmpty() }?.toFloat() ?: 0f,
            clarityAvg = feedbacks.map { it.clarityScore }.average()
                .takeIf { feedbacks.isNotEmpty() }?.toFloat() ?: 0f,
            sentimentAvg = feedbacks.map { it.sentimentScore }.average()
                .takeIf { feedbacks.isNotEmpty() }?.toFloat() ?: 0f,
            questionReviews = reviews
        )
    }

    private suspend fun <T> safeCall(call: suspend () -> Response<T>): Resource<T> {
        return try {
            val r = call()
            if (r.isSuccessful) Resource.Success(r.body()!!)
            else {
                val body = r.errorBody()?.string() ?: ""
                Resource.Error("Server error ${r.code()}: $body")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}
