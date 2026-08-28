package com.desafiomusical.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desafiomusical.app.di.AppContainer
import com.desafiomusical.app.domain.model.GameHistoryEntry
import com.desafiomusical.app.domain.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HistoryListUiState(
    val isLoading: Boolean = true,
    val entries: List<GameHistoryEntry> = emptyList(),
    val players: List<Player> = emptyList()
)

class HistoryListViewModel(container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryListUiState())
    val uiState: StateFlow<HistoryListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                container.gameHistoryRepository.observeHistory(),
                container.playerRepository.observeKnownPlayers()
            ) { entries, players -> HistoryListUiState(isLoading = false, entries = entries, players = players) }
                .collect { _uiState.value = it }
        }
    }
}
