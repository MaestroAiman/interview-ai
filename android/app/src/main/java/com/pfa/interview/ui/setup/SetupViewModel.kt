package com.pfa.interview.ui.setup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.local.CurrentSessionHolder
import com.pfa.interview.data.local.PendingSession
import com.pfa.interview.data.remote.dto.CvAnalysisResponse
import com.pfa.interview.data.repository.CvRepository
import com.pfa.interview.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class InterviewType(val display: String) {
    TECHNICAL("Technical"),
    HR("HR"),
    DOMAIN("Domain")
}

enum class Difficulty(val display: String) {
    JUNIOR("Junior"),
    MID("Mid-level"),
    SENIOR("Senior")
}

data class SetupUiState(
    val currentStep: Int = 1,
    val totalSteps: Int = 5,
    val selectedType: InterviewType? = null,
    val selectedPosition: String = "",
    val difficulty: Difficulty = Difficulty.MID,
    val questionCount: Int = 10,
    val isLoading: Boolean = false,
    val sessionId: String? = null,
    val error: String? = null,
    val cvState: Resource<CvAnalysisResponse>? = null,
    val cvApplied: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val currentSessionHolder: CurrentSessionHolder,
    private val cvRepository: CvRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun selectType(type: InterviewType) = _uiState.update { it.copy(selectedType = type) }
    fun onPositionChange(position: String) = _uiState.update { it.copy(selectedPosition = position) }
    fun selectDifficulty(diff: Difficulty) = _uiState.update { it.copy(difficulty = diff) }
    fun onQuestionCountChange(count: Int) = _uiState.update { it.copy(questionCount = count) }

    fun nextStep() {
        val state = _uiState.value
        if (state.currentStep < state.totalSteps) {
            _uiState.update { it.copy(currentStep = it.currentStep + 1) }
        }
    }

    fun previousStep() {
        val state = _uiState.value
        if (state.currentStep > 1) {
            _uiState.update { it.copy(currentStep = it.currentStep - 1) }
        }
    }

    fun analyzeCv(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(cvState = Resource.Loading()) }
            val result = cvRepository.analyzeCv(uri, context)
            _uiState.update { it.copy(cvState = result) }
        }
    }

    fun applyCvRecommendations(r: CvAnalysisResponse) {
        val type = runCatching { InterviewType.valueOf(r.type) }.getOrNull()
        val diff = runCatching { Difficulty.valueOf(r.difficulty) }.getOrNull() ?: Difficulty.MID
        _uiState.update {
            it.copy(
                selectedType = type ?: it.selectedType,
                difficulty = diff,
                selectedPosition = r.position.ifBlank { it.selectedPosition },
                cvApplied = true,
                cvState = null
            )
        }
    }

    fun dismissCv() = _uiState.update { it.copy(cvState = null) }

    fun startSession() {
        val state = _uiState.value
        val type = state.selectedType ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = sessionRepository.startAdaptiveSession(
                type.name,
                state.selectedPosition,
                state.difficulty.name,
                state.questionCount
            )) {
                is Resource.Success -> {
                    currentSessionHolder.set(
                        PendingSession(
                            sessionId = result.data.sessionId,
                            firstQuestion = result.data.firstQuestion,
                            totalQuestions = state.questionCount
                        )
                    )
                    _uiState.update { it.copy(isLoading = false, sessionId = result.data.sessionId) }
                }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
