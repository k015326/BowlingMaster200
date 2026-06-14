package com.example.bowlingmaster200.ui.camera

/**
 * スコア表示用の1フレーム（UI専用）。
 */
data class OcrFrameDisplay(
    val frameIndex: Int,
    val roll1: String,
    val roll2: String,
    val roll3: String?,
    val cumulative: String,
)

fun formatOcrRoll(
    value: Int?,
    isSecondRoll: Boolean = false,
    firstRoll: Int? = null,
): String {
    if (value == null) return "·"
    if (!isSecondRoll && value == 10) return "X"
    if (value == 0) return "-"
    if (isSecondRoll && firstRoll != null && firstRoll != 10 && firstRoll + value == 10) {
        return "/"
    }
    return value.toString()
}
