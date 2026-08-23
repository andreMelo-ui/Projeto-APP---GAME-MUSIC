package com.desafiomusical.app.domain.model

/**
 * Retrato resumido de uma partida encerrada, usado na lista de histórico
 * (seção 22 da especificação): data, jogadores, vencedor, placar final e
 * quantidade de rodadas — sem o detalhe rodada a rodada (ver [GameHistoryDetail]).
 */
data class GameHistoryEntry(
    val gameId: String,
    val playedAt: Long,
    val roundCount: Int,
    val scoreboard: List<PlayerScore>,
    val winner: Player?
)
