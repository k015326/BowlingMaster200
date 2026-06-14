package com.example.bowlingmaster200.ocr.service

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.bowlingmaster200.ocr.pipeline.OcrImageSource
import com.example.bowlingmaster200.ocr.pipeline.OcrInput
import com.google.mlkit.vision.common.InputImage
import java.io.File

/**
 * [OcrInput] を ML Kit [InputImage] に変換する（テキスト抽出前の最小変換のみ）。
 */
internal object OcrInputImageConverter {

    fun toInputImage(context: Context, input: OcrInput): InputImage {
        return try {
            when (val source = input.source) {
                is OcrImageSource.Camera -> fromBytes(
                    bytes = source.frame.imageBytes,
                    rotationDegrees = source.frame.rotationDegrees,
                )
                is OcrImageSource.Bytes -> fromBytes(
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

    private fun fromBytes(bytes: ByteArray, rotationDegrees: Int): InputImage {
        if (bytes.isEmpty()) {
            error("Empty image bytes")
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (decoded == null) {
            error("Corrupt image bytes")
        }
        val inputImage = InputImage.fromBitmap(decoded, rotationDegrees)
        decoded.recycle()
        return inputImage
    }
}
