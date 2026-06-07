package com.example.bowlingmaster200.ui.games

import com.example.bowlingmaster200.domain.model.GameMetadata

data class GamesHistoryUiState(
    val games: List<GameMetadata> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
