package com.example.bowlingmaster200.domain.model

/**
 * 入力用フレームモデル。
 * - 1〜9フレーム: [firstRoll] / [secondRoll]
 * - 10フレーム: [bonusRoll] まで使用可能
 *
 * null は「未入力」を表す。
 */
data class Frame(
    val firstRoll: Int? = null,
    val secondRoll: Int? = null,
    val bonusRoll: Int? = null,
) {
    fun withFirstRoll(pins: Int): Frame = copy(firstRoll = pins)
    fun withSecondRoll(pins: Int): Frame = copy(secondRoll = pins)
    fun withBonusRoll(pins: Int): Frame = copy(bonusRoll = pins)

    companion object {
        const val FRAME_COUNT = 10
        const val LAST_FRAME_INDEX = FRAME_COUNT - 1
    }
}
