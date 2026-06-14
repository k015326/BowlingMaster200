package com.example.bowlingmaster200.ocr.pipeline

/**
 * OCR エンジンの生結果。
 * ML Kit / Gemini Vision 等は [engineId] で区別。
 */
data class OcrResult(
    val rawText: String,
    val lines: List<String> = emptyList(),
    val confidence: Float = 0f,
    val engineId: String,
    val debugInfo: Map<String, String> = emptyMap(),
)
