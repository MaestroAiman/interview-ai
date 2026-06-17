package com.pfa.interview.data.repository

import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.remote.dto.AdminDashboardDto
import com.pfa.interview.data.remote.dto.AdminUserCreateRequest
import com.pfa.interview.data.remote.dto.AdminUserUpdateRequest
import com.pfa.interview.data.remote.dto.UserAdminDto

interface AdminRepository {
    suspend fun getDashboard(): Resource<AdminDashboardDto>
    suspend fun getUsers(): Resource<List<UserAdminDto>>
    suspend fun createUser(request: AdminUserCreateRequest): Resource<UserAdminDto>
    suspend fun updateUser(userId: String, request: AdminUserUpdateRequest): Resource<Unit>
    suspend fun deleteUser(userId: String): Resource<Unit>
    suspend fun promoteUser(userId: String): Resource<Unit>
    suspend fun demoteUser(userId: String): Resource<Unit>
    suspend fun deleteSession(sessionId: String): Resource<Unit>
}
