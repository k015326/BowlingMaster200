package com.example.bowlingmaster200.ocr.service

import android.content.Context
import com.example.bowlingmaster200.ocr.pipeline.OcrInput
import com.example.bowlingmaster200.ocr.pipeline.OcrLine
import com.example.bowlingmaster200.ocr.pipeline.OcrResult
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
        val inputImage = OcrInputImageConverter.toInputImage(context, input)
        val visionText = recognizeText(inputImage)
        return toOcrResult(input, visionText)
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
        val lines = visionText.textBlocks
            .flatMap { block -> block.lines }
            .mapIndexed { index, line ->
                OcrLine(
                    text = line.text,
                    confidence = null,
                    lineIndex = index,
                )
            }

        return OcrResult(
            rawText = visionText.text,
            lines = lines,
            confidence = null,
            engineId = engineId,
            debugInfo = mapOf(
                "engine" to "mlkit",
                "source" to input.metadata.sourceLabel.orEmpty(),
            ),
        )
    }

    companion object {
        const val ENGINE_ID = "mlkit"
    }
}
