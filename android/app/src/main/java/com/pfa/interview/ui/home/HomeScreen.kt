package com.pfa.interview.ui.home

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfa.interview.core.ui.components.ShimmerBox
import com.pfa.interview.data.model.InterviewSession
import com.pfa.interview.ui.theme.*
import java.util.*

@Composable
fun HomeScreen(
    onStartInterview: () -> Unit,
    onSessionClick: (String, Boolean) -> Unit,
    onSeeAllHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "$greeting, ${uiState.userName.split(" ").firstOrNull() ?: uiState.userName} 👋",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Ready to practice today?", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Notifications, null, tint = TextSecondary)
                }
            }
        }

        item {
            if (uiState.isLoading) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) { ShimmerBox(modifier = Modifier.weight(1f).height(80.dp), cornerRadius = 12.dp) }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(modifier = Modifier.weight(1f), value = "${uiState.totalSessions}", label = "Sessions")
                    StatCard(modifier = Modifier.weight(1f), value = String.format("%.1f", uiState.averageScore), label = "Avg Score")
                    StatCard(modifier = Modifier.weight(1f), value = "🔥 ${uiState.streakDays}", label = "Streak")
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clickable { onStartInterview() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(ElectricBlue, BlueLight)))
                        .padding(20.dp)
                ) {
                    Column {
                        Text("Start a New Interview", style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Technical", "HR", "Domain").forEach {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(TextPrimary.copy(alpha = 0.2f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(it, color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                    Icon(
                        Icons.Default.ArrowForward,
                        null,
                        tint = TextPrimary,
                        modifier = Modifier.align(Alignment.CenterEnd).size(32.dp)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Sessions", style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onSeeAllHistory) {
                    Text("See all", color = BlueLight)
                }
            }
        }

        if (uiState.isLoading) {
            items(3) { ShimmerBox(modifier = Modifier.fillMaxWidth().height(80.dp)) }
        } else if (uiState.recentSessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Assessment, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No sessions yet", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onStartInterview) {
                            Text("Start your first interview", color = ElectricBlue)
                        }
                    }
                }
            }
        } else {
            items(uiState.recentSessions) { session ->
                SessionCard(
                    session = session,
                    onClick = { onSessionClick(session.id, session.status == "COMPLETED") }
                )
            }
        }

        if (uiState.dailyTip.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Lightbulb, null, tint = Amber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Daily Tip", style = MaterialTheme.typography.labelLarge, color = Amber)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(uiState.dailyTip, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun SessionCard(session: InterviewSession, onClick: () -> Unit) {
    val scoreColor = when {
        session.overallScore >= 7f -> Emerald
        session.overallScore >= 5f -> Amber
        else -> Rose
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.position.ifBlank { "Interview" }, style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeBadge(session.type)
                    Text(session.startedAt.take(10), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
            if (session.overallScore > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(scoreColor.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        String.format("%.1f", session.overallScore),
                        color = scoreColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TypeBadge(type: String) {
    val color = when (type.uppercase()) {
        "TECHNICAL" -> ElectricBlue
        "HR" -> Emerald
        "DOMAIN" -> Amber
        else -> TextSecondary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(type.lowercase().replaceFirstChar { it.uppercase() },
            color = color, style = MaterialTheme.typography.labelSmall)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E1A)
@Composable
private fun HomeScreenPreview() {
    InterviewSimulatorTheme {
        HomeScreen(onStartInterview = {}, onSessionClick = { _, _ -> }, onSeeAllHistory = {})
    }
}
