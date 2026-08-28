package com.desafiomusical.app.domain.state

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateMachineTest {
    @Test
    fun `fluxo feliz completo de uma rodada e valido`() {
        assertTrue(GameStateMachine.canTransition(GamePhase.LOBBY, GamePhase.PLAYER_SETUP))
        assertTrue(GameStateMachine.canTransition(GamePhase.PLAYER_SETUP, GamePhase.CATEGORY_SELECTION))
        assertTrue(GameStateMachine.canTransition(GamePhase.CATEGORY_SELECTION, GamePhase.SONG_SELECTION))
        assertTrue(GameStateMachine.canTransition(GamePhase.SONG_SELECTION, GamePhase.READY))
        assertTrue(GameStateMachine.canTransition(GamePhase.READY, GamePhase.PLAYING))
        assertTrue(GameStateMachine.canTransition(GamePhase.PLAYING, GamePhase.ANSWERING))
        assertTrue(GameStateMachine.canTransition(GamePhase.ANSWERING, GamePhase.ROUND_RESULT))
        assertTrue(GameStateMachine.canTransition(GamePhase.ROUND_RESULT, GamePhase.NEXT_ROUND))
        assertTrue(GameStateMachine.canTransition(GamePhase.NEXT_ROUND, GamePhase.GAME_RESULT))
    }

    @Test
    fun `fluxo de roubo e valido`() {
        assertTrue(GameStateMachine.canTransition(GamePhase.ANSWERING, GamePhase.STEAL_WINDOW))
        assertTrue(GameStateMachine.canTransition(GamePhase.STEAL_WINDOW, GamePhase.STEAL_ANSWER))
        assertTrue(GameStateMachine.canTransition(GamePhase.STEAL_ANSWER, GamePhase.STEAL_WINDOW))
        assertTrue(GameStateMachine.canTransition(GamePhase.STEAL_ANSWER, GamePhase.ROUND_RESULT))
    }

    @Test
    fun `nao permite pular etapas`() {
        assertFalse(GameStateMachine.canTransition(GamePhase.LOBBY, GamePhase.PLAYING))
        assertFalse(GameStateMachine.canTransition(GamePhase.CATEGORY_SELECTION, GamePhase.READY))
    }

    @Test
    fun `nao permite transicao para o mesmo estado`() {
        assertFalse(GameStateMachine.canTransition(GamePhase.PLAYING, GamePhase.PLAYING))
    }

    @Test
    fun `resultado final nao tem transicoes de saida na tabela normal`() {
        GamePhase.entries.forEach { target ->
            assertFalse(GameStateMachine.canTransition(GamePhase.GAME_RESULT, target))
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `transicao invalida lanca excecao`() {
        GameStateMachine.requireTransition(GamePhase.LOBBY, GamePhase.GAME_RESULT)
    }
}
