package com.pfa.interview.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.repository.HistoryRepository
import com.pfa.interview.ui.setup.InterviewType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressUiState(
    val scoreTrend: List<Pair<String, Float>> = emptyList(),
    val typeAverages: Map<InterviewType, Float> = emptyMap(),
    val practiceCalendar: Map<String, Int> = emptyMap(),
    val totalSessions: Int = 0,
    val bestScore: Float = 0f,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadProgress()
    }

    private fun loadProgress() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            historyRepository.getSessions().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val sessions = result.data.filter { it.overallScore > 0 }
                        val trend = sessions.takeLast(10).mapIndexed { i, s ->
                            Pair("#${i + 1}", s.overallScore)
                        }
                        val typeAvgs = InterviewType.values().associateWith { type ->
                            val typeSessions = sessions.filter { it.type.equals(type.name, ignoreCase = true) }
                            if (typeSessions.isEmpty()) 0f
                            else typeSessions.map { it.overallScore }.average().toFloat()
                        }
                        val calendar = sessions.groupBy { it.startedAt.take(10) }
                            .mapValues { it.value.size }
                        _uiState.update {
                            it.copy(
                                scoreTrend = trend,
                                typeAverages = typeAvgs,
                                practiceCalendar = calendar,
                                totalSessions = sessions.size,
                                bestScore = sessions.maxOfOrNull { it.overallScore } ?: 0f,
                                isLoading = false
                            )
                        }
                    }
                    is Resource.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                    is Resource.Loading -> Unit
                }
            }
        }
    }
}
