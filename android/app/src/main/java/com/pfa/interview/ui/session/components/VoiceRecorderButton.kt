package com.pfa.interview.ui.session.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pfa.interview.ui.theme.*
import kotlin.random.Random

@Composable
fun VoiceRecorderButton(
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(contentAlignment = Alignment.Center) {
        if (!isRecording) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(ElectricBlue.copy(alpha = 0.2f))
                    .border(1.dp, ElectricBlue.copy(alpha = 0.4f), CircleShape)
            )
        }

        if (isRecording) {
            WaveformBars()
        } else {
            IconButton(
                onClick = onStartRecording,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(ElectricBlue, BlueLight)))
            ) {
                Icon(Icons.Default.Mic, "Start recording", tint = TextPrimary,
                    modifier = Modifier.size(28.dp))
            }
        }

        if (isRecording) {
            IconButton(
                onClick = onStopRecording,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Rose)
            ) {
                Icon(Icons.Default.Stop, "Stop recording", tint = TextPrimary,
                    modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun WaveformBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    Row(
        modifier = Modifier
            .width(80.dp)
            .height(40.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = 8f,
                targetValue = Random.nextFloat() * 28f + 8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(200 + index * 70, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    .background(ElectricBlue)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E1A)
@Composable
private fun VoiceRecorderButtonPreview() {
    InterviewSimulatorTheme {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            VoiceRecorderButton(isRecording = false, onStartRecording = {}, onStopRecording = {})
            VoiceRecorderButton(isRecording = true, onStartRecording = {}, onStopRecording = {})
        }
    }
}
