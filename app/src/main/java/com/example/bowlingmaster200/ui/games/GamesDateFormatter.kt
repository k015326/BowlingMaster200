package com.example.bowlingmaster200.ui.games

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object GamesDateFormatter {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun format(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }
}
