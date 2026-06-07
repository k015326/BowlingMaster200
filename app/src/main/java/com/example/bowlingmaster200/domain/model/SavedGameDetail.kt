package com.example.bowlingmaster200.domain.model

/**
 * 詳細表示用（メタデータ + 計算済みスコア）。
 */
data class SavedGameDetail(
    val metadata: GameMetadata,
    val gameScore: GameScore,
)
