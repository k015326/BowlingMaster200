package com.example.bowlingmaster200.ocr.pipeline

import com.example.bowlingmaster200.ocr.analyzer.BowlingScoreSheetAnalyzer
import com.example.bowlingmaster200.ocr.analyzer.ScoreSheetAnalysisResult
import com.example.bowlingmaster200.ocr.camera.CameraFrame
import com.example.bowlingmaster200.ocr.mapper.OcrInputMapper
import com.example.bowlingmaster200.ocr.service.OcrEngine
import com.example.bowlingmaster200.ocr.service.OcrLogger
import com.example.bowlingmaster200.ocr.service.OcrSafeResults
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
        return try {
            val (width, height) = inputImageSize(input)
            OcrLogger.logOcrStart(
                source = input.metadata.sourceLabel,
                bitmapWidth = width,
                bitmapHeight = height,
            )

            val preprocessed = safePreprocess(input)
            val ocrResult = safeRecognize(preprocessed)
            OcrLogger.logOcrRawText(ocrResult)

            OcrLogger.logParseStart(
                engineId = ocrResult.engineId,
                rawTextLength = ocrResult.rawText.length,
                lineCount = ocrResult.lines.size,
            )
            val analysis = safeAnalyze(ocrResult)
            OcrLogger.logParseResult(analysis)

            OcrPipelineResult(
                input = preprocessed,
                ocrResult = ocrResult,
                analysis = analysis,
            )
        } catch (error: Exception) {
            OcrLogger.e("OcrPipeline.execute failed", error)
            OcrSafeResults.safePipelineResult(
                input = input,
                reason = error.message ?: "pipeline_error",
            )
        }
    }

    suspend fun executeFromCamera(frame: CameraFrame): OcrPipelineResult {
        return try {
            if (frame.imageBytes.isEmpty()) {
                OcrLogger.d("Empty camera frame bytes")
                return OcrSafeResults.safePipelineResult(
                    input = OcrInputMapper.fromCameraFrame(frame),
                    reason = "empty_camera_frame",
                )
            }
            execute(OcrInputMapper.fromCameraFrame(frame))
        } catch (error: Exception) {
            OcrLogger.e("OcrPipeline.executeFromCamera failed", error)
            OcrSafeResults.safePipelineResult(
                input = OcrInput(source = OcrImageSource.Camera(frame)),
                reason = error.message ?: "camera_pipeline_error",
            )
        }
    }

    private fun inputImageSize(input: OcrInput): Pair<Int?, Int?> {
        return when (val source = input.source) {
            is OcrImageSource.Camera -> source.frame.width to source.frame.height
            else -> null to null
        }
    }

    private fun safePreprocess(input: OcrInput): OcrInput {
        return try {
            preprocessor.preprocess(input)
        } catch (error: Exception) {
            OcrLogger.e("Preprocessor failed, using raw input", error)
            input
        }
    }

    private suspend fun safeRecognize(input: OcrInput): OcrResult {
        return try {
            ocrService.recognize(input)
        } catch (error: Exception) {
            OcrLogger.e("OCR recognize failed in pipeline", error)
            OcrSafeResults.emptyOcrResult(
                engineId = ocrService.engineId,
                reason = error.message ?: "recognize_error",
            )
        }
    }

    private fun safeAnalyze(ocrResult: OcrResult): ScoreSheetAnalysisResult {
        return try {
            analyzer.analyze(ocrResult)
        } catch (error: Exception) {
            OcrLogger.e("Analyzer failed in pipeline", error)
            OcrSafeResults.safeAnalysis(
                ocrResult = ocrResult,
                warnings = listOf(error.message ?: "analyze_error"),
            )
        }
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
