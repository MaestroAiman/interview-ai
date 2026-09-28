package com.pfa.interview.ui.session.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pfa.interview.data.model.Feedback
import com.pfa.interview.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackBottomSheet(
    feedback: Feedback,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val overallScore = if (feedback.globalScore > 0f) {
        feedback.globalScore / 10f
    } else {
        (feedback.relevanceScore + feedback.clarityScore + feedback.sentimentScore) / 3f
    }
    val scoreColor = when {
        overallScore >= 7f -> Emerald
        overallScore >= 5f -> Amber
        else -> Rose
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NavyCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Answer Feedback",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            ScoreRing(score = overallScore, color = scoreColor)

            // Level Assessment badge
            feedback.levelAssessment?.let { level ->
                Spacer(modifier = Modifier.height(12.dp))
                val levelColor = when (level.lowercase()) {
                    "senior" -> Emerald
                    "mid" -> Amber
                    else -> Rose
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = levelColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        level.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = levelColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5-score breakdown
            ScoreBreakdownRow("Relevance", feedback.relevanceScore)
            Spacer(modifier = Modifier.height(10.dp))
            ScoreBreakdownRow("Clarity", feedback.clarityScore)
            if (feedback.depthScore > 0f) {
                Spacer(modifier = Modifier.height(10.dp))
                ScoreBreakdownRow("Depth", feedback.depthScore / 10f)
            }
            if (feedback.vocabularyScore > 0f) {
                Spacer(modifier = Modifier.height(10.dp))
                ScoreBreakdownRow("Vocabulary", feedback.vocabularyScore / 10f)
            }
            if (feedback.examplesScore > 0f) {
                Spacer(modifier = Modifier.height(10.dp))
                ScoreBreakdownRow("Examples", feedback.examplesScore / 10f)
            }

            // Key Strengths
            if (feedback.keyStrengths.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                FeedbackSection(
                    title = "Key Strengths",
                    titleColor = Emerald,
                    containerColor = Emerald.copy(alpha = 0.08f)
                ) {
                    feedback.keyStrengths.forEach { strength ->
                        Text(
                            "• $strength",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Critical Gaps
            if (feedback.criticalGaps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FeedbackSection(
                    title = "Critical Gaps",
                    titleColor = Rose,
                    containerColor = Rose.copy(alpha = 0.08f)
                ) {
                    feedback.criticalGaps.forEach { gap ->
                        Text(
                            "• $gap",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Positive Points (or fallback to comments)
            val positiveText = feedback.positivePoints ?: feedback.comments.takeIf { it.isNotEmpty() }
            if (positiveText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                FeedbackSection(
                    title = "Positive Points",
                    titleColor = TextSecondary,
                    containerColor = SurfaceVariant
                ) {
                    Text(positiveText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            // Improvement Points
            feedback.improvementPoints?.let { text ->
                Spacer(modifier = Modifier.height(12.dp))
                FeedbackSection(
                    title = "Improvements",
                    titleColor = Amber,
                    containerColor = Amber.copy(alpha = 0.08f)
                ) {
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            // Concrete Advice
            feedback.concreteAdvice?.let { advice ->
                Spacer(modifier = Modifier.height(12.dp))
                FeedbackSection(
                    title = "Actionable Advice",
                    titleColor = ElectricBlue,
                    containerColor = ElectricBlue.copy(alpha = 0.08f)
                ) {
                    Text(advice, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            // Example Answer (or fallback to improvedAnswer)
            val exampleText = feedback.exampleAnswer ?: feedback.improvedAnswer.takeIf { it.isNotEmpty() }
            if (exampleText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                FeedbackSection(
                    title = "Suggested Answer",
                    titleColor = Emerald,
                    containerColor = Emerald.copy(alpha = 0.1f)
                ) {
                    Text(exampleText, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("Next Question →", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FeedbackSection(
    title: String,
    titleColor: Color,
    containerColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = titleColor,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ScoreRing(score: Float, color: Color) {
    var animStarted by remember { mutableStateOf(false) }
    val animatedSweep by animateFloatAsState(
        targetValue = if (animStarted) (score / 10f) * 360f else 0f,
        animationSpec = tween(1000),
        label = "score_ring"
    )
    LaunchedEffect(Unit) { animStarted = true }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        Canvas(modifier = Modifier.size(120.dp)) {
            drawArc(NavyBorder, -90f, 360f, false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
            drawArc(color, -90f, animatedSweep, false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                String.format("%.1f", score),
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text("/ 10", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun ScoreBreakdownRow(label: String, score: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.width(90.dp)
        )
        LinearProgressIndicator(
            progress = { score / 10f },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = when {
                score >= 7f -> Emerald
                score >= 5f -> Amber
                else -> Rose
            },
            trackColor = NavyBorder
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            String.format("%.1f", score),
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp)
        )
    }
}

@Preview
@Composable
private fun FeedbackBottomSheetPreview() {
    InterviewSimulatorTheme {
        ScoreRing(score = 7.5f, color = Emerald)
    }
}
