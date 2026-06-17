package com.pfa.interview.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfa.interview.ui.theme.*

@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    onAdminDashboard: () -> Unit = {},
    onAdminUsers: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) onLoggedOut()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (!uiState.isEditing) {
                IconButton(onClick = viewModel::enterEditMode) {
                    Icon(Icons.Default.Edit, null, tint = TextSecondary)
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        uiState.name.take(2).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.isEditing) {
                    OutlinedTextField(
                        value = uiState.editName,
                        onValueChange = viewModel::onEditNameChange,
                        label = { Text("Nom", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = ElectricBlue
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.editTargetPosition,
                        onValueChange = viewModel::onEditTargetChange,
                        label = { Text("Poste cible", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Work, null, tint = ElectricBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = ElectricBlue
                        )
                    )
                    uiState.saveError?.let { error ->
                        Text(error, color = Rose, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = viewModel::cancelEdit,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder)
                        ) { Text("Annuler") }
                        Button(
                            onClick = viewModel::saveProfile,
                            enabled = !uiState.saveLoading,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            if (uiState.saveLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextPrimary, strokeWidth = 2.dp)
                            } else {
                                Text("Sauvegarder")
                            }
                        }
                    }
                } else {
                    Text(uiState.name, style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    if (uiState.email.isNotEmpty()) {
                        Text(uiState.email, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    if (uiState.targetJob.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Work, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(uiState.targetJob, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection(title = "Preferences") {
            ToggleSetting(
                icon = Icons.Default.Notifications,
                label = "Notifications",
                checked = uiState.notificationsEnabled,
                onToggle = viewModel::toggleNotifications
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AiProviderSection(
            selectedProvider = uiState.selectedProvider,
            health = uiState.aiHealth,
            isLoading = uiState.aiHealthLoading,
            error = uiState.aiHealthError,
            onSwitch = viewModel::switchProvider,
            onRefresh = viewModel::loadAiHealth
        )

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSection(title = "Account") {
            SettingsItem(icon = Icons.Default.Lock, label = "Change Password", onClick = {})
            Divider(color = NavyBorder, modifier = Modifier.padding(horizontal = 16.dp))
            SettingsItem(
                icon = Icons.Default.Logout,
                label = "Logout",
                labelColor = Rose,
                onClick = { showLogoutDialog = true }
            )
        }

        if (uiState.isAdmin) {
            Spacer(modifier = Modifier.height(12.dp))
            SettingsSection(title = "Administration") {
                SettingsItem(
                    icon = Icons.Default.Dashboard,
                    label = "Admin Dashboard",
                    onClick = onAdminDashboard
                )
                Divider(color = NavyBorder, modifier = Modifier.padding(horizontal = 16.dp))
                SettingsItem(
                    icon = Icons.Default.Group,
                    label = "Users",
                    onClick = onAdminUsers
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = NavyCard,
            title = { Text("Logout", color = TextPrimary) },
            text = { Text("Are you sure you want to logout?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; viewModel.logout() }) {
                    Text("Logout", color = Rose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(content = content)
    }
}

@Composable
private fun ToggleSetting(icon: ImageVector, label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = ElectricBlue)
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    label: String,
    labelColor: androidx.compose.ui.graphics.Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (labelColor == Rose) Rose else TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = labelColor, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AiProviderSection(
    selectedProvider: String,
    health: com.pfa.interview.data.model.HealthStatus?,
    isLoading: Boolean,
    error: String?,
    onSwitch: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Text(
        "Intelligence Artificielle",
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Provider actif", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ollama" to "Ollama (local)", "claude" to "Claude API").forEach { (key, label) ->
                    val selected = selectedProvider == key
                    OutlinedButton(
                        onClick = { if (!selected) onSwitch(key) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) ElectricBlue.copy(alpha = 0.15f) else Color.Transparent,
                            contentColor = if (selected) ElectricBlue else TextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) ElectricBlue else NavyBorder
                        )
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider(color = NavyBorder)

            when {
                isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ElectricBlue, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Vérification...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                error != null -> Text(
                    "Indisponible : $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = Rose
                )
                health != null -> {
                    val isOk = health.status == "ok"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOk) Emerald else Rose)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            buildString {
                                append(if (isOk) "En ligne" else "Hors ligne")
                                append(" • ${health.model}")
                                append(" • ${health.latencyMs} ms")
                                if (health.url != null) append("\n${health.url}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOk) TextPrimary else Rose
                        )
                    }
                    if (!isOk && health.message.isNotEmpty()) {
                        Text(health.message, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            }

            TextButton(
                onClick = onRefresh,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp), tint = ElectricBlue)
                Spacer(Modifier.width(4.dp))
                Text("Rafraîchir", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E1A)
@Composable
private fun ProfileScreenPreview() {
    InterviewSimulatorTheme {
        ProfileScreen(onLoggedOut = {})
    }
}
