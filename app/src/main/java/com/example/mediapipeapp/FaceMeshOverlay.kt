package com.example.mediapipeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

@Composable
fun FaceMeshOverlay(
    result: FaceLandmarkerResult?,
    imageWidth: Int = 640,
    imageHeight: Int = 480
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val landmarks = result?.faceLandmarks()?.firstOrNull() ?: return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height
        val imgWidth = imageWidth.toFloat()
        val imgHeight = imageHeight.toFloat()
        val imageAspectRatio = imgWidth / imgHeight
        val canvasAspectRatio = canvasWidth / canvasHeight
        val scale: Float
        val offsetX: Float
        val offsetY: Float

        if (canvasAspectRatio > imageAspectRatio) {
            scale = canvasHeight / imgHeight
            offsetX = (canvasWidth - imgWidth * scale) / 2f
            offsetY = 0f
        } else {
            scale = canvasWidth / imgWidth
            offsetX = 0f
            offsetY = (canvasHeight - imgHeight * scale) / 2f
        }

        landmarks.forEachIndexed { index, point ->
            val x = (1 - point.y()) * imgWidth * scale + offsetX
            val y = (1 - point.x()) * imgHeight * scale + offsetY

            drawCircle(
                color = Color.Green,
                radius = 3f,
                center = Offset(x, y)
            )
        }
    }
}