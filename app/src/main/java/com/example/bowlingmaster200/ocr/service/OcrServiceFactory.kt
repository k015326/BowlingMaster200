package com.example.bowlingmaster200.ocr.service

/**
 * [OcrEngine] の生成。Fake / Real の切り替えはここだけ。
 */
object OcrServiceFactory {

    enum class EngineMode {
        FAKE,
        REAL,
    }

    fun create(mode: EngineMode = EngineMode.FAKE): OcrEngine {
        return when (mode) {
            EngineMode.FAKE -> FakeOcrService()
            EngineMode.REAL -> createRealEngine()
        }
    }

    private fun createRealEngine(): OcrEngine {
        error(
            "Implement OcrEngine and register it in OcrServiceFactory.createRealEngine().",
        )
    }
}
