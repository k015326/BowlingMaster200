package com.example.bowlingmaster200.domain.model

/**
 * 永続化ゲームのメタデータ（UI・Repository 共通）。
 * 将来 OCR 由来のゲームも同一構造で保存可能。
 */
data class GameMetadata(
    val id: Long = 0L,
    val playedAt: Long,
    val location: String? = null,
    val laneNumber: Int? = null,
    val ballName: String? = null,
    val totalScore: Int? = null,
    val createdAt: Long,
)
