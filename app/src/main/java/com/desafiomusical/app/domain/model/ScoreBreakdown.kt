package com.desafiomusical.app.domain.model

data class ScoreBreakdown(
    val playerId: String,
    val timePoints: Int,
    val titlePoints: Int,
    val artistPoints: Int,
    val workPoints: Int,
    val hintPenalty: Int,
) {
    val totalPoints: Int
        get() = timePoints + titlePoints + artistPoints + workPoints - hintPenalty
}
