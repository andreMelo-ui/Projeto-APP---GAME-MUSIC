package com.desafiomusical.app.domain.usecase

import com.desafiomusical.app.domain.model.PlayerScore

/**
 * Detecta empate entre os jogadores líderes ao final da partida. A resolução
 * (morte súbita) reaproveita o fluxo normal de rodada restrito aos jogadores
 * retornados aqui — orquestrada pelo GameEngine.
 */
class TieBreakUseCase {
    operator fun invoke(scoreboard: List<PlayerScore>): List<PlayerScore> {
        if (scoreboard.isEmpty()) return emptyList()
        val topScore = scoreboard.maxOf { it.totalScore }
        val leaders = scoreboard.filter { it.totalScore == topScore }
        return if (leaders.size > 1) leaders else emptyList()
    }
}
