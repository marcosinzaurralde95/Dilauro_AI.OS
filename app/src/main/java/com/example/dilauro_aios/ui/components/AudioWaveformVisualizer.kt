package com.example.dilauro_aios.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dilauro_aios.ui.theme.NeonCyan
import com.example.dilauro_aios.ui.theme.NeonPurple
import kotlin.math.sin

@Composable
fun AudioWaveformVisualizer(
    isPlaying: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = NeonCyan,
    seed: Long = 42L
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) 6.28f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_anim"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        val barCount = 36
        val barWidth = (size.width / (barCount * 1.4f)).coerceAtLeast(3f)
        val spacing = (size.width - barCount * barWidth) / (barCount - 1).coerceAtLeast(1)
        val midY = size.height / 2f

        for (i in 0 until barCount) {
            val normalizedIdx = i.toFloat() / barCount
            val x = i * (barWidth + spacing)

            // Calculate height using combination of sine waves and pseudo-random seed
            val localPhase = phase + i * 0.35f
            val baseAmp = if (isPlaying) {
                (sin(localPhase) * 0.4f + sin(localPhase * 2.1f) * 0.3f + 0.3f).coerceIn(0.15f, 0.95f)
            } else {
                0.2f + 0.1f * sin(i * 0.5f)
            }

            val barHeight = size.height * baseAmp * 0.85f
            val isPlayed = normalizedIdx <= progress

            val barBrush = if (isPlayed) {
                Brush.verticalGradient(
                    colors = listOf(activeColor, NeonPurple),
                    startY = midY - barHeight / 2f,
                    endY = midY + barHeight / 2f
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(Color.DarkGray.copy(alpha = 0.6f), Color.Gray.copy(alpha = 0.3f)),
                    startY = midY - barHeight / 2f,
                    endY = midY + barHeight / 2f
                )
            }

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, midY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        // Draw scrub head indicator line
        if (progress > 0f) {
            val playheadX = size.width * progress.coerceIn(0f, 1f)
            drawLine(
                color = Color.White,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, size.height),
                strokeWidth = 3f
            )
            drawCircle(
                color = activeColor,
                radius = 5f,
                center = Offset(playheadX, midY)
            )
        }
    }
}
