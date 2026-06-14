package com.example.bowlingmaster200.ocr.service

import android.content.Context
import com.example.bowlingmaster200.BuildConfig

/**
 * [OcrEngine] の生成。Fake / Real の切り替えはここだけ。
 */
object OcrServiceFactory {

    enum class EngineMode {
        FAKE,
        REAL,
    }

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun create(mode: EngineMode = EngineMode.REAL): OcrEngine {
        return when (mode) {
            EngineMode.FAKE -> createFakeEngine()
            EngineMode.REAL -> createRealEngine()
        }
    }

    private fun createFakeEngine(): OcrEngine {
        check(BuildConfig.DEBUG) { "Fake OCR is available in debug builds only" }
        return FakeOcrService()
    }

    private fun createRealEngine(): OcrEngine {
        val context = checkNotNull(appContext) {
            "Call OcrServiceFactory.init(context) before using EngineMode.REAL"
        }
        return FallbackOcrService(
            primary = MlKitOcrService(context),
            fallback = createFallbackEngine(),
        )
    }

    private fun createFallbackEngine(): OcrEngine {
        return if (BuildConfig.DEBUG) {
            FakeOcrService()
        } else {
            EmptyOcrEngine()
        }
    }
}
