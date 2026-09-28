package com.pfa.interview.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfa.interview.data.remote.dto.UserAdminDto
import com.pfa.interview.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    onBack: () -> Unit,
    viewModel: AdminUsersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // ── Delete confirmation dialog ───────────────────────────────────────────
    if (uiState.pendingDeleteUserId != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteUser,
            containerColor = NavyCard,
            title = { Text("Delete User", color = TextPrimary) },
            text = {
                Text(
                    "This will permanently delete the user account.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteUser) {
                    Text("Delete", color = Rose)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteUser) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // ── Create / Edit dialog ─────────────────────────────────────────────────
    val showDialog = uiState.showCreateDialog || uiState.showEditDialog
    if (showDialog) {
        val isCreate = uiState.showCreateDialog
        AlertDialog(
            onDismissRequest = viewModel::closeDialogs,
            containerColor = NavyCard,
            title = {
                Text(
                    if (isCreate) "Create User" else "Edit User",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = ElectricBlue,
                        focusedLabelColor = ElectricBlue,
                        unfocusedLabelColor = TextSecondary
                    )

                    OutlinedTextField(
                        value = uiState.formName,
                        onValueChange = viewModel::onFormNameChange,
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                    OutlinedTextField(
                        value = uiState.formEmail,
                        onValueChange = viewModel::onFormEmailChange,
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )
                    if (isCreate) {
                        OutlinedTextField(
                            value = uiState.formPassword,
                            onValueChange = viewModel::onFormPasswordChange,
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors
                        )
                    }
                    OutlinedTextField(
                        value = uiState.formTargetPosition,
                        onValueChange = viewModel::onFormTargetPositionChange,
                        label = { Text("Target Position (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors
                    )

                    // Role selector
                    Text(
                        "Role",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("USER", "ADMIN").forEach { role ->
                            val selected = uiState.formRole == role
                            OutlinedButton(
                                onClick = { viewModel.onFormRoleChange(role) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) ElectricBlue.copy(alpha = 0.15f)
                                                    else androidx.compose.ui.graphics.Color.Transparent,
                                    contentColor = if (selected) ElectricBlue else TextSecondary
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) ElectricBlue else NavyBorder
                                )
                            ) {
                                Text(role, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    uiState.formError?.let { err ->
                        Text(err, color = Rose, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { if (isCreate) viewModel.submitCreate() else viewModel.submitEdit() },
                    enabled = !uiState.isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (isCreate) "Create" else "Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeDialogs) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        containerColor = DeepNavy,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "User Management",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadData) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyCard)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openCreateDialog,
                containerColor = ElectricBlue,
                contentColor = TextPrimary
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Create User")
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ElectricBlue)
            }
            uiState.users.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No users found.", color = TextSecondary)
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "${uiState.users.size} registered users",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(uiState.users, key = { it.id }) { user ->
                    UserAdminCard(
                        user = user,
                        isSelf = user.id == uiState.currentUserId,
                        onPromote = { viewModel.promote(user.id) },
                        onDemote = { viewModel.demote(user.id) },
                        onEdit = { viewModel.openEditDialog(user) },
                        onDelete = { viewModel.requestDeleteUser(user.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserAdminCard(
    user: UserAdminDto,
    isSelf: Boolean,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isAdmin = user.role == "ADMIN"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            user.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (isSelf) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "(you)",
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Text(
                        user.email,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!user.targetPosition.isNullOrBlank()) {
                        Text(
                            user.targetPosition,
                            color = TextMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isAdmin) ElectricBlue.copy(alpha = 0.15f) else NavyBorder
                    ) {
                        Text(
                            user.role,
                            color = if (isAdmin) ElectricBlue else TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Role toggle
                if (!isSelf) {
                    Spacer(Modifier.width(8.dp))
                    if (isAdmin) {
                        Button(
                            onClick = onDemote,
                            colors = ButtonDefaults.buttonColors(containerColor = Rose),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Demote", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        Button(
                            onClick = onPromote,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Promote", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Edit / Delete row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (!isSelf) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Rose,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
