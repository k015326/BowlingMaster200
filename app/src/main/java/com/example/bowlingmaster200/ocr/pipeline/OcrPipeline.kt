package com.example.bowlingmaster200.ocr.pipeline

import com.example.bowlingmaster200.ocr.analyzer.BowlingScoreSheetAnalyzer
import com.example.bowlingmaster200.ocr.analyzer.ScoreSheetAnalysisResult
import com.example.bowlingmaster200.ocr.camera.CameraFrame
import com.example.bowlingmaster200.ocr.mapper.OcrInputMapper
import com.example.bowlingmaster200.ocr.service.OcrEngine
import com.example.bowlingmaster200.ocr.service.OcrServiceFactory

/**
 * OCR 全体フローのオーケストレータ。
 *
 * ```
 * OcrInput → ImagePreprocessor → OcrEngine → BowlingScoreSheetAnalyzer → OcrPipelineResult
 * ```
 *
 * DI: [createDefault] は [OcrServiceFactory] 経由で [OcrEngine] を注入する。
 */
class OcrPipeline(
    private val preprocessor: ImagePreprocessor = NoOpImagePreprocessor,
    private val ocrService: OcrEngine,
    private val analyzer: BowlingScoreSheetAnalyzer = BowlingScoreSheetAnalyzer(),
) {

    suspend fun execute(input: OcrInput): OcrPipelineResult {
        val preprocessed = preprocessor.preprocess(input)
        val ocrResult = ocrService.recognize(preprocessed)
        val analysis = analyzer.analyze(ocrResult)
        return OcrPipelineResult(
            input = preprocessed,
            ocrResult = ocrResult,
            analysis = analysis,
        )
    }

    suspend fun executeFromCamera(frame: CameraFrame): OcrPipelineResult {
        return execute(OcrInputMapper.fromCameraFrame(frame))
    }

    companion object {
        fun createDefault(
            ocrService: OcrEngine = OcrServiceFactory.create(),
        ): OcrPipeline {
            return OcrPipeline(
                preprocessor = NoOpImagePreprocessor,
                ocrService = ocrService,
            )
        }
    }
}

data class OcrPipelineResult(
    val input: OcrInput,
    val ocrResult: OcrResult,
    val analysis: ScoreSheetAnalysisResult,
)
