package com.pfa.interview.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.local.TokenDataStore
import com.pfa.interview.data.remote.dto.AdminUserCreateRequest
import com.pfa.interview.data.remote.dto.AdminUserUpdateRequest
import com.pfa.interview.data.remote.dto.UserAdminDto
import com.pfa.interview.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUsersUiState(
    val users: List<UserAdminDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUserId: String? = null,
    // Dialog visibility
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingUser: UserAdminDto? = null,
    val pendingDeleteUserId: String? = null,
    // Shared form fields
    val formName: String = "",
    val formEmail: String = "",
    val formPassword: String = "",
    val formRole: String = "USER",
    val formTargetPosition: String = "",
    val formError: String? = null,
    val isSubmitting: Boolean = false
)

@HiltViewModel
class AdminUsersViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUsersUiState())
    val uiState: StateFlow<AdminUsersUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val currentId = tokenDataStore.getUserId()
            when (val result = adminRepository.getUsers()) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, users = result.data, currentUserId = currentId)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    // ── Role management ──────────────────────────────────────────────────────

    fun promote(userId: String) {
        viewModelScope.launch {
            when (val result = adminRepository.promoteUser(userId)) {
                is Resource.Success -> loadData()
                is Resource.Error -> _uiState.update { it.copy(error = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun demote(userId: String) {
        viewModelScope.launch {
            when (val result = adminRepository.demoteUser(userId)) {
                is Resource.Success -> loadData()
                is Resource.Error -> _uiState.update { it.copy(error = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    // ── Create dialog ────────────────────────────────────────────────────────

    fun openCreateDialog() = _uiState.update {
        it.copy(
            showCreateDialog = true,
            showEditDialog = false,
            editingUser = null,
            formName = "",
            formEmail = "",
            formPassword = "",
            formRole = "USER",
            formTargetPosition = "",
            formError = null
        )
    }

    fun submitCreate() {
        val s = _uiState.value
        if (s.formName.isBlank() || s.formEmail.isBlank() || s.formPassword.isBlank()) {
            _uiState.update { it.copy(formError = "Name, email and password are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, formError = null) }
            val request = AdminUserCreateRequest(
                name = s.formName.trim(),
                email = s.formEmail.trim(),
                password = s.formPassword,
                role = s.formRole,
                targetPosition = s.formTargetPosition.trim().ifBlank { null }
            )
            when (val result = adminRepository.createUser(request)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, showCreateDialog = false) }
                    loadData()
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isSubmitting = false, formError = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    // ── Edit dialog ──────────────────────────────────────────────────────────

    fun openEditDialog(user: UserAdminDto) = _uiState.update {
        it.copy(
            showEditDialog = true,
            showCreateDialog = false,
            editingUser = user,
            formName = user.name,
            formEmail = user.email,
            formPassword = "",
            formRole = user.role,
            formTargetPosition = user.targetPosition ?: "",
            formError = null
        )
    }

    fun submitEdit() {
        val s = _uiState.value
        val userId = s.editingUser?.id ?: return
        if (s.formName.isBlank() || s.formEmail.isBlank()) {
            _uiState.update { it.copy(formError = "Name and email are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, formError = null) }
            val request = AdminUserUpdateRequest(
                name = s.formName.trim(),
                email = s.formEmail.trim(),
                targetPosition = s.formTargetPosition.trim().ifBlank { null },
                role = s.formRole
            )
            when (val result = adminRepository.updateUser(userId, request)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, showEditDialog = false) }
                    loadData()
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isSubmitting = false, formError = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    fun requestDeleteUser(userId: String) =
        _uiState.update { it.copy(pendingDeleteUserId = userId) }

    fun cancelDeleteUser() =
        _uiState.update { it.copy(pendingDeleteUserId = null) }

    fun confirmDeleteUser() {
        val userId = _uiState.value.pendingDeleteUserId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(pendingDeleteUserId = null) }
            when (val result = adminRepository.deleteUser(userId)) {
                is Resource.Success -> loadData()
                is Resource.Error -> _uiState.update { it.copy(error = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    // ── Form field updates ───────────────────────────────────────────────────

    fun onFormNameChange(v: String)           = _uiState.update { it.copy(formName = v) }
    fun onFormEmailChange(v: String)          = _uiState.update { it.copy(formEmail = v) }
    fun onFormPasswordChange(v: String)       = _uiState.update { it.copy(formPassword = v) }
    fun onFormRoleChange(v: String)           = _uiState.update { it.copy(formRole = v) }
    fun onFormTargetPositionChange(v: String) = _uiState.update { it.copy(formTargetPosition = v) }

    // ── Misc ─────────────────────────────────────────────────────────────────

    fun closeDialogs() = _uiState.update {
        it.copy(
            showCreateDialog = false,
            showEditDialog = false,
            editingUser = null,
            formError = null,
            isSubmitting = false
        )
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
