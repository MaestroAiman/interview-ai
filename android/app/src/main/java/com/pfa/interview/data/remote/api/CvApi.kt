package com.pfa.interview.data.remote.api

import com.pfa.interview.data.remote.dto.CvAnalysisResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface CvApi {
    @Multipart
    @POST("cv/analyze")
    suspend fun analyzeCv(@Part file: MultipartBody.Part): Response<CvAnalysisResponse>
}
