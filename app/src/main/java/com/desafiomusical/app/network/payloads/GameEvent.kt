package com.desafiomusical.app.network.payloads

import kotlinx.serialization.Serializable

/**
 * Contrato de eventos trocados entre host e clientes no multiplayer local
 * (Fase 3). Todo evento carrega [eventId] único (para deduplicação via
 * [IdempotencyGuard]), [timestamp] e, quando fizer sentido, [roundId] e
 * [playerId] — conforme exigido pela especificação (seção 27).
 */
@Serializable
sealed class GameEvent {
    abstract val eventId: String
    abstract val timestamp: Long
    abstract val roundId: String?
    abstract val playerId: String?

    @Serializable
    data class CreateRoom(
        override val eventId: String,
        override val timestamp: Long,
        override val playerId: String,
        val roundCount: Int,
        val stealEnabled: Boolean
    ) : GameEvent() {
        override val roundId: String? = null
    }

    @Serializable
    data class JoinRoom(
        override val eventId: String,
        override val timestamp: Long,
        override val playerId: String,
        val playerName: String,
        val roomCode: String
    ) : GameEvent() {
        override val roundId: String? = null
    }

    @Serializable
    data class PlayerReady(
        override val eventId: String,
        override val timestamp: Long,
        override val playerId: String,
        val ready: Boolean
    ) : GameEvent() {
        override val roundId: String? = null
    }

    @Serializable
    data class GameStart(
        override val eventId: String,
        override val timestamp: Long
    ) : GameEvent() {
        override val roundId: String? = null
        override val playerId: String? = null
    }

    @Serializable
    data class RoundStart(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        val roundNumber: Int,
        val chooserId: String,
        val mainResponderId: String
    ) : GameEvent() {
        override val playerId: String? = null
    }

    @Serializable
    data class SongSelected(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        override val playerId: String,
        val songId: String
    ) : GameEvent()

    @Serializable
    data class SongPlaying(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String
    ) : GameEvent() {
        override val playerId: String? = null
    }

    @Serializable
    data class TimerSync(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        val elapsedSeconds: Int
    ) : GameEvent() {
        override val playerId: String? = null
    }

    @Serializable
    data class HintUsed(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        val hintLevel: Int
    ) : GameEvent() {
        override val playerId: String? = null
    }

    @Serializable
    data class AnswerAttempt(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        override val playerId: String,
        val titleClaimed: Boolean,
        val artistClaimed: Boolean
    ) : GameEvent()

    @Serializable
    data class AnswerResult(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        override val playerId: String,
        val titleCorrect: Boolean,
        val artistCorrect: Boolean,
        val pointsAwarded: Int
    ) : GameEvent()

    @Serializable
    data class StealOpen(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        val eligiblePlayerIds: List<String>,
        val windowSeconds: Int
    ) : GameEvent() {
        override val playerId: String? = null
    }

    @Serializable
    data class StealClaim(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        override val playerId: String
    ) : GameEvent()

    @Serializable
    data class StealResult(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        override val playerId: String,
        val correct: Boolean,
        val pointsAwarded: Int
    ) : GameEvent()

    @Serializable
    data class RoundEnd(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        val winnerId: String?
    ) : GameEvent() {
        override val playerId: String? = null
    }

    @Serializable
    data class GameEnd(
        override val eventId: String,
        override val timestamp: Long,
        val winnerId: String?
    ) : GameEvent() {
        override val roundId: String? = null
        override val playerId: String? = null
    }
}
