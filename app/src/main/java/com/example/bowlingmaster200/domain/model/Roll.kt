package com.example.bowlingmaster200.domain.model

/**
 * 1投分の倒ピン数（0〜10）。
 * Gutter は [MIN_PINS]、Strike 投球は [MAX_PINS]。
 */
@JvmInline
value class Roll(val pins: Int) {

    val isStrike: Boolean get() = pins == MAX_PINS
    val isGutter: Boolean get() = pins == MIN_PINS
    val isSpareContribution: Boolean get() = pins in MIN_PINS..MAX_PINS

    init {
        require(pins in MIN_PINS..MAX_PINS) {
            "Roll must be between $MIN_PINS and $MAX_PINS, got $pins"
        }
    }

    companion object {
        const val MIN_PINS = 0
        const val MAX_PINS = 10
        const val FRAME_PINS = 10

        fun gutter(): Roll = Roll(MIN_PINS)
        fun strike(): Roll = Roll(MAX_PINS)
    }
}
