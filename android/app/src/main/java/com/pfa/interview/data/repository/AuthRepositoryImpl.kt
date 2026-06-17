package com.pfa.interview.data.repository

import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.local.TokenDataStore
import com.pfa.interview.data.remote.api.AuthApi
import com.pfa.interview.data.remote.dto.LoginRequest
import com.pfa.interview.data.remote.dto.LoginResponse
import com.pfa.interview.data.remote.dto.RegisterRequest
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenDataStore: TokenDataStore
) : AuthRepository {

    override suspend fun login(email: String, password: String): Resource<LoginResponse> {
        return try {
            val response = authApi.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenDataStore.saveToken(body.token)
                tokenDataStore.saveUserId(body.userId)
                tokenDataStore.saveUserName(body.name)
                tokenDataStore.saveUserRole(body.role)
                Resource.Success(body)
            } else {
                Resource.Error(parseError(response.code()))
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun register(name: String, email: String, password: String): Resource<LoginResponse> {
        return try {
            val response = authApi.register(RegisterRequest(name, email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenDataStore.saveToken(body.token)
                tokenDataStore.saveUserId(body.userId)
                tokenDataStore.saveUserName(body.name)
                tokenDataStore.saveUserRole(body.role)
                Resource.Success(body)
            } else {
                Resource.Error(parseError(response.code()))
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun logout(): Resource<Unit> {
        return try {
            authApi.logout()
            tokenDataStore.clearAll()
            Resource.Success(Unit)
        } catch (e: Exception) {
            tokenDataStore.clearAll()
            Resource.Success(Unit)
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return tokenDataStore.getToken() != null
    }

    private fun parseError(code: Int): String = when (code) {
        401 -> "Invalid credentials"
        403 -> "Access denied"
        404 -> "Account not found"
        409 -> "Email already registered"
        500 -> "Server error, please try again"
        else -> "Something went wrong (code $code)"
    }
}
