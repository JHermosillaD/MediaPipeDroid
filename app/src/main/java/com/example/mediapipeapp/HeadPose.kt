package com.example.mediapipeapp

import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.sqrt

data class HeadPoseResult(
    val rotationMatrix: Mat,
    val translationVector: Mat,
    val eulerAngles: Triple<Double, Double, Double>,
    val xAxis: DoubleArray,
    val yAxis: DoubleArray,
    val zAxis: DoubleArray
)

class HeadPose(
    private val imageWidth: Int,
    private val imageHeight: Int
) {
    val cameraMatrix: Mat
    private val distCoeffs: MatOfDouble

    private val landmarkIndices = intArrayOf(1, 10, 103, 332, 127, 356)

    private val faceModel3D = MatOfPoint3f(
        Point3( 0.000,  0.000,  0.000),  // 1: Nose Tip
        Point3( 0.000, -0.050, -0.030),  // 10: Glabella
        Point3(-0.040, -0.070, -0.040),  // 103: Forehead L
        Point3( 0.040, -0.070, -0.040),  // 332: Forehead R
        Point3(-0.065, -0.030, -0.070),  // 127: Temple L
        Point3( 0.065, -0.030, -0.070)   // 356: Temple R
    )

    init {
        val calibrationRatio = 0.746025219
        val fx = imageWidth * calibrationRatio
        val fy = imageHeight * calibrationRatio
        val cx = imageWidth / 2.0
        val cy = imageHeight / 2.0

        cameraMatrix = Mat(3, 3, CvType.CV_64FC1)
        cameraMatrix.put(0, 0, fx, 0.0, cx, 0.0, fy, cy, 0.0, 0.0, 1.0)
        distCoeffs = MatOfDouble(0.0, 0.0, 0.0, 0.0, 0.0)
    }

    fun estimatePose(result: FaceLandmarkerResult?): HeadPoseResult? {
        val landmarks = result?.faceLandmarks()?.firstOrNull() ?: return null
        val points2D = MatOfPoint2f()
        val pointList = mutableListOf<Point>()

        for (idx in landmarkIndices) {
            if (idx < landmarks.size) {
                val lm = landmarks[idx]
                val pixelX = (1.0f - lm.x()) * imageWidth
                val pixelY = lm.y() * imageHeight
                pointList.add(Point(pixelX.toDouble(), pixelY.toDouble()))
            }
        }

        if (pointList.size != landmarkIndices.size) return null
        points2D.fromList(pointList)

        val rvec = Mat()
        val tvec = Mat()
        val rotationMatrix = Mat()

        return try {
            val success = Calib3d.solvePnP(
                faceModel3D, points2D, cameraMatrix, distCoeffs, rvec, tvec, false, Calib3d.SOLVEPNP_ITERATIVE
            )

            if (!success) return null

            Calib3d.Rodrigues(rvec, rotationMatrix)

            val xAxis = doubleArrayOf(
                rotationMatrix.get(0, 0)[0],
                rotationMatrix.get(1, 0)[0],
                rotationMatrix.get(2, 0)[0]
            )

            val yAxis = doubleArrayOf(
                rotationMatrix.get(0, 1)[0],
                rotationMatrix.get(1, 1)[0],
                rotationMatrix.get(2, 1)[0]
            )

            val zAxis = doubleArrayOf(
                rotationMatrix.get(0, 2)[0],
                rotationMatrix.get(1, 2)[0],
                rotationMatrix.get(2, 2)[0]
            )

            val eulerAngles = rotationMatrixToEulerAngles(rotationMatrix)

            HeadPoseResult(
                rotationMatrix = rotationMatrix.clone(),
                translationVector = tvec.clone(),
                eulerAngles = eulerAngles,
                xAxis = xAxis,
                yAxis = yAxis,
                zAxis = zAxis
            )
        } finally {
            rvec.release()
            tvec.release()
            rotationMatrix.release()
            points2D.release()
        }
    }

    private fun rotationMatrixToEulerAngles(R: Mat): Triple<Double, Double, Double> {
        if (R.rows() != 3 || R.cols() != 3) return Triple(0.0, 0.0, 0.0)

        val r = DoubleArray(9)
        R.get(0, 0, r)
        val sy = sqrt(r[0] * r[0] + r[3] * r[3])
        val singular = sy < 1e-6
        val pitch: Double
        val yaw: Double
        val roll: Double

        if (!singular) {
            pitch = Math.atan2(r[7], r[8])
            yaw = Math.atan2(-r[6], sy)
            roll = Math.atan2(r[3], r[0])
        } else {
            pitch = Math.atan2(-r[5], r[4])
            yaw = Math.atan2(-r[6], sy)
            roll = 0.0
        }

        return Triple(Math.toDegrees(pitch), Math.toDegrees(yaw), Math.toDegrees(roll))
    }

    fun release() {
        cameraMatrix.release()
        distCoeffs.release()
        faceModel3D.release()
    }
}