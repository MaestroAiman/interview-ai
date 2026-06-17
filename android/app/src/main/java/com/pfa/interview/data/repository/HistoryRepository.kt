package com.pfa.interview.data.repository

import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.model.HistoryDetails
import com.pfa.interview.data.model.HistoryPage
import com.pfa.interview.data.model.InterviewSession
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getSessions(): Flow<Resource<List<InterviewSession>>>
    suspend fun getSessionsOnce(): Resource<List<InterviewSession>>
    suspend fun getHistoryPage(page: Int, size: Int): Resource<HistoryPage>
    suspend fun getHistoryDetails(sessionId: String): Resource<HistoryDetails>
}
