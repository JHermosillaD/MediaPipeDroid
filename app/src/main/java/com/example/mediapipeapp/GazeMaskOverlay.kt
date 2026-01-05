package com.example.mediapipeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.max

@Composable
fun GazeMaskOverlay(
    result: FaceLandmarkerResult?,
    imageWidth: Int,
    imageHeight: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val landmarks = result?.faceLandmarks()?.firstOrNull()
        if (landmarks == null || landmarks.isEmpty()) return@Canvas

        val canvasW = size.width
        val canvasH = size.height

        if (imageWidth <= 0 || imageHeight <= 0) return@Canvas

        val scale = max(canvasW / imageWidth.toFloat(), canvasH / imageHeight.toFloat())
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale
        val offsetX = (canvasW - scaledWidth) / 2f
        val offsetY = (canvasH - scaledHeight) / 2f

        val indices = mapOf(
            "nose" to 1,
            "glabella" to 10,
            "forehead_L" to 103,
            "forehead_R" to 332,
            "temple_L" to 127,
            "temple_R" to 356
        )

        fun getPoint(index: Int): Offset? {
            if (index >= landmarks.size) return null
            val lm = landmarks[index]
            val x = (1.0f - lm.x()) * imageWidth
            val y = lm.y() * imageHeight
            return Offset(x * scale + offsetX, y * scale + offsetY)
        }

        val nose = getPoint(indices["nose"] ?: return@Canvas) ?: return@Canvas
        val glabella = getPoint(indices["glabella"] ?: return@Canvas) ?: return@Canvas
        val foreL = getPoint(indices["forehead_L"] ?: return@Canvas) ?: return@Canvas
        val foreR = getPoint(indices["forehead_R"] ?: return@Canvas) ?: return@Canvas
        val tempL = getPoint(indices["temple_L"] ?: return@Canvas) ?: return@Canvas
        val tempR = getPoint(indices["temple_R"] ?: return@Canvas) ?: return@Canvas

        val shieldColor = Color(0xFF00E5FF).copy(alpha = 0.2f)
        val structureColor = Color(0xFF2979FF).copy(alpha = 0.4f)
        val wireColor = Color.White.copy(alpha = 0.5f)

        drawFilledTriangle(glabella, foreL, foreR, shieldColor)
        drawFilledTriangle(glabella, foreL, tempL, shieldColor)
        drawFilledTriangle(glabella, foreR, tempR, shieldColor)
        drawFilledTriangle(nose, glabella, foreL, structureColor)
        drawFilledTriangle(nose, glabella, foreR, structureColor)
        drawFilledTriangle(nose, tempL, foreL, structureColor)
        drawFilledTriangle(nose, tempR, foreR, structureColor)

        val stroke = Stroke(width = 3f, cap = StrokeCap.Round)
        val rimPath = Path().apply {
            moveTo(tempL.x, tempL.y)
            lineTo(foreL.x, foreL.y)
            lineTo(foreR.x, foreR.y)
            lineTo(tempR.x, tempR.y)
        }
        drawPath(rimPath, wireColor, style = stroke)
        drawLine(wireColor, nose, glabella, strokeWidth = 3f)
        drawLine(wireColor, nose, tempL, strokeWidth = 3f)
        drawLine(wireColor, nose, tempR, strokeWidth = 3f)

        val dotColor = Color.Magenta
        listOf(nose, glabella, foreL, foreR, tempL, tempR).forEach { point ->
            drawCircle(
                color = dotColor,
                radius = 18f,
                center = point
            )
        }
    }
}

private fun DrawScope.drawFilledTriangle(p1: Offset, p2: Offset, p3: Offset, color: Color) {
    val path = Path().apply {
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        close()
    }
    drawPath(path = path, color = color, style = Fill)
}