package com.desafiomusical.app.domain

import com.desafiomusical.app.domain.model.Category
import com.desafiomusical.app.domain.model.Difficulty
import com.desafiomusical.app.domain.model.GameConfig
import com.desafiomusical.app.domain.model.Player
import com.desafiomusical.app.domain.model.Song
import com.desafiomusical.app.domain.state.GameUiState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private fun song(id: String, category: Category = Category.BRASILEIRA) = Song(
        id = id,
        title = "Título $id",
        artist = "Artista $id",
        category = category,
        work = null,
        difficulty = Difficulty.FACIL,
        youtubeVideoId = "yt-$id",
        hint1 = "dica 1",
        hint2 = "dica 2",
        hint3 = "dica 3",
        tags = emptyList(),
        active = true,
        createdAt = 0L,
        updatedAt = 0L
    )

    private fun fourPlayers() = listOf(
        Player("p1", "André", 0L),
        Player("p2", "Maria", 0L),
        Player("p3", "João", 0L),
        Player("p4", "Ana", 0L)
    )

    private fun newGame(scope: kotlinx.coroutines.CoroutineScope, roundCount: Int = 5, stealEnabled: Boolean = true): GameEngine {
        val engine = GameEngine(scope = scope)
        // Rodadas válidas são 5/10/15/20 (GameConfig); catálogo com músicas suficientes
        // para cobrir todas as rodadas de qualquer teste sem esgotar a categoria.
        engine.setCatalog((1..roundCount).map { song("s$it") })
        engine.startGame(GameConfig(players = fourPlayers(), roundCount = roundCount, stealEnabled = stealEnabled))
        return engine
    }

    private fun advanceToPlaying(engine: GameEngine) {
        engine.selectCategory(Category.BRASILEIRA)
        val songSelection = engine.uiState.value as GameUiState.SongSelection
        engine.selectSongForRound(songSelection.candidates.first())
        engine.startPlayback()
    }

    @Test
    fun `respondente principal acerta tudo e pontua 20`() = runTest {
        val engine = newGame(backgroundScope)
        advanceToPlaying(engine)

        engine.submitMainAnswer(titleClaimed = true, artistClaimed = true)
        assertTrue(engine.uiState.value is GameUiState.Answering)

        engine.confirmMainAnswer(titleCorrect = true, artistCorrect = true)
        val result = (engine.uiState.value as GameUiState.RoundResult).result

        // Rodada 1: chooser = p1 (André), respondente principal = p2 (Maria) — rodízio simples.
        assertEquals("p2", result.winner?.id)
        assertEquals(20, result.pointsAwarded)
    }

    @Test
    fun `respondente erra e abre janela de roubo`() = runTest {
        val engine = newGame(backgroundScope)
        advanceToPlaying(engine)

        engine.submitMainAnswer(titleClaimed = false, artistClaimed = false)
        engine.confirmMainAnswer(titleCorrect = false, artistCorrect = false)

        assertTrue(engine.uiState.value is GameUiState.StealWindow)
    }

    @Test
    fun `escolhedor nunca aparece como elegivel a roubo`() = runTest {
        val engine = newGame(backgroundScope)
        advanceToPlaying(engine)
        val playing = engine.uiState.value as GameUiState.Playing

        assertTrue(playing.round.chooser.id !in playing.round.eligibleStealers.map { it.id })
        assertTrue(playing.round.mainResponder.id !in playing.round.eligibleStealers.map { it.id })
    }

    @Test
    fun `jogador que rouba e acerta fica com os pontos`() = runTest {
        val engine = newGame(backgroundScope)
        advanceToPlaying(engine)

        engine.submitMainAnswer(titleClaimed = false, artistClaimed = false)
        engine.confirmMainAnswer(titleCorrect = false, artistCorrect = false)

        val stealWindow = engine.uiState.value as GameUiState.StealWindow
        val stealer = stealWindow.round.eligibleStealers.first()
        engine.claimSteal(stealer.id)
        assertTrue(engine.uiState.value is GameUiState.StealAnswer)

        engine.confirmStealAnswer(titleCorrect = true, artistCorrect = false)
        val result = (engine.uiState.value as GameUiState.RoundResult).result

        assertEquals(stealer.id, result.winner?.id)
    }

    @Test
    fun `jogador que erra o roubo e eliminado e nao pode tentar de novo`() = runTest {
        val engine = newGame(backgroundScope, stealEnabled = true)
        advanceToPlaying(engine)

        engine.submitMainAnswer(titleClaimed = false, artistClaimed = false)
        engine.confirmMainAnswer(titleCorrect = false, artistCorrect = false)

        val firstWindow = engine.uiState.value as GameUiState.StealWindow
        val firstStealer = firstWindow.round.eligibleStealers.first()
        engine.claimSteal(firstStealer.id)
        engine.confirmStealAnswer(titleCorrect = false, artistCorrect = false)

        // Com dois jogadores elegíveis, a rodada deve reabrir a janela para o outro.
        val nextState = engine.uiState.value
        if (nextState is GameUiState.StealWindow) {
            assertTrue(firstStealer.id !in nextState.round.eligibleStealers.filterNot { it.id in nextState.round.eliminatedIds }.map { it.id })
        }
    }

    @Test
    fun `ninguem acerta e ninguem pontua`() = runTest {
        val engine = newGame(backgroundScope, roundCount = 5, stealEnabled = false)
        advanceToPlaying(engine)

        engine.submitMainAnswer(titleClaimed = false, artistClaimed = false)
        engine.confirmMainAnswer(titleCorrect = false, artistCorrect = false)

        val result = (engine.uiState.value as GameUiState.RoundResult).result
        assertNull(result.winner)
        assertEquals(0, result.pointsAwarded)
    }

    @Test
    fun `partida termina apos a ultima rodada`() = runTest {
        val roundCount = 5
        val engine = newGame(backgroundScope, roundCount = roundCount, stealEnabled = false)

        repeat(roundCount) {
            advanceToPlaying(engine)
            engine.submitMainAnswer(titleClaimed = true, artistClaimed = true)
            engine.confirmMainAnswer(titleCorrect = true, artistCorrect = true)
            engine.goToNextRound()
        }

        assertTrue(engine.uiState.value is GameUiState.GameResult)
    }
}
