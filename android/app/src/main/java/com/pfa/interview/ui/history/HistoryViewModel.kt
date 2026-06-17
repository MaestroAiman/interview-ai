package com.pfa.interview.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.model.InterviewSession
import com.pfa.interview.data.repository.HistoryRepository
import com.pfa.interview.ui.setup.InterviewType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val sessions: List<InterviewSession> = emptyList(),
    val filteredSessions: List<InterviewSession> = emptyList(),
    val selectedFilter: InterviewType? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    private fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            historyRepository.getSessions().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val sessions = result.data
                        _uiState.update {
                            it.copy(
                                sessions = sessions,
                                filteredSessions = applyFilter(sessions, it.selectedFilter),
                                isLoading = false
                            )
                        }
                    }
                    is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun refresh() = loadSessions()

    fun filterByType(type: InterviewType?) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = type,
                filteredSessions = applyFilter(state.sessions, type)
            )
        }
    }

    private fun applyFilter(sessions: List<InterviewSession>, type: InterviewType?): List<InterviewSession> {
        return if (type == null) sessions
        else sessions.filter { it.type.equals(type.name, ignoreCase = true) }
    }
}
