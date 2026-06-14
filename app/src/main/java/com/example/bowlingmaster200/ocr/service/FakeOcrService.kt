package com.example.bowlingmaster200.ocr.service

import com.example.bowlingmaster200.ocr.pipeline.OcrInput
import com.example.bowlingmaster200.ocr.pipeline.OcrLine
import com.example.bowlingmaster200.ocr.pipeline.OcrResult

/**
 * UI 接続テスト用の固定ダミー OCR（[OcrEngine] 実装）。
 * [FakeOcrService.FAKE_SCORE_SHEET_TEXT] を返す。
 */
class FakeOcrService : OcrEngine {

    override val engineId: String = ENGINE_ID

    override suspend fun recognize(input: OcrInput): OcrResult {
        val lines = FAKE_SCORE_SHEET_TEXT.lines()
            .filter { it.isNotBlank() }
            .mapIndexed { index, line ->
                OcrLine(text = line.trim(), confidence = 1.0f, lineIndex = index)
            }

        return OcrResult(
            rawText = FAKE_SCORE_SHEET_TEXT,
            lines = lines,
            confidence = 1.0f,
            engineId = engineId,
            debugInfo = mapOf(
                "mode" to "fake",
                "source" to input.metadata.sourceLabel.orEmpty(),
            ),
        )
    }

    companion object {
        const val ENGINE_ID = "fake"

        /**
         * BowlingScoreSheetAnalyzer が解釈可能な簡易フォーマット。
         * 形式: F{frame}:{roll1},{roll2}[,{bonus}]
         */
        val FAKE_SCORE_SHEET_TEXT: String = """
            Bowling Score Sheet
            F1:10
            F2:10
            F3:10
            F4:10
            F5:10
            F6:10
            F7:10
            F8:10
            F9:10
            F10:10,10,10
        """.trimIndent()
    }
}
