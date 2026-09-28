package com.pfa.interview.data.remote.api

import com.pfa.interview.data.model.HealthStatus
import com.pfa.interview.data.model.HistoryDetails
import com.pfa.interview.data.model.HistoryPage
import com.pfa.interview.data.model.SessionState
import com.pfa.interview.data.model.SessionSummary
import com.pfa.interview.data.remote.dto.AnswerFeedbackResponse
import com.pfa.interview.data.remote.dto.StartSessionRequest
import com.pfa.interview.data.remote.dto.StartSessionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface InterviewApi {

    @POST("sessions/start")
    suspend fun startAdaptiveSession(@Body request: StartSessionRequest): Response<StartSessionResponse>

    @POST("sessions/{sessionId}/answer")
    suspend fun submitAdaptiveAnswer(
        @Path("sessionId") sessionId: String,
        @Body body: Map<String, String>
    ): Response<AnswerFeedbackResponse>

    @GET("sessions/{sessionId}")
    suspend fun getSessionState(@Path("sessionId") sessionId: String): Response<SessionState>

    @POST("sessions/{sessionId}/end")
    suspend fun endSession(@Path("sessionId") sessionId: String): Response<SessionSummary>

    @GET("history")
    suspend fun getHistoryPage(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<HistoryPage>

    @GET("history/{sessionId}/details")
    suspend fun getHistoryDetails(@Path("sessionId") sessionId: String): Response<HistoryDetails>

    @GET("health/ai")
    suspend fun checkAiHealth(): Response<HealthStatus>
}
