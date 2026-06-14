package com.example.bowlingmaster200.ocr.service

import com.example.bowlingmaster200.ocr.pipeline.OcrInput
import com.example.bowlingmaster200.ocr.pipeline.OcrResult

/**
 * 本番フォールバック用の空 OCR（Fake OCR は使用しない）。
 */
internal class EmptyOcrEngine : OcrEngine {

    override val engineId: String = ENGINE_ID

    override suspend fun recognize(input: OcrInput): OcrResult {
        return OcrSafeResults.emptyOcrResult(
            engineId = engineId,
            reason = "ocr_unavailable",
        )
    }

    companion object {
        const val ENGINE_ID = "none"
    }
}
