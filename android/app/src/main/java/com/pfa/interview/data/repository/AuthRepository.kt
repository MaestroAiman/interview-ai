package com.pfa.interview.data.repository

import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.remote.dto.LoginResponse

interface AuthRepository {
    suspend fun login(email: String, password: String): Resource<LoginResponse>
    suspend fun register(name: String, email: String, password: String): Resource<LoginResponse>
    suspend fun logout(): Resource<Unit>
    suspend fun isLoggedIn(): Boolean
}
