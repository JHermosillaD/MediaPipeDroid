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
    var frameCount by remember { mutableIntStateOf(0) }
    var lastFpsUpdateTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var fps by remember { mutableIntStateOf(0) }

    Canvas(modifier = modifier.fillMaxSize()) {
        frameCount++

        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastFpsUpdateTime

        if (elapsed >= 1000) {
            fps = ((frameCount * 1000) / elapsed).toInt()
            frameCount = 0
            lastFpsUpdateTime = currentTime
        }

        val canvasW = size.width
        val canvasH = size.height
        val viewAspectRatio = canvasW / canvasH
        val imageAspectRatio = if (imageHeight > 0) imageWidth.toFloat() / imageHeight else 1f
        var scaleFactor = 1f
        var drawW = canvasW
        var drawH = canvasH
        var offsetX = 0f
        var offsetY = 0f

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
            color = android.graphics.Color.BLACK
            textSize = 50f
            isFakeBoldText = true
        }

        val debugText = """
            FPS: $fps
            Canvas: ${canvasW.toInt()} x ${canvasH.toInt()} -> AR: ${"%.2f".format(viewAspectRatio)}
            MP Image: $imageWidth x $imageHeight -> AR: ${"%.2f".format(imageAspectRatio)}
            Scale: ${"%.2f".format(scaleFactor)}
            Offset: X:${offsetX.toInt()}, Y:${offsetY.toInt()}
        """.trimIndent()

        var textY = 400f
        debugText.lines().forEach { line ->
            drawContext.canvas.nativeCanvas.drawText(line, 40f, textY, textPaint)
            textY += 55f
        }

        val debugSign = """
            By JHermosillaD using MediaPipe
        """.trimIndent()

        val signY = offsetY + drawH - 60f
        val signX = drawW / 4
        debugSign.lines().forEach { line ->
            drawContext.canvas.nativeCanvas.drawText(line, signX, signY, textPaint)
        }

        drawRect(
            color = Color.Red,
            style = Stroke(width = 24f)
        )
        drawRect(
            color = Color.Yellow,
            topLeft = Offset(offsetX, offsetY),
            size = Size(drawW, drawH),
            style = Stroke(width = 6f)
        )

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