package com.desafiomusical.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateScoreUseCaseTest {

    private val useCase = CalculateScoreUseCase()

    @Test
    fun `0 a 30s com nome e artista corretos soma 20 pontos`() {
        val result = useCase("p1", elapsedSeconds = 24, titleCorrect = true, artistCorrect = true, hintsUsed = 0)
        assertEquals(20, result.totalPoints)
    }

    @Test
    fun `31 a 60s apenas com o nome correto soma 10 pontos`() {
        val result = useCase("p1", elapsedSeconds = 42, titleCorrect = true, artistCorrect = false, hintsUsed = 0)
        assertEquals(10, result.totalPoints)
    }

    @Test
    fun `61 a 90s apenas com o artista correto soma 5 pontos`() {
        val result = useCase("p1", elapsedSeconds = 75, titleCorrect = false, artistCorrect = true, hintsUsed = 0)
        assertEquals(5, result.totalPoints)
    }

    @Test
    fun `limites exatos de tempo 30 60 e 90 segundos`() {
        assertEquals(10, useCase.timePointsFor(30))
        assertEquals(5, useCase.timePointsFor(31))
        assertEquals(5, useCase.timePointsFor(60))
        assertEquals(0, useCase.timePointsFor(61))
        assertEquals(0, useCase.timePointsFor(90))
    }

    @Test
    fun `penalidade de uma dica e -1`() {
        assertEquals(1, useCase.hintPenaltyFor(1))
    }

    @Test
    fun `penalidade de duas dicas e -3`() {
        assertEquals(3, useCase.hintPenaltyFor(2))
    }

    @Test
    fun `penalidade de tres dicas e -6`() {
        assertEquals(6, useCase.hintPenaltyFor(3))
    }

    @Test
    fun `penalidade maxima acumulada nunca passa de -6`() {
        assertEquals(6, useCase.hintPenaltyFor(4))
    }

    @Test
    fun `pontuacao maxima normal e 20 pontos`() {
        val result = useCase("p1", elapsedSeconds = 0, titleCorrect = true, artistCorrect = true, hintsUsed = 0)
        assertEquals(20, result.totalPoints)
    }

    @Test
    fun `dicas reduzem a pontuacao final de quem acerta`() {
        val result = useCase("p1", elapsedSeconds = 10, titleCorrect = true, artistCorrect = true, hintsUsed = 3)
        assertEquals(20 - 6, result.totalPoints)
    }

    @Test
    fun `acertar a obra soma mais 5 pontos`() {
        val result = useCase(
            "p1",
            elapsedSeconds = 0,
            titleCorrect = true,
            artistCorrect = true,
            workCorrect = true,
            hintsUsed = 0
        )
        assertEquals(25, result.totalPoints)
    }

    @Test
    fun `obra nao reivindicada nao soma pontos`() {
        val result = useCase("p1", elapsedSeconds = 0, titleCorrect = true, artistCorrect = true, hintsUsed = 0)
        assertEquals(20, result.totalPoints)
    }

    @Test
    fun `pontos disponiveis agora incluem bonus de obra quando aplicavel`() {
        assertEquals(20, useCase.pointsAvailableNow(elapsedSeconds = 0, hintsUsed = 0, workAvailable = false))
        assertEquals(25, useCase.pointsAvailableNow(elapsedSeconds = 0, hintsUsed = 0, workAvailable = true))
    }
}
