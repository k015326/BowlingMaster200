package com.example.bowlingmaster200.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.bowlingmaster200.BuildConfig
import com.example.bowlingmaster200.ocr.service.OcrMlKitInputDebugSnapshot
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.bowlingmaster200.camera.CameraFrameCapture
import com.example.bowlingmaster200.camera.CapturedCameraFrame
import java.util.concurrent.Executors

private const val CAPTURE_AUTO_SHUTTER_DEBUG_TAG = "CaptureAutoShutterDebug"

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scanGeneration by viewModel.scanGeneration.collectAsStateWithLifecycle()

    CameraScreenContent(
        uiState = uiState,
        scanGeneration = scanGeneration,
        onFrameCaptured = viewModel::onFrameCaptured,
        onRescanClick = viewModel::requestRescan,
        modifier = modifier,
    )
}

@Composable
fun CameraScreenContent(
    uiState: CameraUiState,
    scanGeneration: Int,
    onFrameCaptured: (CapturedCameraFrame) -> Unit,
    onRescanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var autoShutterState by remember { mutableStateOf(CaptureAutoShutterState()) }
    val autoShutterAnalyzer = remember {
        CaptureAutoShutterAnalyzer { state ->
            mainHandler.post { autoShutterState = state }
        }
    }
    var autoShutterPaused by remember { mutableStateOf(false) }
    var showDebugSnapshot by remember { mutableStateOf(false) }
    var debugSnapshotBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val hasDebugSnapshot = !uiState.isProcessing && OcrMlKitInputDebugSnapshot.exists(context)

    DisposableEffect(showDebugSnapshot) {
        if (showDebugSnapshot && BuildConfig.DEBUG) {
            val file = OcrMlKitInputDebugSnapshot.snapshotFile(context)
            debugSnapshotBitmap = if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        }
        onDispose {
            debugSnapshotBitmap?.recycle()
            debugSnapshotBitmap = null
        }
    }

    DisposableEffect(hasCameraPermission, lifecycleOwner) {
        if (!hasCameraPermission) {
            imageCapture = null
            return@DisposableEffect onDispose { }
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, autoShutterAnalyzer) }
                imageCapture = capture
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture,
                    analysis,
                )
            } catch (_: Exception) {
                imageCapture = null
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            imageCapture = null
            autoShutterAnalyzer.reset()
            analysisExecutor.shutdown()
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    LaunchedEffect(scanGeneration) {
        autoShutterPaused = false
        autoShutterAnalyzer.reset()
    }

    LaunchedEffect(autoShutterState.ready, uiState.isProcessing, autoShutterPaused, imageCapture) {
        if (autoShutterState.ready) {
            Log.d(CAPTURE_AUTO_SHUTTER_DEBUG_TAG, "LaunchedEffect entered with ready=true")
        }
        Log.d(
            CAPTURE_AUTO_SHUTTER_DEBUG_TAG,
            "imageCapture is null=${imageCapture == null}",
        )

        val capture = imageCapture ?: return@LaunchedEffect
        if (
            !autoShutterState.ready ||
            uiState.isProcessing ||
            autoShutterPaused
        ) {
            return@LaunchedEffect
        }

        autoShutterPaused = true
        Log.d(CAPTURE_AUTO_SHUTTER_DEBUG_TAG, "calling takePicture")
        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    Log.d(CAPTURE_AUTO_SHUTTER_DEBUG_TAG, "onCaptureSuccess")
                    onFrameCaptured(CameraFrameCapture.fromImageProxy(image))
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.d(
                        CAPTURE_AUTO_SHUTTER_DEBUG_TAG,
                        "onError: ${exception.message}",
                        exception,
                    )
                    autoShutterPaused = false
                    autoShutterAnalyzer.reset()
                }
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize(),
                    )
                    CaptureGuideOverlay(
                        frameReady = autoShutterState.ready,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Camera permission required")
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
            if (uiState.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp),
                )
            }

            Text(
                text = "OCR Score Scan",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))
            OcrStatusBanner(uiState = uiState)

            Spacer(modifier = Modifier.height(8.dp))
            OcrScoreHeader(uiState = uiState)

            if (uiState.isScoreComplete) {
                Text(
                    text = "Complete game",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            OcrFrameScoreGrid(frames = uiState.frameDisplays)

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            OcrRecognizedTextPanel(rawText = uiState.rawOcrText)

            Spacer(modifier = Modifier.height(8.dp))
            OcrWarningsPanel(warnings = uiState.warnings)

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (BuildConfig.DEBUG) {
                    OutlinedButton(
                        onClick = { showDebugSnapshot = true },
                        enabled = hasDebugSnapshot,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("Open Debug Snapshot")
                    }
                }
                Button(
                    onClick = onRescanClick,
                    enabled = hasCameraPermission && !uiState.isProcessing,
                ) {
                    Text("Rescan")
                }
            }
            }
        }

        if (showDebugSnapshot && BuildConfig.DEBUG) {
            AlertDialog(
                onDismissRequest = { showDebugSnapshot = false },
                title = { Text("ML Kit Input Snapshot") },
                text = {
                    val bitmap = debugSnapshotBitmap
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "ML Kit debug snapshot",
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 480.dp),
                            contentScale = ContentScale.FillWidth,
                        )
                    } else {
                        Text("No snapshot yet. Run OCR first.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDebugSnapshot = false }) {
                        Text("Close")
                    }
                },
            )
        }
    }
}
