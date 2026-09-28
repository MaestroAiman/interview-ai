package com.pfa.interview.data.repository

import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.model.HealthStatus

interface HealthRepository {
    suspend fun checkHealth(): Resource<HealthStatus>
}
