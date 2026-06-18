package com.example.bowlingmaster200.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.mutableIntStateOf
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

    val previewView = remember { PreviewView(context) }
    var lastCapturedGeneration by remember { mutableIntStateOf(-1) }
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

    LaunchedEffect(hasCameraPermission, scanGeneration) {
        if (!hasCameraPermission) return@LaunchedEffect
        if (lastCapturedGeneration == scanGeneration) return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture,
                )

                capture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            lastCapturedGeneration = scanGeneration
                            onFrameCaptured(CameraFrameCapture.fromImageProxy(image))
                        }

                        override fun onError(exception: ImageCaptureException) = Unit
                    },
                )
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = "Camera permission required",
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = 360.dp)
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
