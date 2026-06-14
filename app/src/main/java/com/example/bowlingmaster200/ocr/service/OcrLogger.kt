package com.example.bowlingmaster200.ocr.service

import android.util.Log
import com.example.bowlingmaster200.ocr.pipeline.OcrResult

internal object OcrLogger {

    private const val TAG = "BowlingOCR"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
    }

    fun logOcrResult(result: OcrResult) {
        Log.d(
            TAG,
            buildString {
                appendLine("OCR result")
                appendLine("  engineId=${result.engineId}")
                appendLine("  confidence=${result.confidence}")
                appendLine("  lineCount=${result.lines.size}")
                appendLine("  debugInfo=${result.debugInfo}")
                appendLine("  rawText:")
                result.rawText.lines().forEach { line ->
                    appendLine("    | $line")
                }
            },
        )
    }
}
