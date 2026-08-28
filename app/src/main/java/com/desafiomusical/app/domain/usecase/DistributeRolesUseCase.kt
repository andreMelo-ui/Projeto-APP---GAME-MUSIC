package com.desafiomusical.app.domain.usecase

import com.desafiomusical.app.domain.model.Player
import com.desafiomusical.app.domain.model.RoundSetup

/**
 * Distribui escolhedor e respondente principal de forma equilibrada (rodízio),
 * garantindo que o escolhedor nunca seja o respondente da própria rodada e que
 * todos os jogadores passem por ambos os papéis um número parecido de vezes.
 */
class DistributeRolesUseCase {
    operator fun invoke(
        players: List<Player>,
        roundCount: Int,
    ): List<RoundSetup> {
        require(players.size in 2..4) { "A partida precisa de 2 a 4 jogadores." }
        require(roundCount > 0) { "roundCount deve ser positivo." }

        val playerCount = players.size
        return (0 until roundCount).map { index ->
            val chooserIndex = index % playerCount
            val responderIndex = (index + 1) % playerCount
            val chooser = players[chooserIndex]
            val responder = players[responderIndex]
            val eligibleStealers =
                players
                    .filterNot { it.id == chooser.id || it.id == responder.id }
                    .map { it.id }

            RoundSetup(
                roundNumber = index + 1,
                chooserId = chooser.id,
                mainResponderId = responder.id,
                eligibleStealerIds = eligibleStealers,
            )
        }
    }
}
