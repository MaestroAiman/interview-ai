package com.pfa.interview.data.remote.api

import com.pfa.interview.data.remote.dto.FeedbackResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface FeedbackApi {
    @GET("feedback/{sessionId}")
    suspend fun getFeedback(@Path("sessionId") sessionId: String): Response<List<FeedbackResponse>>
}
