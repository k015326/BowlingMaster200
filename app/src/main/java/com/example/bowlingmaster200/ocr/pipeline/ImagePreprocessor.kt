package com.example.bowlingmaster200.ocr.pipeline

/**
 * OCR 前処理の抽象。
 * 将来 SpareMaster の crop / grayscale / threshold / perspective correction を
 * 個別 Preprocessor として追加可能。
 */
interface ImagePreprocessor {

    val preprocessorId: String

    fun preprocess(input: OcrInput): OcrInput
}

/** 前処理なし（今回のデフォルト）。 */
object NoOpImagePreprocessor : ImagePreprocessor {

    override val preprocessorId: String = "noop"

    override fun preprocess(input: OcrInput): OcrInput = input
}

/**
 * 複数前処理を順番に適用。
 * SpareMaster の OcrInputBitmapPipeline 移植時はここにチェーン追加。
 */
class ChainedImagePreprocessor(
    override val preprocessorId: String,
    private val preprocessors: List<ImagePreprocessor>,
) : ImagePreprocessor {

    override fun preprocess(input: OcrInput): OcrInput {
        return preprocessors.fold(input) { current, step -> step.preprocess(current) }
    }
}

// --- 将来追加用スタブ ---

/** @see ImagePreprocessor crop 実装予定 */
interface CropPreprocessor : ImagePreprocessor

/** @see ImagePreprocessor grayscale 実装予定 */
interface GrayscalePreprocessor : ImagePreprocessor

/** @see ImagePreprocessor threshold 実装予定 */
interface ThresholdPreprocessor : ImagePreprocessor

/** @see ImagePreprocessor perspective correction 実装予定 */
interface PerspectiveCorrectionPreprocessor : ImagePreprocessor
