package com.desafiomusical.app.data.mapper

import com.desafiomusical.app.data.room.entity.AttemptEntity
import com.desafiomusical.app.data.room.entity.GameEntity
import com.desafiomusical.app.data.room.entity.GamePlayerEntity
import com.desafiomusical.app.data.room.entity.RoundEntity
import com.desafiomusical.app.data.room.entity.RoundScoreEntity
import com.desafiomusical.app.domain.model.GameSnapshot

fun GameSnapshot.toGameEntity(): GameEntity = GameEntity(
    id = gameId,
    createdAt = rounds.minOfOrNull { it.startedAt } ?: System.currentTimeMillis(),
    playerCount = config.players.size,
    roundCount = config.roundCount,
    stealEnabled = config.stealEnabled,
    winnerId = winner?.id,
    finishedAt = rounds.maxOfOrNull { it.endedAt } ?: System.currentTimeMillis()
)

fun GameSnapshot.toGamePlayerEntities(): List<GamePlayerEntity> = config.players.mapIndexed { index, player ->
    GamePlayerEntity(
        gameId = gameId,
        playerId = player.id,
        seatOrder = index,
        finalScore = finalScoreboard.firstOrNull { it.player.id == player.id }?.totalScore ?: 0
    )
}

fun GameSnapshot.toRoundEntities(): List<RoundEntity> = rounds.map { outcome ->
    RoundEntity(
        id = "$gameId-round-${outcome.roundNumber}",
        gameId = gameId,
        roundNumber = outcome.roundNumber,
        chooserId = outcome.chooserId,
        responderId = outcome.mainResponderId,
        songId = outcome.songId,
        startedAt = outcome.startedAt,
        endedAt = outcome.endedAt,
        winnerId = outcome.winnerId,
        hintsUsed = outcome.hintsUsed
    )
}

fun GameSnapshot.toRoundScoreEntities(): List<RoundScoreEntity> = rounds.flatMap { outcome ->
    val roundId = "$gameId-round-${outcome.roundNumber}"
    outcome.scores.map { breakdown ->
        RoundScoreEntity(
            roundId = roundId,
            playerId = breakdown.playerId,
            timePoints = breakdown.timePoints,
            titlePoints = breakdown.titlePoints,
            artistPoints = breakdown.artistPoints,
            workPoints = breakdown.workPoints,
            hintPenalty = breakdown.hintPenalty,
            totalPoints = breakdown.totalPoints
        )
    }
}

fun GameSnapshot.toAttemptEntities(): List<AttemptEntity> = rounds.flatMap { outcome ->
    val roundId = "$gameId-round-${outcome.roundNumber}"
    outcome.eliminatedIds.mapIndexed { index, playerId ->
        AttemptEntity(
            id = "$roundId-eliminated-$index",
            roundId = roundId,
            playerId = playerId,
            attemptType = "STEAL_ANSWER",
            timestamp = outcome.endedAt,
            correct = false,
            eliminated = true
        )
    }
}
