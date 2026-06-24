package com.example.bowlingmaster200.ocr.service

import android.content.Context
import android.graphics.Bitmap
import com.example.bowlingmaster200.BuildConfig
import java.io.File

/**
 * DEBUG 専用: ML Kit 投入直前の Bitmap を JPEG 保存し、Logcat にパスを出力する。
 */
object OcrMlKitInputDebugSnapshot {

    const val FILE_NAME = "mlkit_input.jpg"

    fun snapshotFile(context: Context): File {
        val dir = context.getExternalFilesDir("ocr_debug")
            ?: File(context.cacheDir, "ocr_debug")
        return File(dir, FILE_NAME)
    }

    fun exists(context: Context): Boolean {
        if (!BuildConfig.DEBUG) return false
        val file = snapshotFile(context)
        return file.exists() && file.length() > 0L
    }

    fun save(context: Context, bitmap: Bitmap, rotationDegrees: Int) {
        if (!BuildConfig.DEBUG) return
        try {
            val file = snapshotFile(context)
            if (!file.parentFile.exists()) {
                file.parentFile.mkdirs()
            }
            file.outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)) {
                    "JPEG compress failed"
                }
            }
        } catch (error: Exception) {
            OcrLogger.e("Failed to save ML Kit input snapshot", error)
        }
    }
}
