package com.example.bowlingmaster200.ocr.pipeline

import com.example.bowlingmaster200.ocr.camera.CameraFrame

/**
 * OCR パイプラインへの入力。
 * 画像ソースを抽象化し、Camera / File / Bytes を統一扱い。
 */
data class OcrInput(
    val source: OcrImageSource,
    val metadata: OcrInputMetadata = OcrInputMetadata(),
)

sealed interface OcrImageSource {
    data class Bytes(val data: ByteArray, val mimeType: String) : OcrImageSource
    data class FilePath(val path: String) : OcrImageSource
    data class Camera(val frame: CameraFrame) : OcrImageSource
}

data class OcrInputMetadata(
    val sourceLabel: String? = null,
    val captureGuideApplied: Boolean = false,
    val rotationDegrees: Int = 0,
    val extras: Map<String, String> = emptyMap(),
)
