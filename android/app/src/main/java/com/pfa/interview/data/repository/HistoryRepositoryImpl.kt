package com.pfa.interview.data.repository

import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.model.HistoryDetails
import com.pfa.interview.data.model.HistoryPage
import com.pfa.interview.data.model.InterviewSession
import com.pfa.interview.data.remote.api.InterviewApi
import com.pfa.interview.data.remote.api.SessionApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class HistoryRepositoryImpl @Inject constructor(
    private val sessionApi: SessionApi,
    private val interviewApi: InterviewApi
) : HistoryRepository {

    override fun getSessions(): Flow<Resource<List<InterviewSession>>> = flow {
        emit(Resource.Loading())
        try {
            val r = interviewApi.getHistoryPage(0, 100)
            if (r.isSuccessful) {
                emit(Resource.Success(r.body()?.sessions ?: emptyList()))
            } else {
                emit(Resource.Error("Erreur serveur: ${r.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Erreur réseau"))
        }
    }

    override suspend fun getSessionsOnce(): Resource<List<InterviewSession>> {
        return try {
            val response = sessionApi.getHistory()
            if (response.isSuccessful) Resource.Success(response.body() ?: emptyList())
            else Resource.Error("Failed to load history (${response.code()})")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun getHistoryPage(page: Int, size: Int): Resource<HistoryPage> {
        return try {
            val r = interviewApi.getHistoryPage(page, size)
            if (r.isSuccessful) Resource.Success(r.body()!!)
            else Resource.Error("Error: ${r.code()}")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    override suspend fun getHistoryDetails(sessionId: String): Resource<HistoryDetails> {
        return try {
            val r = interviewApi.getHistoryDetails(sessionId)
            if (r.isSuccessful) Resource.Success(r.body()!!)
            else Resource.Error("Error: ${r.code()}")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}
