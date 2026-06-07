package com.example.bowlingmaster200.ocr.service

import com.example.bowlingmaster200.ocr.pipeline.OcrInput
import com.example.bowlingmaster200.ocr.pipeline.OcrResult

/**
 * OCR エンジンの抽象。
 * ML Kit / Gemini Vision / その他を [engineId] で差し替え。
 */
interface OcrService {

    val engineId: String

    suspend fun recognize(input: OcrInput): OcrResult
}
