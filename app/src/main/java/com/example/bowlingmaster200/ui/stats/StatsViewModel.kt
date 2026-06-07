package com.example.bowlingmaster200.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bowlingmaster200.data.repository.BowlingRepository
import com.example.bowlingmaster200.domain.statistics.toStatisticsSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StatsViewModel(
    application: Application,
    private val repository: BowlingRepository = BowlingRepository.create(application),
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        observeStatistics()
    }

    private fun observeStatistics() {
        viewModelScope.launch {
            repository.observeAllGames()
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "統計の読み込みに失敗しました",
                        )
                    }
                }
                .collect { games ->
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    try {
                        val details = games.mapNotNull { metadata ->
                            repository.getGameDetail(metadata.id)
                        }
                        val summary = details.toStatisticsSummary()
                        _uiState.update {
                            it.copy(summary = summary, isLoading = false, errorMessage = null)
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = e.message ?: "統計の生成に失敗しました",
                            )
                        }
                    }
                }
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StatsViewModel(application) as T
                }
            }
        }
    }
}
