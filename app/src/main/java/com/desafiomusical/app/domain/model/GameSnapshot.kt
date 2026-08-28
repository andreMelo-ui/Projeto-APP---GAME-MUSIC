package com.desafiomusical.app.domain.model

/** Retrato final de uma partida encerrada, pronto para ser persistido no histórico. */
data class GameSnapshot(
    val gameId: String,
    val config: GameConfig,
    val rounds: List<RoundOutcome>,
    val finalScoreboard: List<PlayerScore>,
    val winner: Player?,
)
