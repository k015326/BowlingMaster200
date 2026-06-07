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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameDetailViewModel(
    application: Application,
    private val gameId: Long,
    private val repository: BowlingRepository = BowlingRepository.create(application),
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GameDetailUiState())
    val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val detail = repository.getGameDetail(gameId)
                if (detail == null) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "ゲームが見つかりません")
                    }
                } else {
                    _uiState.update {
                        it.copy(detail = detail, isLoading = false, errorMessage = null)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "読み込みに失敗しました")
                }
            }
        }
    }

    fun deleteGame(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteMessage = null) }
            try {
                repository.deleteGame(gameId)
                _uiState.update { it.copy(isDeleting = false, deleteMessage = "削除しました") }
                onDeleted()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = e.message ?: "削除に失敗しました",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(application: Application, gameId: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GameDetailViewModel(application, gameId) as T
                }
            }
        }
    }
}
