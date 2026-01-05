package com.example.mediapipeapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

class MediaPipeAnalyzer(
    context: Context,
    private val onResults: (FaceLandmarkerResult, Int, Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val landmarker: FaceLandmarker
    private val matrix = Matrix()

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task")
            .build()

        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setMinFaceDetectionConfidence(0.5f)
            .setNumFaces(1)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, image ->
                onResults(result, image.width, image.height)
            }
            .build()

        landmarker = FaceLandmarker.createFromOptions(context, options)
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        try {
            val mediaImage = imageProxy.image

            if (mediaImage != null) {
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                var bitmap = imageProxy.toBitmap()

                if (bitmap.width > bitmap.height) {
                    matrix.reset()
                    matrix.postRotate(rotationDegrees.toFloat())
                    val rotatedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                    )

                    if (rotatedBitmap != bitmap) {
                        bitmap = rotatedBitmap
                    }
                }

                val mpImage = BitmapImageBuilder(bitmap).build()
                landmarker.detectAsync(mpImage, imageProxy.imageInfo.timestamp / 1_000_000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }

    fun close() {
        landmarker.close()
    }
}