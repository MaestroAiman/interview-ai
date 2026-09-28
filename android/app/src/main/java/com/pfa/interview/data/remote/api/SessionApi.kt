package com.pfa.interview.data.remote.api

import com.pfa.interview.data.model.InterviewSession
import com.pfa.interview.data.remote.dto.AnswerResponse
import com.pfa.interview.data.remote.dto.SessionResults
import com.pfa.interview.data.remote.dto.StartSessionRequest
import com.pfa.interview.data.remote.dto.StartSessionResponse
import com.pfa.interview.data.remote.dto.SubmitAnswerRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SessionApi {
    @POST("session/start")
    suspend fun startSession(@Body request: StartSessionRequest): Response<StartSessionResponse>

    @POST("session/answer")
    suspend fun submitAnswer(@Body request: SubmitAnswerRequest): Response<AnswerResponse>

    @GET("session/{sessionId}/results")
    suspend fun getResults(@Path("sessionId") sessionId: String): Response<SessionResults>

    @GET("session/history")
    suspend fun getHistory(): Response<List<InterviewSession>>
}
