package com.pfa.interview.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.local.AiProviderPreference
import com.pfa.interview.data.local.TokenDataStore
import com.pfa.interview.data.model.HealthStatus
import com.pfa.interview.data.remote.api.ProfileApi
import com.pfa.interview.data.remote.dto.UpdateProfileRequest
import com.pfa.interview.data.repository.AuthRepository
import com.pfa.interview.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val targetJob: String = "",
    val notificationsEnabled: Boolean = true,
    val isLoggedOut: Boolean = false,
    val isAdmin: Boolean = false,
    // AI provider
    val selectedProvider: String = "ollama",
    val aiHealth: HealthStatus? = null,
    val aiHealthLoading: Boolean = false,
    val aiHealthError: String? = null,
    // Profile editing
    val isEditing: Boolean = false,
    val editName: String = "",
    val editTargetPosition: String = "",
    val saveLoading: Boolean = false,
    val saveError: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenDataStore: TokenDataStore,
    private val healthRepository: HealthRepository,
    private val aiProviderPreference: AiProviderPreference,
    private val profileApi: ProfileApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadAiHealth()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val name = tokenDataStore.getUserName() ?: ""
            val isAdmin = tokenDataStore.isAdmin()
            val provider = aiProviderPreference.aiProvider.first()
            _uiState.update {
                it.copy(
                    name = name,
                    isAdmin = isAdmin,
                    selectedProvider = provider
                )
            }
        }
    }

    fun loadAiHealth() {
        viewModelScope.launch {
            _uiState.update { it.copy(aiHealthLoading = true, aiHealthError = null) }
            when (val result = healthRepository.checkHealth()) {
                is Resource.Success -> _uiState.update {
                    it.copy(aiHealth = result.data, aiHealthLoading = false)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(aiHealthLoading = false, aiHealthError = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun switchProvider(provider: String) {
        viewModelScope.launch {
            aiProviderPreference.setAiProvider(provider)
            _uiState.update { it.copy(selectedProvider = provider) }
            loadAiHealth()
        }
    }

    fun enterEditMode() {
        val s = _uiState.value
        _uiState.update {
            it.copy(
                isEditing = true,
                editName = s.name,
                editTargetPosition = s.targetJob,
                saveError = null,
                saveSuccess = false
            )
        }
    }

    fun cancelEdit() = _uiState.update { it.copy(isEditing = false, saveError = null) }

    fun onEditNameChange(name: String) = _uiState.update { it.copy(editName = name) }
    fun onEditTargetChange(target: String) = _uiState.update { it.copy(editTargetPosition = target) }

    fun saveProfile() {
        val s = _uiState.value
        if (s.editName.isBlank()) {
            _uiState.update { it.copy(saveError = "Le nom ne peut pas être vide") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saveLoading = true, saveError = null) }
            try {
                val resp = profileApi.updateProfile(
                    UpdateProfileRequest(name = s.editName.trim(), targetPosition = s.editTargetPosition.trim())
                )
                if (resp.isSuccessful) {
                    tokenDataStore.saveUserName(s.editName.trim())
                    _uiState.update {
                        it.copy(
                            name = s.editName.trim(),
                            targetJob = s.editTargetPosition.trim(),
                            isEditing = false,
                            saveLoading = false,
                            saveSuccess = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(saveLoading = false, saveError = "Erreur ${resp.code()}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(saveLoading = false, saveError = e.message ?: "Erreur réseau")
                }
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) = _uiState.update { it.copy(notificationsEnabled = enabled) }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }
}
