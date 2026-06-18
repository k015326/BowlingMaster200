package com.example.bowlingmaster200.ocr.image

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.bowlingmaster200.ocr.service.OcrLogger

internal object OcrRegionCrop {

    fun crop(source: Bitmap, rect: Rect): Bitmap? {
        val width = rect.width()
        val height = rect.height()
        if (width <= 0 || height <= 0) {
            OcrLogger.d("OcrRegionCrop invalid rect=$rect")
            return null
        }
        return try {
            Bitmap.createBitmap(source, rect.left, rect.top, width, height)
        } catch (error: Exception) {
            OcrLogger.e("OcrRegionCrop failed rect=$rect", error)
            null
        }
    }
}
