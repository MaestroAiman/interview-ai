package com.pfa.interview.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfa.interview.data.remote.dto.AdminDashboardDto
import com.pfa.interview.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    onReviewSession: (String) -> Unit = {},
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (uiState.pendingDeleteSessionId != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteSession,
            containerColor = NavyCard,
            title = { Text("Delete Session", color = TextPrimary) },
            text = {
                Text(
                    "This will permanently remove the session and all its data.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteSession) {
                    Text("Delete", color = Rose)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteSession) {
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
                    Text("Admin Dashboard", color = TextPrimary, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadDashboard) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyCard)
            )
        }
    ) { padding ->
        when {
            uiState.isLoading || uiState.isDeleting -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ElectricBlue)
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Total Users",
                            value = uiState.totalUsers.toString(),
                            valueColor = ElectricBlue
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Completed",
                            value = uiState.completedCount.toString(),
                            valueColor = Emerald
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "In Progress",
                            value = uiState.inProgressCount.toString(),
                            valueColor = Amber
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Avg Score",
                            value = "%.1f/10".format(uiState.platformAvgScore),
                            valueColor = TextPrimary
                        )
                    }
                }

                item {
                    Text(
                        "All Sessions (${uiState.sessions.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (uiState.sessions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No sessions found.", color = TextSecondary)
                        }
                    }
                } else {
                    items(uiState.sessions, key = { it.id }) { session ->
                        SessionAdminRow(
                            session = session,
                            onReview = { onReviewSession(session.id) },
                            onDelete = { viewModel.requestDeleteSession(session.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun SessionAdminRow(
    session: AdminDashboardDto.SessionRowDto,
    onReview: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (session.status) {
        "COMPLETED"   -> Emerald
        "IN_PROGRESS" -> Amber
        "ABANDONED"   -> Rose
        else          -> TextMuted
    }
    val statusLabel = when (session.status) {
        "COMPLETED"   -> "Done"
        "IN_PROGRESS" -> "Active"
        "ABANDONED"   -> "Abandoned"
        else          -> session.status ?: "—"
    }
    val shortUserId = if (session.userId.length > 8)
        session.userId.take(8) + "…" else session.userId

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session.position ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "User: $shortUserId • ${session.type ?: "—"} • ${session.difficulty ?: "—"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    session.startedAt?.take(10)?.let { date ->
                        Text(date, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "%.1f/10".format(session.overallScore),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            statusLabel,
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (session.status == "COMPLETED") {
                    IconButton(onClick = onReview, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = "Review",
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
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
