package com.pfa.interview.data.remote.api

import com.pfa.interview.data.remote.dto.AdminDashboardDto
import com.pfa.interview.data.remote.dto.AdminUserCreateRequest
import com.pfa.interview.data.remote.dto.AdminUserUpdateRequest
import com.pfa.interview.data.remote.dto.UserAdminDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AdminApi {
    @GET("admin/dashboard")
    suspend fun getDashboard(): Response<AdminDashboardDto>

    @GET("admin/users")
    suspend fun getUsers(): Response<List<UserAdminDto>>

    @POST("admin/users")
    suspend fun createUser(@Body request: AdminUserCreateRequest): Response<UserAdminDto>

    @PUT("admin/users/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: String,
        @Body request: AdminUserUpdateRequest
    ): Response<Unit>

    @DELETE("admin/users/{userId}")
    suspend fun deleteUser(@Path("userId") userId: String): Response<Unit>

    @DELETE("admin/sessions/{sessionId}")
    suspend fun deleteSession(@Path("sessionId") sessionId: String): Response<Unit>

    @POST("admin/users/{userId}/promote")
    suspend fun promoteUser(@Path("userId") userId: String): Response<Unit>

    @POST("admin/users/{userId}/demote")
    suspend fun demoteUser(@Path("userId") userId: String): Response<Unit>
}
