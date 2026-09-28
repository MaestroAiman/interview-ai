package com.pfa.interview.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfa.interview.ui.session.components.*
import com.pfa.interview.ui.theme.*

@Composable
fun SessionScreen(
    sessionId: String,
    onSessionComplete: (String) -> Unit,
    onQuit: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(sessionId) {
        viewModel.loadForResume(sessionId)
    }

    LaunchedEffect(uiState.isSessionComplete, uiState.showFeedback) {
        if (uiState.isSessionComplete && !uiState.showFeedback) {
            onSessionComplete(sessionId)
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(DeepNavy, NavyCard, DeepNavy))
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Question ${uiState.currentQuestionIndex + 1} of ${uiState.totalQuestions}",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (uiState.totalQuestions > 0)
                                (uiState.currentQuestionIndex).toFloat() / uiState.totalQuestions
                            else 0f
                        },
                        modifier = Modifier.width(180.dp).height(4.dp),
                        color = ElectricBlue,
                        trackColor = NavyBorder
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.currentQuestion?.category?.let { cat ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = ElectricBlue.copy(alpha = 0.15f),
                                    labelColor = ElectricBlue
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = ElectricBlue.copy(alpha = 0.3f)
                                )
                            )
                        }
                        uiState.currentQuestion?.aiDifficulty?.let { diff ->
                            val diffColor = when (diff.lowercase()) {
                                "beginner" -> Emerald
                                "intermediate" -> Amber
                                else -> Rose
                            }
                            SuggestionChip(
                                onClick = {},
                                label = { Text(diff.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = diffColor.copy(alpha = 0.15f),
                                    labelColor = diffColor
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = diffColor.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimerCircle(totalSeconds = 60, remainingSeconds = uiState.timerSeconds)
                    IconButton(onClick = onQuit) {
                        Icon(Icons.Default.Close, "Quit", tint = TextSecondary)
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.messages) { message ->
                    ChatBubble(
                        message = message.text,
                        isAI = message.isAI,
                        timestamp = message.timestamp,
                        isTyping = message.isTyping
                    )
                }
            }

            if (uiState.isTimeout) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "L'IA met du temps à répondre…",
                        color = Amber,
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = viewModel::retryAnswer) {
                        Text("Réessayer", color = ElectricBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            uiState.error?.let { error ->
                Text(error, color = Rose, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = uiState.currentAnswer,
                        onValueChange = viewModel::onAnswerChange,
                        placeholder = { Text("Type or speak your answer...", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = NavyBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = ElectricBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VoiceRecorderButton(
                            isRecording = uiState.isRecording,
                            onStartRecording = { viewModel.startRecording(context) },
                            onStopRecording = viewModel::stopRecording
                        )
                        IconButton(
                            onClick = viewModel::submitAnswer,
                            enabled = uiState.currentAnswer.isNotBlank() && !uiState.isAITyping,
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    if (uiState.currentAnswer.isNotBlank()) ElectricBlue else NavyBorder,
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(Icons.Default.Send, "Send answer", tint = TextPrimary)
                        }
                    }
                }
            }
        }

        if (uiState.showFeedback && uiState.currentFeedback != null) {
            FeedbackBottomSheet(
                feedback = uiState.currentFeedback!!,
                onNext = {
                    if (uiState.isSessionComplete) {
                        onSessionComplete(sessionId)
                    } else {
                        viewModel.dismissFeedback()
                    }
                },
                onDismiss = viewModel::dismissFeedback
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E1A)
@Composable
private fun SessionScreenPreview() {
    InterviewSimulatorTheme {
        Box(modifier = Modifier.fillMaxSize().background(DeepNavy)) {
            Text("Session Screen", color = TextPrimary,
                modifier = Modifier.align(Alignment.Center))
        }
    }
}
