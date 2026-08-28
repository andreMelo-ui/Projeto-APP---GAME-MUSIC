package com.desafiomusical.app.domain.model

data class PlayerScore(
    val player: Player,
    val totalScore: Int,
    val role: PlayerRole? = null,
)

data class PlayerGameStats(
    val player: Player,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val averagePoints: Double,
    val bestRoundPoints: Int,
    val hintsUsed: Int,
    val stealsAttempted: Int,
    val stealsWon: Int,
)
