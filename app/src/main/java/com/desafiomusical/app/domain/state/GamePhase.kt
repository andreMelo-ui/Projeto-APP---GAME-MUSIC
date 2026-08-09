package com.desafiomusical.app.domain.state

/**
 * Máquina de estados estrita da partida. A ordem declarada é a ordem
 * "natural" do fluxo, usada por [GameStateMachine] para validar transições.
 */
enum class GamePhase {
    LOBBY,
    PLAYER_SETUP,
    CATEGORY_SELECTION,
    SONG_SELECTION,
    READY,
    PLAYING,
    ANSWERING,
    STEAL_WINDOW,
    STEAL_ANSWER,
    ROUND_RESULT,
    NEXT_ROUND,
    GAME_RESULT
}
