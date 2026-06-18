package com.example.bowlingmaster200.ocr.service

import android.util.Log
import com.example.bowlingmaster200.BuildConfig
import com.example.bowlingmaster200.ocr.analyzer.ScoreSheetAnalysisResult
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

    /** 1. OCR実行開始 */
    fun logOcrStart(
        source: String?,
        bitmapWidth: Int?,
        bitmapHeight: Int?,
    ) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            TAG,
            buildString {
                append("[OCR start]")
                source?.let { append(" source=$it") }
                if (bitmapWidth != null && bitmapHeight != null) {
                    append(" bitmap=${bitmapWidth}x${bitmapHeight}")
                }
            },
        )
    }

    /** 2. OCR生テキスト取得 */
    fun logOcrRawText(ocrResult: OcrResult) {
        if (!BuildConfig.DEBUG) return
        val isFallback = ocrResult.debugInfo["fallback"] == "true"
        val primaryRawText = ocrResult.debugInfo["primaryRawText"]
        val primaryLineCount = ocrResult.debugInfo["primaryLineCount"]

        Log.d(
            TAG,
            buildString {
                appendLine("[OCR raw text]")
                appendLine("  engineId=${ocrResult.engineId}")
                appendLine("  lineCount=${ocrResult.lines.size}")
                appendLine("  confidence=${ocrResult.confidence}")
                if (isFallback) {
                    appendLine("  fallback=true")
                    appendLine("  primaryEngine=${ocrResult.debugInfo["primaryEngine"]}")
                    appendLine("  primaryLineCount=${primaryLineCount ?: "0"}")
                    appendLine("  primaryRawText:")
                    appendRawTextLines(primaryRawText.orEmpty())
                    appendLine("  finalRawText (fallback engine):")
                } else {
                    appendLine("  rawText:")
                }
                appendRawTextLines(ocrResult.rawText)
            },
        )
    }

    /** 3. Parse開始 */
    fun logParseStart(engineId: String, rawTextLength: Int, lineCount: Int) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            TAG,
            "[Parse start] engineId=$engineId rawTextLength=$rawTextLength lineCount=$lineCount",
        )
    }

    /** 4. Parse結果 */
    fun logParseResult(analysis: ScoreSheetAnalysisResult) {
        if (!BuildConfig.DEBUG) return
        val parsedFrames = analysis.savedGame.frames.count { it.firstRoll != null }
        val isComplete = parsedFrames == analysis.savedGame.frames.size &&
            analysis.savedGame.frames.all { it.firstRoll != null }
        Log.d(
            TAG,
            buildString {
                appendLine("[Parse result]")
                appendLine("  engineId=${analysis.engineId}")
                appendLine("  parsedFrames=$parsedFrames/10")
                appendLine("  complete=$isComplete")
                appendLine("  confidence=${analysis.confidence}")
                if (analysis.warnings.isNotEmpty()) {
                    appendLine("  warnings=${analysis.warnings.joinToString("; ")}")
                }
            },
        )
    }

    /** 5. Fallback理由 */
    fun logFallbackReason(
        reason: String,
        primaryEngine: String?,
        primaryLineCount: Int?,
        primaryRawText: String?,
        exception: Exception? = null,
    ) {
        if (!BuildConfig.DEBUG) return
        Log.d(
            TAG,
            buildString {
                appendLine("[Fallback] reason=$reason")
                exception?.let { appendLine("  exception=${it.message}") }
                primaryEngine?.let { appendLine("  primaryEngine=$it") }
                primaryLineCount?.let { appendLine("  primaryLineCount=$it") }
                primaryRawText?.let {
                    appendLine("  primaryRawText:")
                    appendRawTextLines(it)
                }
            },
        )
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
                appendRawTextLines(result.rawText)
            },
        )
    }

    private fun StringBuilder.appendRawTextLines(text: String) {
        if (text.isBlank()) {
            appendLine("    | (empty)")
        } else {
            text.lines().forEach { line ->
                appendLine("    | $line")
            }
        }
    }
}
