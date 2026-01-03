package com.example.mediapipeapp

import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.opencv.android.OpenCVLoader
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var mediaPipeAnalyzer: MediaPipeAnalyzer? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                setContent { AppEntryPoint(true) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Failed to load OpenCV")
        } else {
            Log.i("OpenCV", "OpenCV loaded successfully")
        }

        val isGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            setContent { AppEntryPoint(true) }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            setContent { AppEntryPoint(false) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPipeAnalyzer?.close()
        cameraExecutor.shutdown()
    }
}

@Composable
fun AppEntryPoint(isPermissionGranted: Boolean) {
    if (isPermissionGranted) {
        CameraScreen()
    } else {
        Text(text = "Please enable camera permissions to use this app.")
    }
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentFaceResult by remember { mutableStateOf<FaceLandmarkerResult?>(null) }
    var imageWidth by remember { mutableIntStateOf(640) }
    var imageHeight by remember { mutableIntStateOf(480) }
    var headPoseResult by remember { mutableStateOf<HeadPoseResult?>(null) }
    val headPose = remember(imageWidth, imageHeight) {
        HeadPose(imageWidth, imageHeight)
    }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            headPose.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(Size(640, 480))
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()

                    val mediaPipeAnalyzer = MediaPipeAnalyzer(ctx) { result, width, height ->
                        (ctx as? ComponentActivity)?.runOnUiThread {
                            currentFaceResult = result
                            imageWidth = width
                            imageHeight = height
                            headPoseResult = headPose.estimatePose(result)
                        }
                    }

                    analyzer.setAnalyzer(executor, mediaPipeAnalyzer)

                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            analyzer
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        FaceMeshOverlay(
            result = currentFaceResult,
            imageWidth = imageWidth,
            imageHeight = imageHeight
        )

        HeadPoseArrowOverlay(
            result = currentFaceResult,
            headPoseResult = headPoseResult,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            cameraMatrix = headPose.cameraMatrix
        )

        headPoseResult?.let { pose ->
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Pitch: %.1f°".format(pose.eulerAngles.first),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Yaw: %.1f°".format(pose.eulerAngles.second),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Roll: %.1f°".format(pose.eulerAngles.third),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}