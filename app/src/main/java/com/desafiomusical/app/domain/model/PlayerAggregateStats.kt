package com.desafiomusical.app.domain.model

/**
 * Estatísticas agregadas de um jogador ao longo de todas as partidas salvas
 * no histórico (seção 22 da especificação).
 *
 * [winRate] é uma fração de 0.0 a 1.0 (sempre 0.0 quando [gamesPlayed] é 0,
 * nunca divide por zero). [bestTimeSeconds] é `null` quando o jogador nunca
 * acertou uma rodada. [averagePoints] e [bestScore] são calculados sobre o
 * placar FINAL de cada partida — não sobre pontos de rodadas individuais
 * (esse já existe por partida em [PlayerGameStats.bestRoundPoints]).
 */
data class PlayerAggregateStats(
    val playerId: String,
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Double,
    val songsAnswered: Int,
    val correctAnswers: Int,
    val averagePoints: Double,
    val bestScore: Int,
    val bestTimeSeconds: Int?,
    val hintsUsed: Int,
    val stealsAttempted: Int,
    val stealsWon: Int,
    val categoryBreakdown: List<CategoryStats>
) {
    companion object {
        /** Estado neutro para um jogador sem nenhuma partida salva — nunca divide por zero. */
        fun empty(playerId: String): PlayerAggregateStats = PlayerAggregateStats(
            playerId = playerId,
            gamesPlayed = 0,
            wins = 0,
            losses = 0,
            winRate = 0.0,
            songsAnswered = 0,
            correctAnswers = 0,
            averagePoints = 0.0,
            bestScore = 0,
            bestTimeSeconds = null,
            hintsUsed = 0,
            stealsAttempted = 0,
            stealsWon = 0,
            categoryBreakdown = Category.concrete.map { CategoryStats(category = it, songsAnswered = 0, correctAnswers = 0) }
        )
    }
}

/** Quebra de [PlayerAggregateStats] por categoria (seção 22 da especificação). */
data class CategoryStats(
    val category: Category,
    val songsAnswered: Int,
    val correctAnswers: Int
)
