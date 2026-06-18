package com.example.bowlingmaster200.ocr.analyzer

import com.example.bowlingmaster200.domain.model.SavedGame

/**
 * OCR テキスト解析結果（Analyzer 出力）。
 * [BowlingScoreSheetAnalyzer] が [OcrResult] から生成する。
 */
data class ScoreSheetAnalysisResult(
    val savedGame: SavedGame,
    val confidence: Float,
    val warnings: List<String> = emptyList(),
    val engineId: String,
)
