package com.pfa.interview.data.remote.api

import com.pfa.interview.data.remote.dto.LoginRequest
import com.pfa.interview.data.remote.dto.LoginResponse
import com.pfa.interview.data.remote.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}
