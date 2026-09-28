package com.pfa.interview.ui.session.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pfa.interview.ui.theme.*

@Composable
fun TimerCircle(
    totalSeconds: Int,
    remainingSeconds: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f
    val color = when {
        progress > 0.5f -> Emerald
        progress > 0.25f -> Amber
        else -> Rose
    }

    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(48.dp)) {
            drawArc(
                color = NavyBorder,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$remainingSeconds",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E1A)
@Composable
private fun TimerCirclePreview() {
    InterviewSimulatorTheme {
        TimerCircle(totalSeconds = 60, remainingSeconds = 30)
    }
}
