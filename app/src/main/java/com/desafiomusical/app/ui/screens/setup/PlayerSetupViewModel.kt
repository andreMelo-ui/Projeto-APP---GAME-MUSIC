package com.desafiomusical.app.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desafiomusical.app.di.AppContainer
import com.desafiomusical.app.domain.model.GameConfig
import com.desafiomusical.app.domain.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class PlayerSetupUiState(
    val playerCount: Int = 2,
    val playerNames: List<String> = List(MAX_PLAYERS) { "" },
    val roundCount: Int = 10,
    val stealEnabled: Boolean = true,
    val isStarting: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        const val MAX_PLAYERS = 4
    }
}

class PlayerSetupViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerSetupUiState())
    val uiState: StateFlow<PlayerSetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedNames = container.appPreferences.lastPlayerNames.first()
            val stealDefault = container.appPreferences.stealEnabledDefault.first()
            _uiState.update { state ->
                state.copy(
                    playerNames = List(PlayerSetupUiState.MAX_PLAYERS) { index -> savedNames.getOrNull(index).orEmpty() },
                    stealEnabled = stealDefault
                )
            }
        }
    }

    fun setPlayerCount(count: Int) {
        _uiState.update { it.copy(playerCount = count, errorMessage = null) }
    }

    fun setPlayerName(index: Int, name: String) {
        _uiState.update { state ->
            val updated = state.playerNames.toMutableList().apply { this[index] = name }
            state.copy(playerNames = updated, errorMessage = null)
        }
    }

    fun setRoundCount(count: Int) {
        _uiState.update { it.copy(roundCount = count) }
    }

    fun setStealEnabled(enabled: Boolean) {
        _uiState.update { it.copy(stealEnabled = enabled) }
    }

    fun startGame(onStarted: () -> Unit) {
        val state = _uiState.value
        val names = state.playerNames.take(state.playerCount).map { it.trim() }
        if (names.any { it.isEmpty() }) {
            _uiState.update { it.copy(errorMessage = "Preencha o nome de todos os jogadores.") }
            return
        }
        if (names.toSet().size != names.size) {
            _uiState.update { it.copy(errorMessage = "Os nomes dos jogadores devem ser diferentes.") }
            return
        }

        _uiState.update { it.copy(isStarting = true, errorMessage = null) }
        viewModelScope.launch {
            val catalog = container.songRepository.getCatalog()
            if (catalog.isEmpty()) {
                _uiState.update {
                    it.copy(isStarting = false, errorMessage = "Catálogo de músicas ainda não carregado. Tente novamente.")
                }
                return@launch
            }

            val players = names.map { name ->
                Player(id = UUID.randomUUID().toString(), name = name, createdAt = System.currentTimeMillis())
            }
            val config = GameConfig(players = players, roundCount = state.roundCount, stealEnabled = state.stealEnabled)

            container.gameEngine.setCatalog(catalog)
            container.gameEngine.startGame(config)
            container.playerRepository.saveIfNew(players)
            container.appPreferences.saveLastSetup(names, state.stealEnabled)

            _uiState.update { it.copy(isStarting = false) }
            onStarted()
        }
    }
}
