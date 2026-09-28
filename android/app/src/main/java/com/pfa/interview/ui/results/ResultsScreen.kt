package com.pfa.interview.ui.results

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfa.interview.core.ui.components.ShimmerBox
import com.pfa.interview.data.model.SessionSummary
import com.pfa.interview.data.remote.dto.QuestionReview
import com.pfa.interview.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ResultsScreen(
    sessionId: String,
    onPracticeAgain: () -> Unit,
    onGoHome: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) { viewModel.loadResults(sessionId) }

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
                Text("Results", style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary, fontWeight = FontWeight.Bold)
                IconButton(onClick = onGoHome) {
                    Icon(Icons.Default.Home, null, tint = TextSecondary)
                }
            }
        }

        if (uiState.isLoading) {
            item { ShimmerBox(modifier = Modifier.fillMaxWidth().height(200.dp)) }
        } else if (uiState.results != null) {
            val results = uiState.results!!
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedScore(score = results.overallScore)
                    Spacer(modifier = Modifier.height(8.dp))
                    val label = when {
                        results.overallScore >= 8.5f -> "Excellent!"
                        results.overallScore >= 7f -> "Good job!"
                        results.overallScore >= 5f -> "Keep going!"
                        else -> "Keep practicing"
                    }
                    Text(label, style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${results.position} · ${results.type.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyCard)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center) {
                        RadarChart(
                            relevance = results.relevanceAvg,
                            clarity = results.clarityAvg,
                            sentiment = results.sentimentAvg,
                            modifier = Modifier.size(200.dp)
                        )
                    }
                }
            }

            item {
                Text("Question Review", style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }

            items(results.questionReviews) { review ->
                QuestionReviewCard(review = review)
            }

            uiState.sessionSummary?.let { summary ->
                item {
                    SessionSummarySection(summary = summary)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onGoHome,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonColors(NavyBorder, TextPrimary, NavyBorder, TextMuted)
                    ) {
                        Text("Home")
                    }
                    Button(
                        onClick = onPracticeAgain,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("Practice Again", color = TextPrimary)
                    }
                }
            }
        } else if (uiState.error != null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Rose, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(uiState.error!!, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { viewModel.loadResults(sessionId) }) {
                        Text("Retry", color = ElectricBlue)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSummarySection(summary: SessionSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Session Summary",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        if (summary.globalAssessment.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCard)
            ) {
                Text(
                    summary.globalAssessment,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        val (readinessColor, readinessLabel) = when (summary.readinessLevel.lowercase()) {
            "ready" -> Emerald to "Ready"
            "not_ready" -> Rose to "Not Ready"
            else -> Amber to "Almost Ready"
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = readinessColor.copy(alpha = 0.15f)
        ) {
            Text(
                readinessLabel,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = readinessColor,
                fontWeight = FontWeight.Bold
            )
        }

        if (summary.topStrengths.isNotEmpty()) {
            SummaryListCard(
                title = "Top Strengths",
                titleColor = Emerald,
                containerColor = Emerald.copy(alpha = 0.08f),
                items = summary.topStrengths
            )
        }

        if (summary.priorityImprovements.isNotEmpty()) {
            SummaryListCard(
                title = "Priority Improvements",
                titleColor = Amber,
                containerColor = Amber.copy(alpha = 0.08f),
                items = summary.priorityImprovements
            )
        }

        if (summary.recommendedResources.isNotEmpty()) {
            SummaryListCard(
                title = "Recommended Resources",
                titleColor = ElectricBlue,
                containerColor = ElectricBlue.copy(alpha = 0.08f),
                items = summary.recommendedResources
            )
        }
    }
}

@Composable
private fun SummaryListCard(
    title: String,
    titleColor: Color,
    containerColor: Color,
    items: List<String>
) {
    var expanded by remember { mutableStateOf(true) }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = titleColor, fontWeight = FontWeight.SemiBold)
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = titleColor, modifier = Modifier.size(18.dp)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                items.forEach { item ->
                    Text("• $item", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun AnimatedScore(score: Float) {
    var animStarted by remember { mutableStateOf(false) }
    val animated by animateFloatAsState(
        targetValue = if (animStarted) score else 0f,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "score_count_up"
    )
    LaunchedEffect(Unit) { animStarted = true }

    val scoreColor = when {
        score >= 7f -> Emerald
        score >= 5f -> Amber
        else -> Rose
    }
    Text(
        String.format("%.1f", animated),
        style = MaterialTheme.typography.displayLarge,
        color = scoreColor,
        fontWeight = FontWeight.ExtraBold
    )
    Text("/ 10.0", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
}

@Composable
private fun RadarChart(
    relevance: Float,
    clarity: Float,
    sentiment: Float,
    modifier: Modifier = Modifier
) {
    var animStarted by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animStarted) 1f else 0f,
        animationSpec = tween(1000),
        label = "radar"
    )
    LaunchedEffect(Unit) { animStarted = true }

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = minOf(cx, cy) * 0.8f
        val axes = 3
        val values = listOf(relevance / 10f, clarity / 10f, sentiment / 10f)
        val labels = listOf("Relevance", "Clarity", "Sentiment")

        for (level in 1..5) {
            val r = maxR * level / 5f
            val points = (0 until axes).map { i ->
                val angle = Math.PI * 2 * i / axes - Math.PI / 2
                Offset(cx + r * cos(angle).toFloat(), cy + r * sin(angle).toFloat())
            }
            for (i in points.indices) {
                drawLine(NavyBorder, points[i], points[(i + 1) % axes], strokeWidth = 1.dp.toPx())
            }
        }

        for (i in 0 until axes) {
            val angle = Math.PI * 2 * i / axes - Math.PI / 2
            drawLine(NavyBorder,
                Offset(cx, cy),
                Offset(cx + maxR * cos(angle).toFloat(), cy + maxR * sin(angle).toFloat()),
                strokeWidth = 1.dp.toPx())
        }

        val dataPath = Path()
        values.forEachIndexed { i, v ->
            val angle = Math.PI * 2 * i / axes - Math.PI / 2
            val r = maxR * v * progress
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        drawPath(dataPath, ElectricBlue.copy(alpha = 0.3f))
        drawPath(dataPath, ElectricBlue, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun QuestionReviewCard(review: QuestionReview) {
    var expanded by remember { mutableStateOf(false) }
    val scoreColor = when {
        review.score >= 7f -> Emerald
        review.score >= 5f -> Amber
        else -> Rose
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()) {
                Text(
                    review.question,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(String.format("%.1f", review.score), color = scoreColor,
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, tint = TextSecondary, modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Your Answer", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text(review.answer, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Feedback", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text(review.feedback, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

@Preview(showBackground = true, backgroundColor = 0xFF0A0E1A)
@Composable
private fun ResultsScreenPreview() {
    InterviewSimulatorTheme {
        Box(modifier = Modifier.fillMaxSize().background(DeepNavy), contentAlignment = Alignment.Center) {
            AnimatedScore(score = 7.5f)
        }
    }
}
