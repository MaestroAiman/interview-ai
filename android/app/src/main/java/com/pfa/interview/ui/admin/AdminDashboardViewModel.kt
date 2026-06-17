package com.pfa.interview.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.remote.dto.AdminDashboardDto
import com.pfa.interview.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminDashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalUsers: Int = 0,
    val completedCount: Long = 0,
    val inProgressCount: Long = 0,
    val platformAvgScore: Float = 0f,
    val sessions: List<AdminDashboardDto.SessionRowDto> = emptyList(),
    val pendingDeleteSessionId: String? = null,
    val isDeleting: Boolean = false
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = adminRepository.getDashboard()) {
                is Resource.Success -> {
                    val d = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            totalUsers = d.totalUsers,
                            completedCount = d.completedCount,
                            inProgressCount = d.inProgressCount,
                            platformAvgScore = d.platformAvgScore,
                            sessions = d.sessions
                        )
                    }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun requestDeleteSession(sessionId: String) =
        _uiState.update { it.copy(pendingDeleteSessionId = sessionId) }

    fun cancelDeleteSession() =
        _uiState.update { it.copy(pendingDeleteSessionId = null) }

    fun confirmDeleteSession() {
        val sessionId = _uiState.value.pendingDeleteSessionId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, pendingDeleteSessionId = null) }
            when (val r = adminRepository.deleteSession(sessionId)) {
                is Resource.Success -> loadDashboard()
                is Resource.Error -> _uiState.update { it.copy(isDeleting = false, error = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }
}
