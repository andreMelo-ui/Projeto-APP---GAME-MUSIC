package com.desafiomusical.app.network.multiplayer

import com.desafiomusical.app.network.payloads.GameEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato da sessão de multiplayer local (host via NSD/Ktor sobre Wi-Fi —
 * implementação prevista para a Fase 3). O [GameEngine][com.desafiomusical.app.domain.GameEngine]
 * nunca fala com sockets diretamente: ele consome/produz [GameEvent]s e uma
 * implementação concreta desta interface cuida do transporte, mantendo a
 * autoridade de estado sempre do lado do host.
 */
interface RoomSession {
    val roomCode: String
    val isHost: Boolean
    val connectedPlayerIds: StateFlow<List<String>>

    suspend fun start()

    suspend fun stop()

    /** Envia um evento para todos os participantes (uso do host). */
    suspend fun broadcast(event: GameEvent)

    /** Envia um evento para um único participante (ex.: payload secreto do escolhedor). */
    suspend fun sendTo(
        playerId: String,
        event: GameEvent,
    )

    /** Eventos recebidos da rede, já deduplicados por eventId antes de chegar aqui. */
    fun observeIncoming(): Flow<GameEvent>
}
