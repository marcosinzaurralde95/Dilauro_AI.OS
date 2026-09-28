package com.example.dilauro_aios.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.dilauro_aios.model.ImageAspectRatio
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates futuristic cyberpunk and neural visualization Bitmaps for offline simulation,
 * preview modes, and initial seed neural assets.
 */
object ProceduralImageGenerator {

    fun generateCyberNeuralImage(
        prompt: String,
        aspectRatio: ImageAspectRatio,
        isEdit: Boolean = false,
        sourceBitmap: Bitmap? = null
    ): Bitmap {
        val baseWidth = 720
        val baseHeight = (baseWidth / aspectRatio.widthRatio * aspectRatio.heightRatio).toInt().coerceIn(360, 1280)
        val bitmap = Bitmap.createBitmap(baseWidth, baseHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val rand = Random(prompt.hashCode().toLong())

        // 1. Dark Cyber Background with Radial Core Glow
        val bgPaint = Paint().apply {
            color = Color.rgb(10, 14, 23)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, baseWidth.toFloat(), baseHeight.toFloat(), bgPaint)

        // Radial glow from center
        val centerX = baseWidth / 2f
        val centerY = baseHeight / 2f
        val glowRadius = baseWidth * 0.6f

        val glowColor = when {
            prompt.contains("crimson", ignoreCase = true) || prompt.contains("security", ignoreCase = true) -> Color.rgb(255, 51, 85)
            prompt.contains("purple", ignoreCase = true) || prompt.contains("violet", ignoreCase = true) -> Color.rgb(180, 77, 255)
            prompt.contains("emerald", ignoreCase = true) || prompt.contains("green", ignoreCase = true) -> Color.rgb(0, 230, 153)
            prompt.contains("gold", ignoreCase = true) || prompt.contains("amber", ignoreCase = true) -> Color.rgb(255, 179, 0)
            else -> Color.rgb(0, 240, 255)
        }

        val glowPaint = Paint().apply {
            shader = RadialGradient(
                centerX, centerY, glowRadius,
                glowColor, Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            alpha = if (isEdit) 140 else 90
        }
        canvas.drawCircle(centerX, centerY, glowRadius, glowPaint)

        // If editing existing source bitmap, blend it
        if (isEdit && sourceBitmap != null) {
            val scaledSource = Bitmap.createScaledBitmap(sourceBitmap, baseWidth, baseHeight, true)
            val blendPaint = Paint().apply { alpha = 160 }
            canvas.drawBitmap(scaledSource, 0f, 0f, blendPaint)
        }

        // 2. High-Tech Hexagonal & Coordinate Grid Lines
        val gridPaint = Paint().apply {
            color = Color.argb(40, 0, 240, 255)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val gridSize = 48f
        var x = 0f
        while (x < baseWidth) {
            canvas.drawLine(x, 0f, x, baseHeight.toFloat(), gridPaint)
            x += gridSize
        }
        var y = 0f
        while (y < baseHeight) {
            canvas.drawLine(0f, y, baseWidth.toFloat(), y, gridPaint)
            y += gridSize
        }

        // 3. Central Neural Core / Synaptic Rings
        val ringPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = glowColor
            alpha = 200
        }
        val ringRadii = listOf(baseWidth * 0.15f, baseWidth * 0.25f, baseWidth * 0.35f)
        for ((idx, r) in ringRadii.withIndex()) {
            canvas.drawCircle(centerX, centerY, r, ringPaint)
            // Draw notches
            val notchCount = 8 + idx * 4
            for (i in 0 until notchCount) {
                val angle = (i * (360.0 / notchCount) + rand.nextInt(30)) * Math.PI / 180.0
                val nx1 = centerX + (r - 6) * cos(angle).toFloat()
                val ny1 = centerY + (r - 6) * sin(angle).toFloat()
                val nx2 = centerX + (r + 6) * cos(angle).toFloat()
                val ny2 = centerY + (r + 6) * sin(angle).toFloat()
                canvas.drawLine(nx1, ny1, nx2, ny2, ringPaint)
            }
        }

        // 4. Synaptic Nodes & Laser Vector Connections
        val nodeCount = 18
        val nodePoints = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until nodeCount) {
            val angle = rand.nextDouble(0.0, 2.0 * Math.PI)
            val dist = rand.nextDouble(baseWidth * 0.05, baseWidth * 0.38)
            val px = centerX + (dist * cos(angle)).toFloat()
            val py = centerY + (dist * sin(angle)).toFloat()
            nodePoints.add(px to py)
        }

        val laserPaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(120, 200, 245, 255)
            strokeWidth = 1.5f
        }
        val nodeCirclePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        for (i in 0 until nodePoints.size) {
            val (px1, py1) = nodePoints[i]
            for (j in i + 1 until nodePoints.size) {
                val (px2, py2) = nodePoints[j]
                val d2 = (px1 - px2) * (px1 - px2) + (py1 - py2) * (py1 - py2)
                if (d2 < (baseWidth * 0.22f) * (baseWidth * 0.22f)) {
                    canvas.drawLine(px1, py1, px2, py2, laserPaint)
                }
            }
            canvas.drawCircle(px1, py1, 5f, nodeCirclePaint)
        }

        // 5. HUD Framing Brackets & Telemetry Labels
        val hudPaint = Paint().apply {
            color = Color.argb(220, 0, 240, 255)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val cornerSize = 36f
        val margin = 24f

        // Top-Left
        canvas.drawLine(margin, margin, margin + cornerSize, margin, hudPaint)
        canvas.drawLine(margin, margin, margin, margin + cornerSize, hudPaint)
        // Top-Right
        canvas.drawLine(baseWidth - margin - cornerSize, margin, baseWidth - margin, margin, hudPaint)
        canvas.drawLine(baseWidth - margin, margin, baseWidth - margin, margin + cornerSize, hudPaint)
        // Bottom-Left
        canvas.drawLine(margin, baseHeight - margin, margin + cornerSize, baseHeight - margin, hudPaint)
        canvas.drawLine(margin, baseHeight - margin - cornerSize, margin, baseHeight - margin, hudPaint)
        // Bottom-Right
        canvas.drawLine(baseWidth - margin - cornerSize, baseHeight - margin, baseWidth - margin, baseHeight - margin, hudPaint)
        canvas.drawLine(baseWidth - margin, baseHeight - margin - cornerSize, baseWidth - margin, baseHeight - margin, hudPaint)

        // Text HUD Overlay
        val textPaint = Paint().apply {
            color = Color.argb(220, 0, 240, 255)
            textSize = 20f
            isAntiAlias = true
            isFakeBoldText = true
        }
        canvas.drawText("DILAURO SYNAPSE-FS // NEURAL VISION CORE", margin + 8, margin + 28, textPaint)
        textPaint.textSize = 14f
        textPaint.color = Color.argb(160, 160, 190, 220)
        canvas.drawText("MODE: ${if (isEdit) "IMAGE-TO-IMAGE REFINEMENT" else "TEXT-TO-VISION GENERATION"}", margin + 8, margin + 48, textPaint)
        canvas.drawText("RATIO: ${aspectRatio.label} · HNSW VEC_DIM: 768", margin + 8, baseHeight - margin - 12, textPaint)

        return bitmap
    }

    fun generateVideoFrameSequence(
        prompt: String,
        aspectRatio: com.example.dilauro_aios.model.VideoAspectRatio,
        frameCount: Int = 6,
        sourceBitmap: Bitmap? = null
    ): List<Bitmap> {
        val baseWidth = 720
        val baseHeight = (baseWidth / aspectRatio.widthRatio * aspectRatio.heightRatio).toInt().coerceIn(400, 1280)
        val frames = mutableListOf<Bitmap>()

        val rand = java.util.Random(prompt.hashCode().toLong())

        for (frameIdx in 0 until frameCount) {
            val bitmap = Bitmap.createBitmap(baseWidth, baseHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val time = frameIdx.toFloat() / frameCount.toFloat()

            // Background
            val bgPaint = Paint().apply {
                color = Color.rgb(8, 12, 20)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, baseWidth.toFloat(), baseHeight.toFloat(), bgPaint)

            // If animated from photo, draw source with progressive zoom and pan
            if (sourceBitmap != null) {
                val scale = 1.0f + 0.15f * time
                val scaledW = (baseWidth * scale).toInt()
                val scaledH = (baseHeight * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(sourceBitmap, scaledW, scaledH, true)
                val dx = -((scaledW - baseWidth) * time * 0.5f)
                val dy = -((scaledH - baseHeight) * time * 0.5f)
                val photoPaint = Paint().apply { alpha = 175 }
                canvas.drawBitmap(scaled, dx, dy, photoPaint)
            }

            // Radial Glow
            val centerX = baseWidth / 2f
            val centerY = baseHeight / 2f
            val glowColor = when {
                prompt.contains("fire", ignoreCase = true) || prompt.contains("crimson", ignoreCase = true) -> Color.rgb(255, 60, 80)
                prompt.contains("purple", ignoreCase = true) || prompt.contains("cyber", ignoreCase = true) -> Color.rgb(180, 80, 255)
                else -> Color.rgb(0, 240, 255)
            }
            val glowPaint = Paint().apply {
                shader = RadialGradient(
                    centerX, centerY, baseWidth * 0.65f,
                    glowColor, Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                alpha = (90 + 30 * sin(time * Math.PI)).toInt()
            }
            canvas.drawCircle(centerX, centerY, baseWidth * 0.65f, glowPaint)

            // Moving Neon Scanning Beam
            val beamY = (baseHeight * time)
            val beamPaint = Paint().apply {
                color = Color.argb(120, 0, 240, 255)
                strokeWidth = 3f
            }
            canvas.drawLine(0f, beamY, baseWidth.toFloat(), beamY, beamPaint)

            // Rotating Neural Rings
            val ringPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = glowColor
                alpha = 190
            }
            val angleOffset = time * 360.0
            val ringR = baseWidth * 0.25f
            canvas.drawCircle(centerX, centerY, ringR, ringPaint)

            // Notches on rotating ring
            for (i in 0 until 12) {
                val angle = (i * 30.0 + angleOffset) * Math.PI / 180.0
                val nx1 = centerX + (ringR - 8) * cos(angle).toFloat()
                val ny1 = centerY + (ringR - 8) * sin(angle).toFloat()
                val nx2 = centerX + (ringR + 8) * cos(angle).toFloat()
                val ny2 = centerY + (ringR + 8) * sin(angle).toFloat()
                canvas.drawLine(nx1, ny1, nx2, ny2, ringPaint)
            }

            // Frame Timecode Overlay
            val hudPaint = Paint().apply {
                color = Color.argb(220, 0, 240, 255)
                textSize = 16f
                isAntiAlias = true
                isFakeBoldText = true
            }
            canvas.drawText("VEO-3.1 CINEMATIC FRAME ${frameIdx + 1}/$frameCount", 24f, 36f, hudPaint)
            hudPaint.textSize = 12f
            hudPaint.color = Color.argb(180, 200, 220, 240)
            canvas.drawText("RATIO: ${aspectRatio.label} · 1080P", 24f, 56f, hudPaint)

            frames.add(bitmap)
        }
        return frames
    }
}
