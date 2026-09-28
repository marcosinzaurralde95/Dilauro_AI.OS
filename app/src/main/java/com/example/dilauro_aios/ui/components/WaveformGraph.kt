package com.example.dilauro_aios.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dilauro_aios.ui.theme.NeonCyan
import com.example.dilauro_aios.ui.theme.NeonPurple

@Composable
fun BurstRingBufferGraph(
    bursts: List<Long>,
    modifier: Modifier = Modifier,
    activeColor: Color = NeonCyan,
    baseColor: Color = NeonPurple
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        val count = bursts.size
        if (count == 0) return@Canvas

        val maxVal = (bursts.maxOrNull() ?: 30L).coerceAtLeast(10L).toFloat()
        val spacing = 6.dp.toPx()
        val barWidth = (size.width - (spacing * (count - 1))) / count

        bursts.forEachIndexed { i, burst ->
            val barHeight = ((burst.toFloat() / maxVal) * size.height).coerceIn(4.dp.toPx(), size.height)
            val left = i * (barWidth + spacing)
            val top = size.height - barHeight

            val brush = Brush.verticalGradient(
                colors = listOf(activeColor, baseColor),
                startY = top,
                endY = size.height
            )

            drawRoundRect(
                brush = brush,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}
