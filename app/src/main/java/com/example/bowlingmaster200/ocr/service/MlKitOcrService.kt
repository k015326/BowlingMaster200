package com.example.bowlingmaster200.ocr.service

import android.content.Context
import com.example.bowlingmaster200.BuildConfig
import com.example.bowlingmaster200.ocr.pipeline.OcrInput
import com.example.bowlingmaster200.ocr.pipeline.OcrResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * ML Kit テキスト認識による [OcrEngine] 実装。
 * スコアシート解析は [com.example.bowlingmaster200.ocr.analyzer.BowlingScoreSheetAnalyzer] に委譲する。
 */
class MlKitOcrService(
    private val context: Context,
) : OcrEngine {

    override val engineId: String = ENGINE_ID

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognize(input: OcrInput): OcrResult {
        return try {
            OcrLogger.d("MlKitOcrService.recognize source=${input.metadata.sourceLabel}")
            val inputImage = OcrInputImageConverter.toInputImage(context, input)
            val visionText = recognizeText(inputImage)
            toOcrResult(input, visionText).also { OcrLogger.logOcrResult(it) }
        } catch (error: Exception) {
            OcrLogger.e("MlKitOcrService.recognize failed", error)
            OcrSafeResults.emptyOcrResult(
                engineId = engineId,
                reason = error.message ?: "mlkit_error",
            )
        }
    }

    private suspend fun recognizeText(inputImage: InputImage): Text {
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    if (continuation.isActive) {
                        continuation.resume(text)
                    }
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
        }
    }

    private fun toOcrResult(input: OcrInput, visionText: Text): OcrResult {
        val normalized = try {
            OcrTextNormalizer.fromMlKitText(visionText)
        } catch (error: Exception) {
            OcrLogger.e("ML Kit text normalization failed", error)
            return OcrSafeResults.emptyOcrResult(
                engineId = engineId,
                reason = error.message ?: "normalize_error",
            )
        }
        val rawText = normalized.rawText.ifBlank { "" }

        return OcrResult(
            rawText = rawText,
            lines = normalized.lines,
            confidence = estimateConfidence(normalized),
            engineId = engineId,
            debugInfo = buildMlKitDebugInfo(input, normalized, rawText),
        )
    }

    private fun buildMlKitDebugInfo(
        input: OcrInput,
        normalized: OcrTextNormalizer.NormalizedText,
        rawText: String,
    ): Map<String, String> {
        if (!BuildConfig.DEBUG) {
            return mapOf("engine" to "mlkit")
        }
        return mapOf(
            "engine" to "mlkit",
            "source" to input.metadata.sourceLabel.orEmpty(),
            "blockCount" to normalized.blockCount.toString(),
            "lineCount" to normalized.lines.size.toString(),
            "droppedLines" to normalized.droppedLineCount.toString(),
            "acceptedFrames" to normalized.filteredLineCount.toString(),
            "rejectedLines" to normalized.rejectedLineCount.toString(),
            "usable" to OcrTextNormalizer.isUsableForAnalyzer(rawText).toString(),
        )
    }

    private fun estimateConfidence(normalized: OcrTextNormalizer.NormalizedText): Float? {
        if (normalized.lines.isEmpty()) return 0f
        return if (OcrTextNormalizer.isUsableForAnalyzer(normalized.rawText)) 1f else 0.5f
    }

    companion object {
        const val ENGINE_ID = "mlkit"
    }
}
