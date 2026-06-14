package com.example.bowlingmaster200.ocr.usecase

import com.example.bowlingmaster200.ocr.adapter.OcrProcessUiResult
import com.example.bowlingmaster200.ocr.adapter.OcrToCalculatorAdapter
import com.example.bowlingmaster200.ocr.pipeline.OcrPipelineResult

/**
 * [OcrPipelineResult] を受け取り、成功時のみ Calculator 接続を行う。
 */
class ProcessOcrResultUseCase(
    private val adapter: OcrToCalculatorAdapter = OcrToCalculatorAdapter(),
) {

    fun execute(pipelineResult: OcrPipelineResult): OcrProcessUiResult {
        val analysis = pipelineResult.analysis
        if (analysis.savedGame == null) {
            return OcrProcessUiResult(
                warnings = analysis.warnings,
                confidence = analysis.confidence,
                engineId = analysis.engineId,
                errorMessage = "OCR result has no game data",
            )
        }
        return adapter.adapt(analysis)
    }
}
