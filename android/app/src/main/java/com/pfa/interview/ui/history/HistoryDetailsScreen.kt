package com.pfa.interview.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfa.interview.core.ui.components.ShimmerBox
import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.model.Answer
import com.pfa.interview.data.model.Feedback
import com.pfa.interview.data.model.HistoryDetails
import com.pfa.interview.data.model.InterviewSession
import com.pfa.interview.data.model.Question
import com.pfa.interview.ui.theme.*

@Composable
fun HistoryDetailsScreen(
    onBack: () -> Unit,
    viewModel: HistoryDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
            }
            Text(
                "Détails de la session",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        when (val s = state) {
            is Resource.Loading -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(4) { ShimmerBox(modifier = Modifier.fillMaxWidth().height(120.dp)) }
                }
            }
            is Resource.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Rose, modifier = Modifier.size(48.dp))
                        Text(s.message ?: "Erreur de chargement", color = TextSecondary)
                        Button(onClick = viewModel::loadDetails,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)) {
                            Text("Réessayer")
                        }
                    }
                }
            }
            is Resource.Success -> {
                HistoryDetailsContent(details = s.data)
            }
        }
    }
}

@Composable
private fun HistoryDetailsContent(details: HistoryDetails) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SessionHeader(session = details.session) }
        itemsIndexed(details.questions) { index, question ->
            val answer = details.answers.find { it.questionId == question.id }
            val feedback = details.feedbacks.find { it.questionId == question.id }
            QuestionDetailCard(
                index = index + 1,
                question = question,
                answer = answer,
                feedback = feedback
            )
        }
    }
}

@Composable
private fun SessionHeader(session: InterviewSession) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session.position.ifBlank { "Session d'entretien" },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${session.type} • ${session.difficulty} • ${session.questionCount} questions",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        session.startedAt.take(10),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
                if (session.overallScore > 0) {
                    val color = when {
                        session.overallScore >= 7f -> Emerald
                        session.overallScore >= 5f -> Amber
                        else -> Rose
                    }
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                String.format("%.1f", session.overallScore),
                                color = color,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text("/10", color = color.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (!session.readinessLevel.isNullOrBlank()) {
                val (readinessColor, readinessLabel) = when (session.readinessLevel) {
                    "ready" -> Emerald to "Prêt"
                    "almost_ready" -> Amber to "Presque prêt"
                    else -> Rose to "En progression"
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(readinessColor))
                    Text(readinessLabel, style = MaterialTheme.typography.labelMedium, color = readinessColor)
                }
            }
        }
    }
}

@Composable
private fun QuestionDetailCard(
    index: Int,
    question: Question,
    answer: Answer?,
    feedback: Feedback?
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$index",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!question.category.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ElectricBlue.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    question.category,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricBlue
                                )
                            }
                        }
                        if (!question.aiDifficulty.isNullOrBlank()) {
                            val diffColor = when (question.aiDifficulty.lowercase()) {
                                "advanced" -> Rose
                                "intermediate" -> Amber
                                else -> Emerald
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = diffColor.copy(alpha = 0.15f)) {
                                Text(
                                    question.aiDifficulty,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = diffColor
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(question.content, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = TextMuted, modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = NavyBorder)

                    if (answer != null && answer.answerText.isNotBlank()) {
                        Column {
                            Text("Ta réponse", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceVariant
                            ) {
                                Text(
                                    answer.answerText,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    if (feedback != null) {
                        ScoresRow(feedback)

                        if (feedback.levelAssessment != null) {
                            val lvlColor = when (feedback.levelAssessment.lowercase()) {
                                "senior" -> Emerald
                                "mid" -> Amber
                                else -> Rose
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Niveau :", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Surface(shape = RoundedCornerShape(4.dp), color = lvlColor.copy(alpha = 0.15f)) {
                                    Text(
                                        feedback.levelAssessment,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = lvlColor
                                    )
                                }
                            }
                        }

                        if (!feedback.positivePoints.isNullOrBlank()) {
                            FeedbackSection(icon = Icons.Default.ThumbUp, title = "Points positifs",
                                content = feedback.positivePoints, color = Emerald)
                        }
                        if (!feedback.concreteAdvice.isNullOrBlank()) {
                            FeedbackSection(icon = Icons.Default.Lightbulb, title = "Conseils concrets",
                                content = feedback.concreteAdvice, color = Amber)
                        }
                        if (!feedback.exampleAnswer.isNullOrBlank()) {
                            FeedbackSection(icon = Icons.Default.School, title = "Exemple de réponse",
                                content = feedback.exampleAnswer, color = BlueLight)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoresRow(feedback: Feedback) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Scores", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        val scores = listOf(
            "Pertinence" to feedback.relevanceScore,
            "Clarté" to feedback.clarityScore,
            "Profondeur" to feedback.depthScore,
            "Vocabulaire" to feedback.vocabularyScore,
            "Exemples" to feedback.examplesScore
        ).filter { it.second > 0 }

        scores.forEach { (label, score) ->
            val normalizedScore = if (score > 10) score / 10f else score
            val color = when {
                normalizedScore >= 7f -> Emerald
                normalizedScore >= 5f -> Amber
                else -> Rose
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.width(80.dp)
                )
                LinearProgressIndicator(
                    progress = { (normalizedScore / 10f).coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = color,
                    trackColor = NavyBorder
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    String.format("%.1f", normalizedScore),
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    modifier = Modifier.width(28.dp)
                )
            }
        }
    }
}

@Composable
private fun FeedbackSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = color)
        }
        Text(content, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}
