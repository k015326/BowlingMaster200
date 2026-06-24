package com.example.bowlingmaster200.ocr.service

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.bowlingmaster200.ocr.image.BitmapRotationUtils
import com.example.bowlingmaster200.ocr.image.OcrInputBitmapPipeline
import com.example.bowlingmaster200.ocr.pipeline.OcrImageSource
import com.example.bowlingmaster200.ocr.pipeline.OcrInput
import com.google.mlkit.vision.common.InputImage
import java.io.File

/**
 * [OcrInput] を ML Kit [InputImage] に変換する。
 * Step1: 物理回転 / Step2: crop・trim・1280px 正規化（SpareMaster 移植）。
 */
internal object OcrInputImageConverter {

    fun toInputImage(context: Context, input: OcrInput): InputImage {
        return try {
            when (val source = input.source) {
                is OcrImageSource.Camera -> fromBytes(
                    context = context,
                    bytes = source.frame.imageBytes,
                    rotationDegrees = source.frame.rotationDegrees,
                )
                is OcrImageSource.Bytes -> fromBytes(
                    context = context,
                    bytes = source.data,
                    rotationDegrees = input.metadata.rotationDegrees,
                )
                is OcrImageSource.FilePath -> {
                    val file = File(source.path)
                    if (!file.exists() || file.length() == 0L) {
                        error("Image file missing or empty: ${source.path}")
                    }
                    InputImage.fromFilePath(context, Uri.fromFile(file))
                }
            }
        } catch (error: Exception) {
            OcrLogger.e("OcrInputImageConverter failed", error)
            throw error
        }
    }

    private fun fromBytes(
        context: Context,
        bytes: ByteArray,
        rotationDegrees: Int,
    ): InputImage {
        if (bytes.isEmpty()) {
            error("Empty image bytes")
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Corrupt image bytes")
        val oriented = BitmapRotationUtils.rotate(decoded, rotationDegrees)
        try {
            val prepared = OcrInputBitmapPipeline.prepareFromOrientedBitmap(oriented)
                ?: error("OCR bitmap preparation failed")
            val forOcr = prepared.bitmap
            OcrLogger.logOcrRegionDetail(
                debugInfo = prepared.debugInfo,
                outputWidth = forOcr.width,
                outputHeight = forOcr.height,
            )
            OcrMlKitInputDebugSnapshot.save(context, forOcr, rotationDegrees = 0)
            OcrLogger.logSnapshotSaved(context, forOcr)
            OcrLogger.logSnapshotBitmapCorrelation(
                context = context,
                phase = "ocr_mlkit_input",
                bitmap = forOcr,
            )
            return InputImage.fromBitmap(forOcr, 0)
        } finally {
            if (oriented !== decoded) {
                oriented.recycle()
            }
            decoded.recycle()
        }
    }
}
