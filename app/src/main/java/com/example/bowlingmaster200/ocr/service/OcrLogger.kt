package com.example.bowlingmaster200.ocr.service

import android.util.Log
import com.example.bowlingmaster200.BuildConfig
import com.example.bowlingmaster200.ocr.pipeline.OcrResult

internal object OcrLogger {

    private const val TAG = "BowlingOCR"

    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG && throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.w(TAG, message)
        }
    }

    fun logOcrResult(result: OcrResult) {
        if (!BuildConfig.DEBUG) return
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
