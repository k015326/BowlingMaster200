package com.example.bowlingmaster200.ocr.service

import com.example.bowlingmaster200.ocr.pipeline.OcrInput
import com.example.bowlingmaster200.ocr.pipeline.OcrResult

/**
 * OCR エンジンの抽象（[OcrEngine] の実体）。
 * 差し替え実装は [engineId] と [recognize] のみ提供すればよい。
 */
interface OcrService {

    val engineId: String

    suspend fun recognize(input: OcrInput): OcrResult
}
