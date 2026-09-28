package com.pfa.interview.ui.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfa.interview.core.ui.components.ShimmerBox
import com.pfa.interview.ui.setup.InterviewType
import com.pfa.interview.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Progress", style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary, fontWeight = FontWeight.Bold)
        }

        if (uiState.isLoading) {
            item { ShimmerBox(modifier = Modifier.fillMaxWidth().height(200.dp)) }
            item { ShimmerBox(modifier = Modifier.fillMaxWidth().height(150.dp)) }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(modifier = Modifier.weight(1f), value = "${uiState.totalSessions}", label = "Sessions")
                    StatCard(modifier = Modifier.weight(1f),
                        value = String.format("%.1f", uiState.bestScore), label = "Best Score")
                }
            }

            if (uiState.scoreTrend.isNotEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NavyCard)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Score Trend", style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(16.dp))
                            ScoreTrendChart(data = uiState.scoreTrend, modifier = Modifier.fillMaxWidth().height(160.dp))
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyCard)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Score by Type", style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))
                        uiState.typeAverages.forEach { (type, avg) ->
                            TypeScoreRow(type = type, average = avg)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }

            if (uiState.practiceCalendar.isNotEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NavyCard)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Activity Calendar", style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(16.dp))
                            PracticeCalendarHeatmap(calendar = uiState.practiceCalendar)
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyCard)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Achievements", style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))
                        val badges = listOf(
                            Triple("First Session", Icons.Default.Star, uiState.totalSessions >= 1),
                            Triple("5 Sessions", Icons.Default.EmojiEvents, uiState.totalSessions >= 5),
                            Triple("10 Sessions", Icons.Default.EmojiEvents, uiState.totalSessions >= 10),
                            Triple("Score 8+", Icons.Default.TrendingUp, uiState.bestScore >= 8f),
                            Triple("Score 9+", Icons.Default.AutoAwesome, uiState.bestScore >= 9f),
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.height(160.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            userScrollEnabled = false
                        ) {
                            items(badges) { (label, icon, unlocked) ->
                                BadgeItem(label = label, icon = icon, unlocked = unlocked)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun ScoreTrendChart(data: List<Pair<String, Float>>, modifier: Modifier) {
    var animStarted by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(targetValue = if (animStarted) 1f else 0f,
        animationSpec = tween(1200), label = "trend")
    LaunchedEffect(Unit) { animStarted = true }

    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val maxScore = 10f
        val stepX = size.width / (data.size - 1)
        val points = data.mapIndexed { i, (_, score) ->
            Offset(i * stepX, size.height - (score / maxScore) * size.height)
        }
        val pointsAnimated = points.map { pt ->
            Offset(pt.x, size.height - (size.height - pt.y) * progress)
        }

        val fillPath = Path()
        fillPath.moveTo(0f, size.height)
        pointsAnimated.forEach { fillPath.lineTo(it.x, it.y) }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()
        drawPath(fillPath, Brush.linearGradient(
            listOf(ElectricBlue.copy(alpha = 0.4f), ElectricBlue.copy(alpha = 0f)),
            start = Offset(0f, 0f), end = Offset(0f, size.height)
        ))

        for (i in 0 until pointsAnimated.size - 1) {
            drawLine(ElectricBlue, pointsAnimated[i], pointsAnimated[i + 1],
                strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }
        pointsAnimated.forEach {
            drawCircle(ElectricBlue, 4.dp.toPx(), it)
        }
    }
}

@Composable
private fun TypeScoreRow(type: InterviewType, average: Float) {
    val color = when (type) {
        InterviewType.TECHNICAL -> ElectricBlue
        InterviewType.HR -> Emerald
        InterviewType.DOMAIN -> Amber
    }
    var animStarted by remember { mutableStateOf(false) }
    val animated by animateFloatAsState(
        targetValue = if (animStarted) average / 10f else 0f,
        animationSpec = tween(800), label = "bar_$type")
    LaunchedEffect(Unit) { animStarted = true }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(type.display, style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
            modifier = Modifier.width(80.dp))
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color, trackColor = NavyBorder
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(String.format("%.1f", average), style = MaterialTheme.typography.labelLarge,
            color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
    }
}

@Composable
private fun PracticeCalendarHeatmap(calendar: Map<String, Int>) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = Date()
    val days = (29 downTo 0).map { offset ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -offset)
        val dateStr = sdf.format(cal.time)
        Pair(dateStr, calendar[dateStr] ?: 0)
    }
    val max = days.maxOfOrNull { it.second } ?: 1
    LazyVerticalGrid(
        columns = GridCells.Fixed(10),
        modifier = Modifier.height(80.dp),
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        items(days) { (_, count) ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        ElectricBlue.copy(alpha = if (max > 0) (count.toFloat() / max).coerceIn(0.1f, 1f) else 0.1f)
                    )
            )
        }
    }
}

@Composable
private fun BadgeItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, unlocked: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (unlocked) ElectricBlue.copy(alpha = 0.1f) else SurfaceVariant)
            .padding(8.dp)
    ) {
        Icon(icon, null,
            tint = if (unlocked) Amber else TextMuted,
            modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = if (unlocked) TextPrimary else TextMuted,
            textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E1A)
@Composable
private fun ProgressScreenPreview() {
    InterviewSimulatorTheme {
        ProgressScreen()
    }
}
