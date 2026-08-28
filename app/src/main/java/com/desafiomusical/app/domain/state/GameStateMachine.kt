package com.desafiomusical.app.domain.state

/**
 * Tabela de transições válidas entre [GamePhase]s. Qualquer transição fora
 * desta tabela é rejeitada, o que impede pontuação duplicada e estados
 * inconsistentes originados por eventos de rede fora de ordem.
 */
object GameStateMachine {
    private val validTransitions: Map<GamePhase, Set<GamePhase>> =
        mapOf(
            GamePhase.LOBBY to setOf(GamePhase.PLAYER_SETUP),
            GamePhase.PLAYER_SETUP to setOf(GamePhase.CATEGORY_SELECTION, GamePhase.LOBBY),
            GamePhase.CATEGORY_SELECTION to setOf(GamePhase.SONG_SELECTION),
            GamePhase.SONG_SELECTION to setOf(GamePhase.READY),
            GamePhase.READY to setOf(GamePhase.PLAYING),
            GamePhase.PLAYING to setOf(GamePhase.ANSWERING, GamePhase.ROUND_RESULT),
            GamePhase.ANSWERING to setOf(GamePhase.STEAL_WINDOW, GamePhase.ROUND_RESULT),
            GamePhase.STEAL_WINDOW to setOf(GamePhase.STEAL_ANSWER, GamePhase.ROUND_RESULT),
            GamePhase.STEAL_ANSWER to setOf(GamePhase.STEAL_WINDOW, GamePhase.ROUND_RESULT),
            GamePhase.ROUND_RESULT to setOf(GamePhase.NEXT_ROUND, GamePhase.GAME_RESULT),
            GamePhase.NEXT_ROUND to setOf(GamePhase.CATEGORY_SELECTION, GamePhase.GAME_RESULT),
            GamePhase.GAME_RESULT to emptySet(),
        )

    fun canTransition(
        from: GamePhase,
        to: GamePhase,
    ): Boolean {
        if (from == to) return false
        return validTransitions[from]?.contains(to) == true
    }

    fun requireTransition(
        from: GamePhase,
        to: GamePhase,
    ) {
        check(canTransition(from, to)) {
            "Transição inválida de $from para $to"
        }
    }
}
