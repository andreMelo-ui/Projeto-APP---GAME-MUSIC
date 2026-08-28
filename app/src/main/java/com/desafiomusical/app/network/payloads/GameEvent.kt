package com.desafiomusical.app.network.payloads

import kotlinx.serialization.Serializable

/** Um jogador da sala durante o lobby (antes da partida começar) — ver [GameEvent.RoomRoster]. */
@Serializable
data class RoomPlayerInfo(
    val playerId: String,
    val playerName: String,
    val ready: Boolean,
)

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
        val stealEnabled: Boolean,
    ) : GameEvent() {
        override val roundId: String? = null
    }

    @Serializable
    data class JoinRoom(
        override val eventId: String,
        override val timestamp: Long,
        override val playerId: String,
        val playerName: String,
        val roomCode: String,
    ) : GameEvent() {
        override val roundId: String? = null
    }

    @Serializable
    data class PlayerReady(
        override val eventId: String,
        override val timestamp: Long,
        override val playerId: String,
        val ready: Boolean,
    ) : GameEvent() {
        override val roundId: String? = null
    }

    /**
     * Retrato completo da sala durante o lobby: configurações + quem já
     * entrou e quem está pronto. O host manda de novo (broadcast) a cada
     * mudança — entrada, saída ou alguém alternando pronto — em vez de os
     * clientes tentarem reconstruir isso sozinhos a partir de uma sequência
     * de [JoinRoom]/[PlayerReady]. Sem risco de segurança aqui: não há
     * música nem pontuação envolvida antes da partida começar.
     */
    @Serializable
    data class RoomRoster(
        override val eventId: String,
        override val timestamp: Long,
        val roundCount: Int,
        val stealEnabled: Boolean,
        val players: List<RoomPlayerInfo>,
    ) : GameEvent() {
        override val roundId: String? = null
        override val playerId: String? = null
    }

    @Serializable
    data class GameStart(
        override val eventId: String,
        override val timestamp: Long,
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
        val mainResponderId: String,
    ) : GameEvent() {
        override val playerId: String? = null
    }

    @Serializable
    data class SongSelected(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        override val playerId: String,
        val songId: String,
    ) : GameEvent()

    /**
     * Anúncio de rodada pronta para tocar, com os campos JÁ MASCARADOS de
     * [com.desafiomusical.app.domain.model.MaskedSong] — nunca título, artista
     * ou obra. Seguro para [com.desafiomusical.app.network.multiplayer.RoomSession.broadcast].
     */
    @Serializable
    data class SongPlaying(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        val category: String,
        val difficulty: String,
        val youtubeVideoId: String,
        val hasWork: Boolean,
    ) : GameEvent() {
        override val playerId: String? = null
    }

    /**
     * Enviado só ao escolhedor da rodada (via
     * [com.desafiomusical.app.network.multiplayer.RoomSession.sendTo]), com os
     * `songId`s candidatos por categoria — o cliente resolve o restante (título,
     * artista) no próprio catálogo local, que é idêntico ao do host.
     */
    @Serializable
    data class SongOptions(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        override val playerId: String,
        val categories: List<String>,
        val candidateSongIdsByCategory: Map<String, List<String>>,
    ) : GameEvent()

    @Serializable
    data class TimerSync(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        val elapsedSeconds: Int,
    ) : GameEvent() {
        override val playerId: String? = null
    }

    /**
     * Trafega nos dois sentidos: sem [playerId] dedicado (a rede não distingue
     * quem pediu), o host trata qualquer chegada como "pedir a próxima dica" e
     * broadcasta de volta com [hintText] já preenchido — o texto revelado é
     * seguro para todos assim que a dica é usada.
     */
    @Serializable
    data class HintUsed(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        val hintLevel: Int,
        val hintText: String = "",
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
        val artistClaimed: Boolean,
    ) : GameEvent()

    @Serializable
    data class AnswerResult(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        override val playerId: String,
        val titleCorrect: Boolean,
        val artistCorrect: Boolean,
        val pointsAwarded: Int,
        val workCorrect: Boolean = false,
    ) : GameEvent()

    /**
     * Enviado SÓ pelo escolhedor (o julgador) ao host: o veredito sobre a
     * resposta do respondente principal, depois de ouvi-la de verdade — o
     * equivalente em rede de [com.desafiomusical.app.domain.GameEngine.confirmMainAnswer].
     * [playerId] aqui é o julgador (escolhedor), não o respondente.
     */
    @Serializable
    data class AnswerJudged(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        override val playerId: String,
        val titleCorrect: Boolean,
        val artistCorrect: Boolean,
        val workCorrect: Boolean = false,
    ) : GameEvent()

    @Serializable
    data class StealOpen(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        val eligiblePlayerIds: List<String>,
        val windowSeconds: Int,
    ) : GameEvent() {
        override val playerId: String? = null
    }

    @Serializable
    data class StealClaim(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        override val playerId: String,
    ) : GameEvent()

    @Serializable
    data class StealResult(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        override val playerId: String,
        val correct: Boolean,
        val pointsAwarded: Int,
    ) : GameEvent()

    /**
     * Enviado SÓ pelo escolhedor (o julgador) ao host: o veredito sobre a
     * tentativa de roubo — equivalente em rede de
     * [com.desafiomusical.app.domain.GameEngine.confirmStealAnswer]. [playerId]
     * aqui é o julgador (escolhedor), não quem está roubando.
     */
    @Serializable
    data class StealJudged(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        override val playerId: String,
        val titleCorrect: Boolean,
        val artistCorrect: Boolean,
        val workCorrect: Boolean = false,
    ) : GameEvent()

    /**
     * [songId] só aparece aqui porque a rodada já terminou e foi julgada — é o
     * único ponto, além de [SongOptions] (privado ao escolhedor), em que a
     * identidade da música é segura para [com.desafiomusical.app.network.multiplayer.RoomSession.broadcast].
     */
    @Serializable
    data class RoundEnd(
        override val eventId: String,
        override val timestamp: Long,
        override val roundId: String,
        val winnerId: String?,
        val songId: String,
    ) : GameEvent() {
        override val playerId: String? = null
    }

    @Serializable
    data class GameEnd(
        override val eventId: String,
        override val timestamp: Long,
        val winnerId: String?,
    ) : GameEvent() {
        override val roundId: String? = null
        override val playerId: String? = null
    }
}
