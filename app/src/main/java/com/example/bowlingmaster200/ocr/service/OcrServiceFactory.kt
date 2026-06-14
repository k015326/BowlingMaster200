package com.example.bowlingmaster200.ocr.service

import android.content.Context

/**
 * [OcrEngine] の生成。Fake / Real の切り替えはここだけ。
 *
 * 本番 OCR へ切り替え: [create] の `mode` を [EngineMode.REAL] に変更する。
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

    fun create(mode: EngineMode = EngineMode.FAKE): OcrEngine {
        return when (mode) {
            EngineMode.FAKE -> FakeOcrService()
            EngineMode.REAL -> createRealEngine()
        }
    }

    private fun createRealEngine(): OcrEngine {
        val context = checkNotNull(appContext) {
            "Call OcrServiceFactory.init(context) before using EngineMode.REAL"
        }
        return FallbackOcrService(
            primary = MlKitOcrService(context),
            fallback = FakeOcrService(),
        )
    }
}
