package com.example.mediapipeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import kotlin.math.max
import kotlin.math.sqrt

@Composable
fun HeadPoseArrowOverlay(
    result: FaceLandmarkerResult?,
    headPoseResult: HeadPoseResult?,
    imageWidth: Int,
    imageHeight: Int,
    cameraMatrix: Mat
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (result == null || headPoseResult == null) return@Canvas

        val landmarks = result.faceLandmarks().firstOrNull() ?: return@Canvas
        if (landmarks.size <= 1) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height
        val imgWidth = imageWidth.toFloat()
        val imgHeight = imageHeight.toFloat()
        val scale = max(canvasWidth / imgWidth, canvasHeight / imgHeight)
        val scaledWidth = imgWidth * scale
        val scaledHeight = imgHeight * scale
        val offsetX = (canvasWidth - scaledWidth) / 2f
        val offsetY = (canvasHeight - scaledHeight) / 2f
        val noseLandmark = landmarks[1]
        val noseX = (1.0f - noseLandmark.x()) * imgWidth
        val noseY = noseLandmark.y() * imgHeight
        val origin2D = Offset(
            noseX * scale + offsetX,
            noseY * scale + offsetY
        )
        val axisLength = 0.05
        val axisPoints3D = MatOfPoint3f(
            Point3(axisLength, 0.0, 0.0),  // X-axis (Pitch)
            Point3(0.0, axisLength, 0.0),  // Y-axis (Yaw)
            Point3(0.0, 0.0, axisLength)   // Z-axis (Roll)
        )
        val projectedPoints = MatOfPoint2f()
        Calib3d.projectPoints(
            axisPoints3D,
            headPoseResult.rotationMatrix,
            headPoseResult.translationVector,
            cameraMatrix,
            MatOfDouble(0.0, 0.0, 0.0, 0.0, 0.0),
            projectedPoints
        )
        val pts = projectedPoints.toArray()
        if (pts.size < 3) return@Canvas

        fun transformPoint(pt: Point): Offset {
            val rawX = pt.x.toFloat()
            val rawY = pt.y.toFloat()
            val finalX = rawX * scale + offsetX
            val finalY = rawY * scale + offsetY
            return Offset(finalX, finalY)
        }

        val xEnd2D = transformPoint(pts[0])
        val yEnd2D = transformPoint(pts[1])
        val zEnd2D = transformPoint(pts[2])

        drawArrow(origin2D, xEnd2D, Color.Red, strokeWidth = 12f)
        drawArrow(origin2D, yEnd2D, Color.Green, strokeWidth = 12f)
        drawArrow(origin2D, zEnd2D, Color.Blue, strokeWidth = 12f)

        drawCircle(
            color = Color.Yellow,
            radius = 20f,
            center = origin2D
        )
    }
}

/**
 * Helper to draw an arrow with wings
 */
private fun DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float = 12f
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = sqrt(dx * dx + dy * dy)

    if (length < 1f) return

    val dirX = dx / length
    val dirY = dy / length
    val arrowSize = strokeWidth * 3f
    val arrowAngle = 0.5f
    val cos = kotlin.math.cos(arrowAngle.toDouble()).toFloat()
    val sin = kotlin.math.sin(arrowAngle.toDouble()).toFloat()
    val wing1X = end.x - arrowSize * (dirX * cos + dirY * sin)
    val wing1Y = end.y - arrowSize * (dirY * cos - dirX * sin)
    val wing2X = end.x - arrowSize * (dirX * cos - dirY * sin)
    val wing2Y = end.y - arrowSize * (dirY * cos + dirX * sin)

    drawLine(
        color = color,
        start = end,
        end = Offset(wing1X, wing1Y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    drawLine(
        color = color,
        start = end,
        end = Offset(wing2X, wing2Y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}