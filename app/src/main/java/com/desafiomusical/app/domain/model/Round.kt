package com.desafiomusical.app.domain.model

data class RoundSetup(
    val roundNumber: Int,
    val chooserId: String,
    val mainResponderId: String,
    val eligibleStealerIds: List<String>,
)

data class RoundOutcome(
    val roundNumber: Int,
    val chooserId: String,
    val mainResponderId: String,
    val songId: String,
    val winnerId: String?,
    val startedAt: Long,
    val endedAt: Long,
    val scores: List<ScoreBreakdown>,
    val hintsUsed: Int,
    val eliminatedIds: List<String>,
)
