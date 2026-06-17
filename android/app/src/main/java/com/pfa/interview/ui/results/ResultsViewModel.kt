package com.pfa.interview.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.local.CurrentSessionHolder
import com.pfa.interview.data.model.SessionSummary
import com.pfa.interview.data.remote.dto.SessionResults
import com.pfa.interview.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultsUiState(
    val results: SessionResults? = null,
    val sessionSummary: SessionSummary? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val currentSessionHolder: CurrentSessionHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    init {
        val summary = currentSessionHolder.consumeSummary()
        if (summary != null) {
            _uiState.update { it.copy(sessionSummary = summary) }
        }
    }

    fun loadResults(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = sessionRepository.getResults(sessionId)) {
                is Resource.Success -> _uiState.update { it.copy(results = result.data, isLoading = false) }
                is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                is Resource.Loading -> Unit
            }
        }
    }
}
