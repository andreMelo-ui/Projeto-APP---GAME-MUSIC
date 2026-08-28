package com.desafiomusical.app.domain.usecase

import com.desafiomusical.app.domain.model.ScoreBreakdown

object ScoringRules {
    const val TIME_TIER_1_MAX_SECONDS = 30
    const val TIME_TIER_2_MAX_SECONDS = 60
    const val ROUND_DURATION_SECONDS = 90
    const val STEAL_WINDOW_SECONDS = 5
    const val MAX_HINTS = 3

    const val TIME_TIER_1_POINTS = 10
    const val TIME_TIER_2_POINTS = 5
    const val TIME_TIER_3_POINTS = 0

    const val TITLE_POINTS = 5
    const val ARTIST_POINTS = 5
    const val WORK_POINTS = 5

    const val MAX_HINT_PENALTY = 6
}

/**
 * Cálculo de pontuação determinístico. Não depende de estado mutável nem de
 * relógio de parede — recebe o tempo decorrido já calculado pelo host.
 */
class CalculateScoreUseCase {
    operator fun invoke(
        playerId: String,
        elapsedSeconds: Int,
        titleCorrect: Boolean,
        artistCorrect: Boolean,
        workCorrect: Boolean = false,
        hintsUsed: Int,
    ): ScoreBreakdown =
        ScoreBreakdown(
            playerId = playerId,
            timePoints = timePointsFor(elapsedSeconds),
            titlePoints = if (titleCorrect) ScoringRules.TITLE_POINTS else 0,
            artistPoints = if (artistCorrect) ScoringRules.ARTIST_POINTS else 0,
            workPoints = if (workCorrect) ScoringRules.WORK_POINTS else 0,
            hintPenalty = hintPenaltyFor(hintsUsed),
        )

    fun timePointsFor(elapsedSeconds: Int): Int =
        when {
            elapsedSeconds <= ScoringRules.TIME_TIER_1_MAX_SECONDS -> ScoringRules.TIME_TIER_1_POINTS
            elapsedSeconds <= ScoringRules.TIME_TIER_2_MAX_SECONDS -> ScoringRules.TIME_TIER_2_POINTS
            else -> ScoringRules.TIME_TIER_3_POINTS
        }

    fun hintPenaltyFor(hintsUsed: Int): Int =
        when {
            hintsUsed <= 0 -> 0
            hintsUsed == 1 -> 1
            hintsUsed == 2 -> 3
            else -> ScoringRules.MAX_HINT_PENALTY
        }

    /**
     * Pontos que o jogador receberia agora, se acertasse título, artista e
     * (quando a música tiver uma obra associada) a obra neste instante.
     */
    fun pointsAvailableNow(
        elapsedSeconds: Int,
        hintsUsed: Int,
        workAvailable: Boolean = false,
    ): Int {
        val max =
            timePointsFor(elapsedSeconds) + ScoringRules.TITLE_POINTS + ScoringRules.ARTIST_POINTS +
                (if (workAvailable) ScoringRules.WORK_POINTS else 0)
        return (max - hintPenaltyFor(hintsUsed)).coerceAtLeast(0)
    }
}
