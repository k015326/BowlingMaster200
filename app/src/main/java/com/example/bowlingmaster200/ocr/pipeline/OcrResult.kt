package com.example.bowlingmaster200.ocr.pipeline

/**
 * OCR エンジンの生結果。
 * ML Kit / Gemini Vision 等は [engineId] で区別。
 */
data class OcrResult(
    val rawText: String,
    val lines: List<OcrLine> = emptyList(),
    val confidence: Float? = null,
    val engineId: String,
    val processedAtMillis: Long = System.currentTimeMillis(),
    val debugInfo: Map<String, String> = emptyMap(),
)

data class OcrLine(
    val text: String,
    val confidence: Float? = null,
    val lineIndex: Int = 0,
)
