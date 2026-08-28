package com.desafiomusical.app.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desafiomusical.app.di.AppContainer
import com.desafiomusical.app.domain.GameEngine
import com.desafiomusical.app.domain.model.Category
import com.desafiomusical.app.domain.model.Song
import com.desafiomusical.app.domain.state.GameUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel(private val container: AppContainer) : ViewModel() {
    private val engine: GameEngine = container.gameEngine

    val uiState: StateFlow<GameUiState> = engine.uiState
    val elapsedSeconds: StateFlow<Int> = engine.elapsedSeconds
    val stealSecondsLeft: StateFlow<Int> = engine.stealSecondsLeft

    private var historySaved = false

    fun selectCategory(category: Category) = engine.selectCategory(category)

    fun selectSong(song: Song) = engine.selectSongForRound(song)

    fun selectRandomSong() = engine.selectRandomSongForRound()

    fun startPlayback() = engine.startPlayback()

    fun requestHint() = engine.requestHint()

    fun submitMainAnswer(
        titleClaimed: Boolean,
        artistClaimed: Boolean,
        workClaimed: Boolean,
    ) = engine.submitMainAnswer(titleClaimed, artistClaimed, workClaimed)

    fun confirmMainAnswer(
        titleCorrect: Boolean,
        artistCorrect: Boolean,
        workCorrect: Boolean,
    ) = engine.confirmMainAnswer(titleCorrect, artistCorrect, workCorrect)

    fun claimSteal(playerId: String) = engine.claimSteal(playerId)

    fun confirmStealAnswer(
        titleCorrect: Boolean,
        artistCorrect: Boolean,
        workCorrect: Boolean,
    ) = engine.confirmStealAnswer(titleCorrect, artistCorrect, workCorrect)

    fun goToNextRound() = engine.goToNextRound()

    /** Persiste o histórico assim que a partida chega ao placar final (uma única vez). */
    fun persistHistoryIfNeeded() {
        if (historySaved) return
        val snapshot = engine.snapshotForPersistence() ?: return
        historySaved = true
        viewModelScope.launch {
            container.gameHistoryRepository.saveFinishedGame(snapshot)
        }
    }
}
