package com.example.bowlingmaster200.ui.games

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bowlingmaster200.data.repository.BowlingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GamesHistoryViewModel(
    application: Application,
    private val repository: BowlingRepository = BowlingRepository.create(application),
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GamesHistoryUiState())
    val uiState: StateFlow<GamesHistoryUiState> = _uiState.asStateFlow()

    init {
        observeGames()
    }

    private fun observeGames() {
        viewModelScope.launch {
            repository.observeAllGames()
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "読み込みに失敗しました")
                    }
                }
                .collect { games ->
                    _uiState.update {
                        it.copy(games = games, isLoading = false, errorMessage = null)
                    }
                }
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GamesHistoryViewModel(application) as T
                }
            }
        }
    }
}
