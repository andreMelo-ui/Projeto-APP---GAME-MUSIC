package com.desafiomusical.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desafiomusical.app.di.AppContainer
import com.desafiomusical.app.domain.model.GameHistoryDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** [detail] só é `null` depois de carregado ([isLoading] `false`) se [gameId] não existir mais no histórico. */
data class GameDetailUiState(
    val isLoading: Boolean = true,
    val detail: GameHistoryDetail? = null,
)

class GameDetailViewModel(container: AppContainer, gameId: String) : ViewModel() {
    private val _uiState = MutableStateFlow(GameDetailUiState())
    val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val detail = container.gameHistoryRepository.getGameDetail(gameId)
            _uiState.value = GameDetailUiState(isLoading = false, detail = detail)
        }
    }
}
