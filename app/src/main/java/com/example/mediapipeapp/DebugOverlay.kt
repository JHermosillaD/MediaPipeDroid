package com.example.mediapipeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint

@Composable
fun DebugDimensionsOverlay(
    imageWidth: Int,
    imageHeight: Int,
    modifier: Modifier = Modifier
) {
    // FPS Calculation State
    var lastFrameTime by remember { mutableLongStateOf(0L) }
    val currentFrameTime = System.currentTimeMillis()
    val fps = if (lastFrameTime > 0) 1000 / (currentFrameTime - lastFrameTime).coerceAtLeast(1) else 0
    lastFrameTime = currentFrameTime

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasW = size.width
        val canvasH = size.height

        val viewAspectRatio = canvasW / canvasH
        val imageAspectRatio = if (imageHeight > 0) imageWidth.toFloat() / imageHeight else 1f

        var scaleFactor = 1f
        var drawW = canvasW
        var drawH = canvasH
        var offsetX = 0f
        var offsetY = 0f

        // Standard FIT_CENTER math
        if (viewAspectRatio > imageAspectRatio) {
            scaleFactor = canvasH / imageHeight
            drawW = imageWidth * scaleFactor
            drawH = canvasH
            offsetX = (canvasW - drawW) / 2f
        } else {
            scaleFactor = canvasW / imageWidth
            drawW = canvasW
            drawH = imageHeight * scaleFactor
            offsetY = (canvasH - drawH) / 2f
        }

        val textPaint = Paint().apply {
            color = android.graphics.Color.GREEN
            textSize = 35f
            isFakeBoldText = true
            setShadowLayer(3f, 0f, 0f, android.graphics.Color.BLACK)
        }

        val debugText = """
            FPS: $fps
            Canvas: ${canvasW.toInt()} x ${canvasH.toInt()} -> AR: ${"%.2f".format(viewAspectRatio)}
            MP Image: $imageWidth x $imageHeight -> AR: ${"%.2f".format(imageAspectRatio)}
            Scale: ${"%.3f".format(scaleFactor)}
            Offset: X:${offsetX.toInt()}, Y:${offsetY.toInt()}
        """.trimIndent()

        var textY = 400f
        debugText.lines().forEach { line ->
            drawContext.canvas.nativeCanvas.drawText(line, 40f, textY, textPaint)
            textY += 45f
        }

        // Screen Size
        drawRect(
            color = Color.Red,
            style = Stroke(width = 24f)
        )

        // Active Area
        drawRect(
            color = Color.Yellow,
            topLeft = Offset(offsetX, offsetY),
            size = Size(drawW, drawH),
            style = Stroke(width = 6f)
        )

        // Center of the IMAGE
        val centerX = offsetX + (drawW / 2)
        val centerY = offsetY + (drawH / 2)
        val crossSize = 40f

        drawLine(
            color = Color.Cyan,
            start = Offset(centerX - crossSize, centerY),
            end = Offset(centerX + crossSize, centerY),
            strokeWidth = 5f
        )
        drawLine(
            color = Color.Cyan,
            start = Offset(centerX, centerY - crossSize),
            end = Offset(centerX, centerY + crossSize),
            strokeWidth = 5f
        )
    }
}