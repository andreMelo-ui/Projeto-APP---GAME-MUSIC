package com.desafiomusical.app.domain.state

import com.desafiomusical.app.domain.model.Category
import com.desafiomusical.app.domain.model.GameConfig
import com.desafiomusical.app.domain.model.MaskedSong
import com.desafiomusical.app.domain.model.Player
import com.desafiomusical.app.domain.model.PlayerGameStats
import com.desafiomusical.app.domain.model.PlayerScore
import com.desafiomusical.app.domain.model.Song

/**
 * Visão pública de uma rodada em andamento — é o único formato de dados de
 * rodada que trafega para o respondente principal e demais jogadores.
 * Nunca contém título, artista ou obra.
 */
data class ActiveRoundView(
    val roundNumber: Int,
    val totalRounds: Int,
    val chooser: Player,
    val mainResponder: Player,
    val maskedSong: MaskedSong,
    val eligibleStealers: List<Player>,
    val eliminatedIds: Set<String>,
    val hintsRevealedCount: Int,
    val pointsAvailableNow: Int,
)

/** Visão privada, exclusiva do escolhedor: contém a resposta completa. */
data class ChooserRoundView(
    val roundNumber: Int,
    val totalRounds: Int,
    val song: Song,
    val mainResponder: Player,
)

data class RoundResultView(
    val roundNumber: Int,
    val totalRounds: Int,
    val song: Song,
    val winner: Player?,
    val pointsAwarded: Int,
    val elapsedSeconds: Int,
    val scoreboard: List<PlayerScore>,
)

data class GameResultView(
    val finalScoreboard: List<PlayerScore>,
    val winner: Player?,
    val stats: List<PlayerGameStats>,
)

sealed class GameUiState {
    data object Lobby : GameUiState()

    data class PlayerSetup(val existingConfig: GameConfig? = null) : GameUiState()

    data class CategorySelection(
        val chooser: Player,
        val roundNumber: Int,
        val totalRounds: Int,
        val categories: List<Category> = Category.selectable,
    ) : GameUiState()

    data class SongSelection(
        val chooser: Player,
        val roundNumber: Int,
        val totalRounds: Int,
        val category: Category,
        val candidates: List<Song>,
    ) : GameUiState()

    data class Ready(val chooserView: ChooserRoundView) : GameUiState()

    data class Playing(val round: ActiveRoundView, val elapsedSeconds: Int) : GameUiState()

    /**
     * Tela de confirmação — quem julga (no modo um celular, normalmente o
     * escolhedor) precisa ver a resposta real, por isso [song] aqui não é
     * mascarado. É o único ponto do fluxo onde isso acontece fora da tela
     * do escolhedor.
     */
    data class Answering(
        val round: ActiveRoundView,
        val song: Song,
        val elapsedSeconds: Int,
        val titleClaimed: Boolean,
        val artistClaimed: Boolean,
        val workClaimed: Boolean = false,
    ) : GameUiState()

    data class StealWindow(
        val round: ActiveRoundView,
        val elapsedSeconds: Int,
        val stealSecondsLeft: Int,
    ) : GameUiState()

    data class StealAnswer(
        val round: ActiveRoundView,
        val song: Song,
        val elapsedSeconds: Int,
        val stealerId: String,
    ) : GameUiState()

    data class RoundResult(val result: RoundResultView) : GameUiState()

    data class GameResult(val result: GameResultView) : GameUiState()
}
