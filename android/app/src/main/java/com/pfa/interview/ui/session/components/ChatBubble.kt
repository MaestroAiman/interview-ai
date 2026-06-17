package com.pfa.interview.ui.session.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pfa.interview.ui.theme.*

@Composable
fun ChatBubble(
    message: String,
    isAI: Boolean,
    timestamp: String,
    isTyping: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isAI) Arrangement.Start else Arrangement.End
    ) {
        if (isAI) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .background(NavyCard)
                    .drawBehind {
                        drawLine(
                            color = ElectricBlue,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 4.dp.toPx()
                        )
                    }
                    .padding(12.dp)
            ) {
                if (isTyping) {
                    TypingIndicator()
                } else {
                    Column {
                        Text(message, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text(timestamp, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                    .background(Brush.linearGradient(listOf(ElectricBlue, BlueLight)))
                    .padding(12.dp)
            ) {
                Column {
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text(
                        timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(4.dp)
    ) {
        repeat(3) { index ->
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = index * 120),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = offsetY.dp)
                    .background(TextSecondary, androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E1A)
@Composable
private fun ChatBubblePreview() {
    InterviewSimulatorTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ChatBubble("Tell me about yourself and your background.", isAI = true, timestamp = "10:01")
            ChatBubble("I'm a software engineer with 3 years of experience...", isAI = false, timestamp = "10:02")
            ChatBubble("", isAI = true, timestamp = "", isTyping = true)
        }
    }
}
