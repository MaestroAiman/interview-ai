package com.pfa.interview.data.repository

import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.model.HealthStatus
import com.pfa.interview.data.remote.api.InterviewApi
import javax.inject.Inject

class HealthRepositoryImpl @Inject constructor(
    private val api: InterviewApi
) : HealthRepository {
    override suspend fun checkHealth(): Resource<HealthStatus> = try {
        val resp = api.checkAiHealth()
        if (resp.isSuccessful) Resource.Success(resp.body()!!)
        else Resource.Error("Erreur ${resp.code()}")
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Service IA indisponible")
    }
}
