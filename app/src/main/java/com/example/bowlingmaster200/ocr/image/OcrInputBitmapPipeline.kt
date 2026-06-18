package com.example.bowlingmaster200.ocr.image

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.bowlingmaster200.ocr.service.OcrLogger

/**
 * 回転済み Bitmap → ガイド切り出し → 下端トリム → 1280px 正規化（SpareMaster Step2 移植）。
 */
object OcrInputBitmapPipeline {

    private const val TARGET_OCR_WIDTH_PX = 1280
    private const val OCR_BOTTOM_EXTEND_IMAGE_RATIO = 0.022f
    private const val OCR_LEFT_EXTEND_IMAGE_RATIO = 0.09f
    private const val OCR_RIGHT_EXTEND_IMAGE_RATIO = 0.13f

    data class DebugInfo(
        val orientedWidth: Int,
        val orientedHeight: Int,
        val guideRect: Rect,
        val ocrRect: Rect,
        val trimBottomPx: Int,
    )

    data class PreparedResult(
        val bitmap: Bitmap,
        val debugInfo: DebugInfo,
    )

    fun prepareFromOrientedBitmap(oriented: Bitmap): PreparedResult? {
        val guideRect = CaptureGuideFrame.toPixelRect(oriented.width, oriented.height)
        val leftExt = (oriented.width * OCR_LEFT_EXTEND_IMAGE_RATIO).toInt()
        val rightExt = (oriented.width * OCR_RIGHT_EXTEND_IMAGE_RATIO).toInt()
        val bottomExt = (oriented.height * OCR_BOTTOM_EXTEND_IMAGE_RATIO).toInt()

        val extendedLeft = (guideRect.left - leftExt).coerceAtLeast(0)
        val extendedRight = (guideRect.right + rightExt).coerceAtMost(oriented.width)
        val extendedBottom = (guideRect.bottom + bottomExt).coerceAtMost(oriented.height)
        val ocrRect = Rect(extendedLeft, guideRect.top, extendedRight, extendedBottom)

        val cropped = OcrRegionCrop.crop(oriented, ocrRect) ?: return null
        val trimmed = OcrDynamicBottomTrim.trimBottom(cropped)
        val trimBottomPx = (cropped.height - trimmed.height).coerceAtLeast(0)
        if (trimmed !== cropped) {
            cropped.recycle()
        }
        val normalized = normalizeWidthForOcr(trimmed)
        if (normalized !== trimmed) {
            trimmed.recycle()
        }

        OcrLogger.d(
            "OcrInputBitmapPipeline prepared bitmap=${normalized.width}x${normalized.height} " +
                "guide=$guideRect ocrRect=$ocrRect trimBottom=$trimBottomPx",
        )

        return PreparedResult(
            bitmap = normalized,
            debugInfo = DebugInfo(
                orientedWidth = oriented.width,
                orientedHeight = oriented.height,
                guideRect = guideRect,
                ocrRect = ocrRect,
                trimBottomPx = trimBottomPx,
            ),
        )
    }

    private fun normalizeWidthForOcr(bitmap: Bitmap): Bitmap {
        if (bitmap.width == TARGET_OCR_WIDTH_PX) return bitmap
        val scale = TARGET_OCR_WIDTH_PX.toFloat() / bitmap.width
        val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, TARGET_OCR_WIDTH_PX, targetH, true)
    }
}
