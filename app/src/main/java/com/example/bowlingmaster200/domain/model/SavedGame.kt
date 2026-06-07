package com.example.bowlingmaster200.domain.model

/**
 * 保存用ゲーム（メタデータ + 入力フレーム）。
 */
data class SavedGame(
    val metadata: GameMetadata,
    val frames: List<Frame>,
) {
    init {
        require(frames.size == Frame.FRAME_COUNT) {
            "frames must contain ${Frame.FRAME_COUNT} entries, got ${frames.size}"
        }
    }
}
