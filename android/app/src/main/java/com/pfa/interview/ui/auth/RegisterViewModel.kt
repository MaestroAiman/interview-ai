package com.pfa.interview.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PasswordStrength { NONE, WEAK, FAIR, STRONG, VERY_STRONG }

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val termsAccepted: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.NONE,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, error = null) }

    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email, error = null) }

    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(password = password, passwordStrength = evaluateStrength(password), error = null)
        }
    }

    fun onConfirmPasswordChange(confirm: String) =
        _uiState.update { it.copy(confirmPassword = confirm, error = null) }

    fun onTermsToggle(accepted: Boolean) = _uiState.update { it.copy(termsAccepted = accepted) }

    fun register() {
        val state = _uiState.value
        when {
            state.name.isBlank() -> { _uiState.update { it.copy(error = "Name is required") }; return }
            state.email.isBlank() -> { _uiState.update { it.copy(error = "Email is required") }; return }
            state.password.length < 6 -> { _uiState.update { it.copy(error = "Password must be at least 6 characters") }; return }
            state.password != state.confirmPassword -> { _uiState.update { it.copy(error = "Passwords do not match") }; return }
            !state.termsAccepted -> { _uiState.update { it.copy(error = "Please accept the terms") }; return }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.register(state.name.trim(), state.email.trim(), state.password)) {
                is Resource.Success -> _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun evaluateStrength(password: String): PasswordStrength {
        if (password.length < 4) return PasswordStrength.WEAK
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return when (score) {
            0, 1 -> PasswordStrength.WEAK
            2 -> PasswordStrength.FAIR
            3 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }
}
