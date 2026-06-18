package com.example.bowlingmaster200.ocr.image

import android.graphics.Rect

/**
 * 1ゲーム単位撮影ガイド枠（正規化座標 0.0〜1.0）。
 * SpareMaster [CaptureGuideFrame] から OCR 切り出し用定数のみ移植。
 */
object CaptureGuideFrame {

    const val GUIDE_LEFT = 0.02f
    const val GUIDE_TOP = 0.425f
    const val GUIDE_RIGHT = 0.99f
    const val GUIDE_BOTTOM = 0.60f

    fun toPixelRect(width: Int, height: Int): Rect {
        val left = (width * GUIDE_LEFT).toInt().coerceIn(0, width - 1)
        val top = (height * GUIDE_TOP).toInt().coerceIn(0, height - 1)
        val right = (width * GUIDE_RIGHT).toInt().coerceIn(left + 1, width)
        val bottom = (height * GUIDE_BOTTOM).toInt().coerceIn(top + 1, height)
        return Rect(left, top, right, bottom)
    }
}
