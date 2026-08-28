package com.desafiomusical.app.domain.model

/** Retrato completo de uma partida encerrada: o resumo de [GameHistoryEntry] mais o detalhe rodada a rodada. */
data class GameHistoryDetail(
    val entry: GameHistoryEntry,
    val rounds: List<GameHistoryRound>,
)

/** Uma rodada de [GameHistoryDetail]: qual música caiu, quem venceu e quantos pontos valeu. */
data class GameHistoryRound(
    val roundNumber: Int,
    val song: Song,
    val winner: Player?,
    val pointsAwarded: Int,
    val elapsedSeconds: Int,
)
