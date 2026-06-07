package com.example.bowlingmaster200.domain.model

/**
 * 計算結果の1フレーム分。
 * [cumulativeScore] はそのフレーム終了時点の累計。未確定なら null。
 */
data class FrameScore(
    val frameIndex: Int,
    val firstRoll: Int?,
    val secondRoll: Int?,
    val bonusRoll: Int?,
    val frameType: FrameType,
    val framePins: Int?,
    val cumulativeScore: Int?,
) {
    init {
        require(frameIndex in 1..Frame.FRAME_COUNT) {
            "frameIndex must be 1..${Frame.FRAME_COUNT}, got $frameIndex"
        }
    }

    val isScored: Boolean get() = cumulativeScore != null
}
