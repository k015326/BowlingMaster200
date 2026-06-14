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
        return when (val source = input.source) {
            is OcrImageSource.Camera -> fromBytes(
                bytes = source.frame.imageBytes,
                rotationDegrees = source.frame.rotationDegrees,
            )
            is OcrImageSource.Bytes -> fromBytes(
                bytes = source.data,
                rotationDegrees = input.metadata.rotationDegrees,
            )
            is OcrImageSource.FilePath -> InputImage.fromFilePath(
                context,
                Uri.fromFile(File(source.path)),
            )
        }
    }

    private fun fromBytes(bytes: ByteArray, rotationDegrees: Int): InputImage {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Failed to decode image bytes for ML Kit OCR")
        return InputImage.fromBitmap(bitmap, rotationDegrees)
    }
}
