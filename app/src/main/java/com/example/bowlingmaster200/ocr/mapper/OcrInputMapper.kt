package com.example.bowlingmaster200.ocr.mapper

import android.graphics.Bitmap
import com.example.bowlingmaster200.camera.CapturedCameraFrame
import com.example.bowlingmaster200.ocr.camera.CameraFrame
import com.example.bowlingmaster200.ocr.camera.ImageFormat
import com.example.bowlingmaster200.ocr.pipeline.OcrImageSource
import com.example.bowlingmaster200.ocr.pipeline.OcrInput
import com.example.bowlingmaster200.ocr.pipeline.OcrInputMetadata
import java.io.ByteArrayOutputStream

/**
 * [CameraFrame] を [OcrInput] に変換する。
 * 実 OCR エンジンは [OcrInput] を受け取るため、ここで形式を揃える。
 */
object OcrInputMapper {

    fun fromCameraFrame(
        frame: CameraFrame,
        sourceLabel: String = "camera",
    ): OcrInput {
        return OcrInput(
            source = OcrImageSource.Camera(frame),
            metadata = OcrInputMetadata(
                sourceLabel = sourceLabel,
                rotationDegrees = frame.rotationDegrees,
            ),
        )
    }

    fun fromCapturedFrame(
        captured: CapturedCameraFrame,
        sourceLabel: String = "camera",
    ): OcrInput {
        return fromCameraFrame(captured.toCameraFrame(), sourceLabel)
    }

    private fun CapturedCameraFrame.toCameraFrame(): CameraFrame {
        return CameraFrame(
            imageBytes = bitmap.toJpegBytes(),
            width = width,
            height = height,
            rotationDegrees = rotationDegrees,
            format = ImageFormat.JPEG,
            capturedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun Bitmap.toJpegBytes(): ByteArray {
        return ByteArrayOutputStream().use { stream ->
            compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.toByteArray()
        }
    }
}
