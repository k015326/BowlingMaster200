package com.example.bowlingmaster200.ocr.pipeline

import com.example.bowlingmaster200.ocr.analyzer.BowlingScoreSheetAnalyzer
import com.example.bowlingmaster200.ocr.analyzer.ScoreSheetAnalysisResult
import com.example.bowlingmaster200.ocr.camera.CameraFrame
import com.example.bowlingmaster200.ocr.mapper.OcrInputMapper
import com.example.bowlingmaster200.ocr.service.OcrService

/**
 * OCR 全体フローのオーケストレータ。
 *
 * ```
 * OcrInput → ImagePreprocessor → OcrService → BowlingScoreSheetAnalyzer → OcrPipelineResult
 * ```
 *
 * SpareMaster 移植時:
 * - [ImagePreprocessor] に OcrInputBitmapPipeline 相当を注入
 * - [OcrService] に ML Kit / Gemini 実装を注入
 */
class OcrPipeline(
    private val preprocessor: ImagePreprocessor = NoOpImagePreprocessor,
    private val ocrService: OcrService,
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
        fun createDefault(ocrService: OcrService): OcrPipeline {
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
