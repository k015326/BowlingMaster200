package com.example.bowlingmaster200.ui.games

object GamesSubRoute {
    const val INPUT = "games_input"
    const val HISTORY = "games_history"
    const val DETAIL = "games_detail/{gameId}"

    fun detail(gameId: Long): String = "games_detail/$gameId"
}
