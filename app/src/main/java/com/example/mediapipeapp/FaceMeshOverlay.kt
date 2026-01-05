package com.example.mediapipeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.max

@Composable
fun FaceMeshOverlay(
    result: FaceLandmarkerResult?,
    imageWidth: Int,
    imageHeight: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val faceLandmarks = result?.faceLandmarks()
        if (!faceLandmarks.isNullOrEmpty()) {
            val landmarks = faceLandmarks[0]
            val canvasW = size.width
            val canvasH = size.height
            val scaleFactor = max(canvasW / imageWidth.toFloat(), canvasH / imageHeight.toFloat())
            val scaledWidth = imageWidth * scaleFactor
            val scaledHeight = imageHeight * scaleFactor
            val offsetX = (canvasW - scaledWidth) / 2f
            val offsetY = (canvasH - scaledHeight) / 2f

            val points = landmarks.map { landmark ->
                val mirroredX = 1.0f - landmark.x()
                val x = (mirroredX * imageWidth * scaleFactor) + offsetX
                val y = (landmark.y() * imageHeight * scaleFactor) + offsetY
                Offset(x, y)
            }

            drawPoints(
                points = points,
                pointMode = PointMode.Points,
                color = Color.Green,
                strokeWidth = 10f,
                cap = StrokeCap.Round
            )
        }
    }
}