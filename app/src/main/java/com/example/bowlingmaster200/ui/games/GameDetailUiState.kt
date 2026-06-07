package com.example.bowlingmaster200.ui.games

import com.example.bowlingmaster200.domain.model.SavedGameDetail

data class GameDetailUiState(
    val detail: SavedGameDetail? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isDeleting: Boolean = false,
    val deleteMessage: String? = null,
)
