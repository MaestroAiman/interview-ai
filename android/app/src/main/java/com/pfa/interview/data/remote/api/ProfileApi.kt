package com.pfa.interview.data.remote.api

import com.pfa.interview.data.remote.dto.UpdateProfileRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT

interface ProfileApi {
    @PUT("profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<Unit>
}
