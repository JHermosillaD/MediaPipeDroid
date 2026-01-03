package com.example.mediapipeapp

import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.sqrt
import org.opencv.core.MatOfDouble

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
    val distCoeffs: MatOfDouble

    private val landmarkIndices = intArrayOf(1, 33, 263, 61, 291, 200)
    private val faceModel3D = MatOfPoint3f(
        Point3( 0.000,  0.000,  0.000),  // Nose
        Point3( 0.032,  0.038, -0.028),  // Right eye
        Point3(-0.032,  0.038, -0.028),  // Left eye
        Point3( 0.028, -0.028, -0.020),  // Right mouth
        Point3(-0.028, -0.028, -0.020),  // Left mouth
        Point3( 0.000, -0.070, -0.010)   // Chin
    )

    init {
        val calibrationRatio = 0.746025219
        val fx = imageWidth * calibrationRatio
        val fy = imageHeight * calibrationRatio
        val cx = imageWidth / 2.0
        val cy = imageHeight / 2.0

        cameraMatrix = Mat(3, 3, CvType.CV_64FC1)
        cameraMatrix.put(0, 0,
            fx, 0.0, cx,
            0.0, fy, cy,
            0.0, 0.0, 1.0
        )

        distCoeffs = MatOfDouble(0.0, 0.0, 0.0, 0.0, 0.0)
    }

    fun setCameraIntrinsics(fx: Double, fy: Double, cx: Double, cy: Double) {
        cameraMatrix.put(0, 0,
            fx, 0.0, cx,
            0.0, fy, cy,
            0.0, 0.0, 1.0
        )
    }

    fun setDistortionCoefficients(k1: Double, k2: Double, p1: Double, p2: Double, k3: Double) {
        distCoeffs.put(0, 0, k1, k2, p1, p2, k3)
    }

    fun estimatePose(result: FaceLandmarkerResult?): HeadPoseResult? {
        val landmarks = result?.faceLandmarks()?.firstOrNull() ?: return null
        val points2D = MatOfPoint2f()
        val pointList = mutableListOf<Point>()

        for (idx in landmarkIndices) {
            if (idx < landmarks.size) {
                val lm = landmarks[idx]
                val x = (1 - lm.y()) * imageWidth
                val y = (1 - lm.x()) * imageHeight
                pointList.add(Point(x.toDouble(), y.toDouble()))
            }
        }

        if (pointList.size != landmarkIndices.size) {
            return null
        }

        points2D.fromList(pointList)

        val rvec = Mat()
        val tvec = Mat()
        val success = Calib3d.solvePnP(
            faceModel3D,
            points2D,
            cameraMatrix,
            distCoeffs,
            rvec,
            tvec,
            false,
            Calib3d.SOLVEPNP_ITERATIVE
        )

        if (!success) {
            return null
        }

        val rotationMatrix = Mat()
        Calib3d.Rodrigues(rvec, rotationMatrix)

        val xAxis = computeXAxis(rotationMatrix)
        val yAxis = computeYAxis(rotationMatrix, xAxis)
        val zAxis = computeZAxis(xAxis, yAxis)

        val finalRotation = buildRotationMatrix(xAxis, yAxis, zAxis)
        val eulerAngles = rotationMatrixToEulerAngles(finalRotation)

        return HeadPoseResult(
            rotationMatrix = finalRotation,
            translationVector = tvec,
            eulerAngles = eulerAngles,
            xAxis = xAxis,
            yAxis = yAxis,
            zAxis = zAxis
        )
    }

    private fun computeXAxis(R: Mat): DoubleArray {
        val z = doubleArrayOf(0.0, 0.0, 1.0)
        val xAxis = matVecMultiply(R, z)
        return normalize(xAxis)
    }

    private fun computeYAxis(R: Mat, xAxis: DoubleArray): DoubleArray {
        val lEye = doubleArrayOf(0.032, 0.038, -0.028)
        val rEye = doubleArrayOf(-0.032, 0.038, -0.028)
        val eyeDiff = doubleArrayOf(
            rEye[0] - lEye[0],
            rEye[1] - lEye[1],
            rEye[2] - lEye[2]
        )

        var lrCam = matVecMultiply(R, eyeDiff)

        val proj = dot(lrCam, xAxis)
        lrCam = doubleArrayOf(
            lrCam[0] - xAxis[0] * proj,
            lrCam[1] - xAxis[1] * proj,
            lrCam[2] - xAxis[2] * proj
        )

        val norm = magnitude(lrCam)

        return if (norm >= 1e-9) {
            doubleArrayOf(lrCam[0] / norm, lrCam[1] / norm, lrCam[2] / norm)
        } else {
            var upRef = doubleArrayOf(0.0, -1.0, 0.0)
            if (Math.abs(dot(xAxis, upRef)) > 0.98) {
                upRef = doubleArrayOf(1.0, 0.0, 0.0)
            }
            val y = cross(upRef, xAxis)
            normalize(y)
        }
    }

    private fun computeZAxis(xAxis: DoubleArray, yAxis: DoubleArray): DoubleArray {
        var z = cross(xAxis, yAxis)
        z = normalize(z)

        val upRef = doubleArrayOf(0.0, -1.0, 0.0)
        if (dot(z, upRef) < 0) {
            z = doubleArrayOf(-z[0], -z[1], -z[2])
        }

        return z
    }

    private fun buildRotationMatrix(x: DoubleArray, y: DoubleArray, z: DoubleArray): Mat {
        val R = Mat(3, 3, CvType.CV_64FC1)
        R.put(0, 0,
            x[0], y[0], z[0],
            x[1], y[1], z[1],
            x[2], y[2], z[2]
        )
        return R
    }

    private fun rotationMatrixToEulerAngles(R: Mat): Triple<Double, Double, Double> {
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

        return Triple(
            Math.toDegrees(pitch),
            -Math.toDegrees(yaw),
            Math.toDegrees(roll)
        )
    }

    private fun matVecMultiply(mat: Mat, vec: DoubleArray): DoubleArray {
        val result = DoubleArray(3)
        val m = DoubleArray(9)
        mat.get(0, 0, m)

        result[0] = m[0] * vec[0] + m[1] * vec[1] + m[2] * vec[2]
        result[1] = m[3] * vec[0] + m[4] * vec[1] + m[5] * vec[2]
        result[2] = m[6] * vec[0] + m[7] * vec[1] + m[8] * vec[2]

        return result
    }

    private fun dot(a: DoubleArray, b: DoubleArray): Double {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
    }

    private fun cross(a: DoubleArray, b: DoubleArray): DoubleArray {
        return doubleArrayOf(
            a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0]
        )
    }

    private fun magnitude(v: DoubleArray): Double {
        return sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
    }

    private fun normalize(v: DoubleArray): DoubleArray {
        val mag = magnitude(v)
        return if (mag > 1e-9) {
            doubleArrayOf(v[0] / mag, v[1] / mag, v[2] / mag)
        } else {
            v
        }
    }

    fun release() {
        cameraMatrix.release()
        distCoeffs.release()
        faceModel3D.release()
    }
}