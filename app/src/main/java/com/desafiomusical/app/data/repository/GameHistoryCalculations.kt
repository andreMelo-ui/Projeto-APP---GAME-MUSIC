package com.desafiomusical.app.data.repository

import com.desafiomusical.app.data.mapper.toDomain
import com.desafiomusical.app.data.room.entity.AttemptEntity
import com.desafiomusical.app.data.room.entity.GameEntity
import com.desafiomusical.app.data.room.entity.GamePlayerEntity
import com.desafiomusical.app.data.room.entity.PlayerEntity
import com.desafiomusical.app.data.room.entity.RoundEntity
import com.desafiomusical.app.data.room.entity.RoundScoreEntity
import com.desafiomusical.app.data.room.entity.SongEntity
import com.desafiomusical.app.domain.model.Category
import com.desafiomusical.app.domain.model.CategoryStats
import com.desafiomusical.app.domain.model.GameHistoryDetail
import com.desafiomusical.app.domain.model.GameHistoryEntry
import com.desafiomusical.app.domain.model.GameHistoryRound
import com.desafiomusical.app.domain.model.PlayerAggregateStats
import com.desafiomusical.app.domain.model.PlayerScore

/**
 * Funções puras (sem I/O) que transformam linhas do Room em modelos de
 * domínio de histórico/estatísticas. Ficam separadas de [GameHistoryRepositoryImpl]
 * de propósito, para serem testadas com fixtures simples em JUnit puro, sem
 * precisar de um banco Room real em memória.
 */

internal fun buildGameHistoryEntry(
    game: GameEntity,
    gamePlayers: List<GamePlayerEntity>,
    playersById: Map<String, PlayerEntity>,
): GameHistoryEntry {
    val scoreboard =
        gamePlayers
            .sortedByDescending { it.finalScore }
            .mapNotNull { gp -> playersById[gp.playerId]?.let { PlayerScore(player = it.toDomain(), totalScore = gp.finalScore) } }
    return GameHistoryEntry(
        gameId = game.id,
        playedAt = game.createdAt,
        roundCount = game.roundCount,
        scoreboard = scoreboard,
        winner = game.winnerId?.let { playersById[it]?.toDomain() },
    )
}

internal fun buildGameHistoryDetail(
    entry: GameHistoryEntry,
    rounds: List<RoundEntity>,
    songsById: Map<String, SongEntity>,
    playersById: Map<String, PlayerEntity>,
    scoresByRoundId: Map<String, RoundScoreEntity>,
): GameHistoryDetail {
    val roundEntries =
        rounds.sortedBy { it.roundNumber }.mapNotNull { round ->
            val song = songsById[round.songId]?.toDomain() ?: return@mapNotNull null
            GameHistoryRound(
                roundNumber = round.roundNumber,
                song = song,
                winner = round.winnerId?.let { playersById[it]?.toDomain() },
                pointsAwarded = scoresByRoundId[round.id]?.totalPoints ?: 0,
                elapsedSeconds = round.endedAt?.let { ((it - round.startedAt) / 1000).toInt() } ?: 0,
            )
        }
    return GameHistoryDetail(entry = entry, rounds = roundEntries)
}

/**
 * Agrega o histórico completo de um jogador (todas as partidas em que
 * participou) nas estatísticas da seção 22 da especificação.
 *
 * [RoundScoreEntity] só guarda uma linha por rodada — a de quem venceu (ver
 * [com.desafiomusical.app.data.mapper.toRoundScoreEntities]) — então "acertos"
 * e "roubo vencido" são derivados diretamente de [RoundEntity.winnerId] e
 * [RoundEntity.responderId], sem precisar ler round_scores. "Roubo tentado"
 * (incluindo os perdidos) soma os vencidos a quem aparece em [attempts] numa
 * rodada em que a pessoa NÃO era a respondente principal — só quem não é
 * escolhedor/respondente pode tentar roubar (ver
 * [com.desafiomusical.app.domain.usecase.DistributeRolesUseCase]), então esse
 * filtro isola só as tentativas de roubo, mesmo as perdidas.
 */
internal fun calculatePlayerAggregateStats(
    playerId: String,
    games: List<GameEntity>,
    gamePlayerRows: List<GamePlayerEntity>,
    rounds: List<RoundEntity>,
    attempts: List<AttemptEntity>,
    songsById: Map<String, SongEntity>,
): PlayerAggregateStats {
    if (games.isEmpty()) return PlayerAggregateStats.empty(playerId)

    val gamesPlayed = games.size
    val wins = games.count { it.winnerId == playerId }
    val losses = gamesPlayed - wins
    val winRate = wins.toDouble() / gamesPlayed

    val finalScores = gamePlayerRows.map { it.finalScore }
    val averagePoints = if (finalScores.isEmpty()) 0.0 else finalScores.average()
    val bestScore = finalScores.maxOrNull() ?: 0

    val roundsById = rounds.associateBy { it.id }
    val respondedRounds = rounds.filter { it.responderId == playerId }
    val stealWinRounds = rounds.filter { it.winnerId == playerId && it.responderId != playerId }
    val stealLossAttempts =
        attempts.filter { attempt ->
            val round = roundsById[attempt.roundId]
            round != null && round.responderId != playerId
        }

    val stealsWon = stealWinRounds.size
    val stealsAttempted = stealsWon + stealLossAttempts.size
    val correctAnswers = rounds.count { it.winnerId == playerId }
    val songsAnswered = respondedRounds.size + stealsAttempted
    val hintsUsed = respondedRounds.sumOf { it.hintsUsed }
    val bestTimeSeconds =
        rounds
            .filter { it.winnerId == playerId && it.endedAt != null }
            .minOfOrNull { ((it.endedAt!! - it.startedAt) / 1000).toInt() }

    val categoryBreakdown =
        Category.concrete.map { category ->
            val categoryRoundIds =
                rounds
                    .filter { songsById[it.songId]?.category == category.name }
                    .mapTo(mutableSetOf()) { it.id }
            val categorySongsAnswered =
                respondedRounds.count { it.id in categoryRoundIds } +
                    stealWinRounds.count { it.id in categoryRoundIds } +
                    stealLossAttempts.count { roundsById[it.roundId]?.id in categoryRoundIds }
            val categoryCorrect = rounds.count { it.winnerId == playerId && it.id in categoryRoundIds }
            CategoryStats(category = category, songsAnswered = categorySongsAnswered, correctAnswers = categoryCorrect)
        }

    return PlayerAggregateStats(
        playerId = playerId,
        gamesPlayed = gamesPlayed,
        wins = wins,
        losses = losses,
        winRate = winRate,
        songsAnswered = songsAnswered,
        correctAnswers = correctAnswers,
        averagePoints = averagePoints,
        bestScore = bestScore,
        bestTimeSeconds = bestTimeSeconds,
        hintsUsed = hintsUsed,
        stealsAttempted = stealsAttempted,
        stealsWon = stealsWon,
        categoryBreakdown = categoryBreakdown,
    )
}
