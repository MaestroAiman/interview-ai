package com.pfa.interview.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfa.interview.core.ui.components.ShimmerBox
import com.pfa.interview.data.model.InterviewSession
import com.pfa.interview.ui.home.TypeBadge
import com.pfa.interview.ui.setup.InterviewType
import com.pfa.interview.ui.theme.*

@Composable
fun HistoryScreen(
    onReviewSession: (String) -> Unit,
    onResumeSession: (String) -> Unit,
    onStartInterview: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        Text(
            "My Sessions",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            item {
                FilterChipItem(
                    label = "All",
                    selected = uiState.selectedFilter == null,
                    onClick = { viewModel.filterByType(null) }
                )
            }
            items(InterviewType.values()) { type ->
                FilterChipItem(
                    label = type.display,
                    selected = uiState.selectedFilter == type,
                    onClick = { viewModel.filterByType(type) }
                )
            }
        }

        if (uiState.isLoading) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) { ShimmerBox(modifier = Modifier.fillMaxWidth().height(80.dp)) }
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Rose, modifier = Modifier.size(48.dp))
                    Text(
                        uiState.error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Button(
                        onClick = viewModel::refresh,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Réessayer")
                    }
                }
            }
        } else if (uiState.filteredSessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Assessment, null, tint = TextMuted, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No sessions yet", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Text("Practice to see your history here", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onStartInterview,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Start your first interview", color = TextPrimary)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.filteredSessions) { session ->
                    val isCompleted = session.status == "COMPLETED"
                    HistorySessionCard(
                        session = session,
                        isCompleted = isCompleted,
                        onReview = { onReviewSession(session.id) },
                        onResume = { onResumeSession(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ElectricBlue.copy(alpha = 0.2f),
            selectedLabelColor = ElectricBlue,
            labelColor = TextSecondary
        )
    )
}

@Composable
private fun HistorySessionCard(
    session: InterviewSession,
    isCompleted: Boolean,
    onReview: () -> Unit,
    onResume: () -> Unit
) {
    val scoreColor = when {
        session.overallScore >= 7f -> Emerald
        session.overallScore >= 5f -> Amber
        else -> Rose
    }
    val statusColor = if (isCompleted) Emerald else Amber
    val statusLabel = if (isCompleted) "Terminé" else "En cours"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = if (isCompleted) onReview else onResume)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session.position.ifBlank { "Interview" },
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TypeBadge(session.type)
                        Text(
                            session.startedAt.take(10),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            "${session.questionCount}Q",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
                if (isCompleted && session.overallScore > 0) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(scoreColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            String.format("%.1f", session.overallScore),
                            color = scoreColor,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        statusLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                Button(
                    onClick = if (isCompleted) onReview else onResume,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) ElectricBlue else Amber
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        if (isCompleted) Icons.Default.Assessment else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (isCompleted) "Review" else "Resume",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E1A)
@Composable
private fun HistoryScreenPreview() {
    InterviewSimulatorTheme {
        HistoryScreen(onReviewSession = {}, onResumeSession = {}, onStartInterview = {})
    }
}
