package com.desafiomusical.app.network.payloads

/**
 * Garante que cada [GameEvent] recebido pela rede seja processado no máximo
 * uma vez, mesmo sob reenvio (retry de socket, reconexão, pacotes fora de
 * ordem). Mantém uma janela deslizante por [maxAgeMillis] e um teto de
 * entradas em memória para não crescer indefinidamente numa partida longa.
 */
class IdempotencyGuard(
    private val maxAgeMillis: Long = 5 * 60_000L,
    private val maxTrackedEvents: Int = 2_000,
) {
    private data class Seen(val eventId: String, val timestamp: Long)

    private val seenEvents = ArrayDeque<Seen>()
    private val seenIds = HashSet<String>()

    @Synchronized
    fun isNew(
        eventId: String,
        timestamp: Long,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        evictExpired(now)
        if (!seenIds.add(eventId)) return false
        seenEvents.addLast(Seen(eventId, timestamp))
        if (seenEvents.size > maxTrackedEvents) {
            val evicted = seenEvents.removeFirst()
            seenIds.remove(evicted.eventId)
        }
        return true
    }

    fun isNew(
        event: GameEvent,
        now: Long = System.currentTimeMillis(),
    ): Boolean = isNew(event.eventId, event.timestamp, now)

    private fun evictExpired(now: Long) {
        while (seenEvents.isNotEmpty() && now - seenEvents.first().timestamp > maxAgeMillis) {
            val expired = seenEvents.removeFirst()
            seenIds.remove(expired.eventId)
        }
    }

    @Synchronized
    fun clear() {
        seenEvents.clear()
        seenIds.clear()
    }
}
