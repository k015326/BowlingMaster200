package com.example.bowlingmaster200.ocr.image

import android.graphics.Bitmap
import android.graphics.Color
import com.example.bowlingmaster200.ocr.service.OcrLogger

/**
 * 固定切り出し後の下方向余白を簡易判定で除去する（SpareMaster 移植）。
 */
internal object OcrDynamicBottomTrim {

    private const val BRIGHTNESS_THRESHOLD = 235
    private const val INK_RATIO_THRESHOLD = 0.015f
    private const val HORIZONTAL_SAMPLE_STEP = 8
    private const val BOTTOM_PADDING_PX = 8
    private const val MIN_HEIGHT_RATIO = 0.45f

    fun trimBottom(fixedCrop: Bitmap): Bitmap {
        val contentBottomY = detectContentBottomY(fixedCrop)
        val targetHeight = (contentBottomY + BOTTOM_PADDING_PX + 1).coerceAtMost(fixedCrop.height)
        val minHeight = (fixedCrop.height * MIN_HEIGHT_RATIO).toInt().coerceAtLeast(1)

        if (targetHeight >= fixedCrop.height || targetHeight < minHeight) {
            OcrLogger.d(
                "OcrDynamicBottomTrim skip height=${fixedCrop.height} " +
                    "contentBottom=$contentBottomY target=$targetHeight",
            )
            return fixedCrop
        }

        OcrLogger.d(
            "OcrDynamicBottomTrim height ${fixedCrop.height} -> $targetHeight " +
                "(contentBottom=$contentBottomY)",
        )
        return Bitmap.createBitmap(fixedCrop, 0, 0, fixedCrop.width, targetHeight)
    }

    private fun detectContentBottomY(bitmap: Bitmap): Int {
        val width = bitmap.width
        val height = bitmap.height
        if (height <= 1) return height - 1

        for (y in height - 1 downTo 0) {
            if (rowHasContent(bitmap, y, width)) {
                return y
            }
        }
        return height - 1
    }

    private fun rowHasContent(bitmap: Bitmap, y: Int, width: Int): Boolean {
        var darkCount = 0
        var sampleCount = 0
        var x = 0
        while (x < width) {
            val pixel = bitmap.getPixel(x, y)
            val brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
            if (brightness < BRIGHTNESS_THRESHOLD) {
                darkCount++
            }
            sampleCount++
            x += HORIZONTAL_SAMPLE_STEP
        }
        return darkCount.toFloat() / sampleCount >= INK_RATIO_THRESHOLD
    }
}
