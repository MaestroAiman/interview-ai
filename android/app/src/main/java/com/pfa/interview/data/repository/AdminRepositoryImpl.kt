package com.pfa.interview.data.repository

import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.remote.api.AdminApi
import com.pfa.interview.data.remote.dto.AdminDashboardDto
import com.pfa.interview.data.remote.dto.AdminUserCreateRequest
import com.pfa.interview.data.remote.dto.AdminUserUpdateRequest
import com.pfa.interview.data.remote.dto.UserAdminDto
import javax.inject.Inject

class AdminRepositoryImpl @Inject constructor(
    private val adminApi: AdminApi
) : AdminRepository {

    override suspend fun getDashboard(): Resource<AdminDashboardDto> =
        safeCall { adminApi.getDashboard() }

    override suspend fun getUsers(): Resource<List<UserAdminDto>> =
        safeCall { adminApi.getUsers() }

    override suspend fun createUser(request: AdminUserCreateRequest): Resource<UserAdminDto> =
        safeCall { adminApi.createUser(request) }

    override suspend fun updateUser(userId: String, request: AdminUserUpdateRequest): Resource<Unit> =
        safeCallUnit { adminApi.updateUser(userId, request) }

    override suspend fun deleteUser(userId: String): Resource<Unit> =
        safeCallUnit { adminApi.deleteUser(userId) }

    override suspend fun promoteUser(userId: String): Resource<Unit> =
        safeCallUnit { adminApi.promoteUser(userId) }

    override suspend fun demoteUser(userId: String): Resource<Unit> =
        safeCallUnit { adminApi.demoteUser(userId) }

    override suspend fun deleteSession(sessionId: String): Resource<Unit> =
        safeCallUnit { adminApi.deleteSession(sessionId) }

    private suspend fun <T> safeCall(block: suspend () -> retrofit2.Response<T>): Resource<T> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun safeCallUnit(block: suspend () -> retrofit2.Response<Unit>): Resource<Unit> {
        return try {
            val response = block()
            if (response.isSuccessful) Resource.Success(Unit)
            else Resource.Error("Server error: ${response.code()}")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }
}
