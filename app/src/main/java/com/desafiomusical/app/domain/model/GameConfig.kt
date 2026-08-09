package com.desafiomusical.app.domain.model

data class GameConfig(
    val players: List<Player>,
    val roundCount: Int,
    val stealEnabled: Boolean
) {
    init {
        require(players.size in 2..4) { "A partida precisa de 2 a 4 jogadores." }
        require(roundCount in setOf(5, 10, 15, 20)) { "Rodadas devem ser 5, 10, 15 ou 20." }
    }
}
