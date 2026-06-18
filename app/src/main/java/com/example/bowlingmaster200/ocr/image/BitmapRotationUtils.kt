package com.example.bowlingmaster200.ocr.image

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * SpareMaster [BitmapRotationUtils] 相当。
 * ML Kit 投入前に Bitmap を物理回転する（Step1）。
 */
object BitmapRotationUtils {

    fun rotate(source: Bitmap, degrees: Int): Bitmap {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return source
        val matrix = Matrix().apply { postRotate(normalized.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
