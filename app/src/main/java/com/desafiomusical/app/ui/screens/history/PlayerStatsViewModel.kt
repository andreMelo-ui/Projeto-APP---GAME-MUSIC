package com.desafiomusical.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desafiomusical.app.di.AppContainer
import com.desafiomusical.app.domain.model.PlayerAggregateStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** [stats] só é `null` enquanto carrega — [com.desafiomusical.app.data.repository.GameHistoryRepository.getPlayerStats] sempre retorna algo (zerado se o jogador nunca jogou). */
data class PlayerStatsUiState(
    val playerName: String = "",
    val stats: PlayerAggregateStats? = null
)

class PlayerStatsViewModel(container: AppContainer, playerId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerStatsUiState())
    val uiState: StateFlow<PlayerStatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val name = container.playerRepository.observeKnownPlayers().first()
                .firstOrNull { it.id == playerId }?.name.orEmpty()
            val stats = container.gameHistoryRepository.getPlayerStats(playerId)
            _uiState.value = PlayerStatsUiState(playerName = name, stats = stats)
        }
    }
}
