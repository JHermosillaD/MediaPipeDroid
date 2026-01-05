package com.example.mediapipeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import kotlin.math.max

@Composable
fun GazePointOverlay(
    result: FaceLandmarkerResult?,
    headPoseResult: HeadPoseResult?,
    imageWidth: Int,
    imageHeight: Int,
    cameraMatrix: Mat
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (result == null || headPoseResult == null) return@Canvas

        val landmarks = result.faceLandmarks().firstOrNull() ?: return@Canvas
        val noseLandmark = landmarks[1]
        val canvasWidth = size.width
        val canvasHeight = size.height
        val imgWidth = imageWidth.toFloat()
        val imgHeight = imageHeight.toFloat()
        val scale = max(canvasWidth / imgWidth, canvasHeight / imgHeight)
        val scaledWidth = imgWidth * scale
        val scaledHeight = imgHeight * scale
        val offsetX = (canvasWidth - scaledWidth) / 2f
        val offsetY = (canvasHeight - scaledHeight) / 2f
        val noseScreenX = ((1.0f - noseLandmark.x()) * imgWidth * scale) + offsetX
        val noseScreenY = (noseLandmark.y() * imgHeight * scale) + offsetY
        val gazeDistance = 0.25
        val points3D = MatOfPoint3f(
            Point3(0.0, 0.0, 0.0),
            Point3(0.0, 0.0, gazeDistance)
        )

        val projectedPoints = MatOfPoint2f()
        Calib3d.projectPoints(
            points3D,
            headPoseResult.rotationMatrix,
            headPoseResult.translationVector,
            cameraMatrix,
            MatOfDouble(0.0, 0.0, 0.0, 0.0, 0.0),
            projectedPoints
        )

        val points = projectedPoints.toArray()
        if (points.size < 2) return@Canvas

        val rawNose3D = points[0]
        val rawGaze3D = points[1]
        val deltaX = (rawGaze3D.x - rawNose3D.x).toFloat()
        val deltaY = (rawGaze3D.y - rawNose3D.y).toFloat()
        val gazeScreenX = noseScreenX - (deltaX * scale)
        val gazeScreenY = noseScreenY - (deltaY * scale)

        drawCircle(
            color = Color.Black.copy(alpha = 0.5f),
            radius = 70f,
            center = Offset(gazeScreenX, gazeScreenY)
        )
    }
}