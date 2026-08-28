package com.desafiomusical.app.network.multiplayer

import com.desafiomusical.app.network.payloads.GameEvent
import com.desafiomusical.app.network.payloads.GameEventJson
import com.desafiomusical.app.network.payloads.IdempotencyGuard
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.min
import kotlin.math.pow

/**
 * Implementação de [RoomSession] para quem entra numa sala (cliente): conecta
 * ao host por IP:porta (digitados manualmente, escaneados via QR ou achados
 * por NSD) e manda [GameEvent.JoinRoom] assim que a conexão abre.
 *
 * [start] faz só a PRIMEIRA tentativa de conexão (lança exceção se falhar —
 * quem chama precisa poder mostrar "não deu pra conectar" na hora, ex.: IP
 * digitado errado). Só DEPOIS de conectar com sucesso é que a reconexão
 * automática (etapa 7) entra em ação: se a conexão cair sozinha (Wi-Fi
 * oscilando), tenta reconectar sozinho com backoff, reenviando
 * [GameEvent.JoinRoom] com o MESMO [playerId] — é assim que o host (ver
 * [KtorHostRoomSession]) reconhece que é uma reconexão, não um jogador novo.
 *
 * O cliente só enxerga o host — não há conexões diretas com outros jogadores
 * — então tanto [broadcast] quanto [sendTo] apenas entregam o evento ao host,
 * que é quem decide (etapa 6) para quem redistribuir. [connectedPlayerIds]
 * aqui é só um registro dos `playerId`s vistos em eventos [GameEvent.JoinRoom]
 * ou [GameEvent.PlayerReady] recebidos do host; a lista completa da sala só
 * fica confiável quando o host começa a redistribuir esses eventos.
 */
class KtorClientRoomSession(
    override val roomCode: String,
    private val playerId: String,
    private val playerName: String,
    private val hostAddress: String,
    private val hostPort: Int,
    private val json: Json = GameEventJson,
    private val idempotencyGuard: IdempotencyGuard = IdempotencyGuard(),
    private val maxReconnectDelayMillis: Long = 8_000L,
) : RoomSession {
    override val isHost: Boolean = false

    private val _connectedPlayerIds = MutableStateFlow<List<String>>(emptyList())
    override val connectedPlayerIds: StateFlow<List<String>> = _connectedPlayerIds.asStateFlow()

    /** `true` enquanto o socket com o host está de pé; cai pra `false` durante uma reconexão. */
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val incomingEvents = MutableSharedFlow<GameEvent>(extraBufferCapacity = 256)
    private val sendMutex = Mutex()

    private val httpClient = HttpClient(CIO) { install(WebSockets) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var session: DefaultClientWebSocketSession? = null
    private var receiveJob: Job? = null

    @Volatile
    private var stopped = false

    override suspend fun start() {
        check(session == null) { "Sessão já iniciada." }
        connectOnce()
        scope.launch { reconnectSupervisor() }
    }

    override suspend fun stop() {
        stopped = true
        receiveJob?.cancel()
        receiveJob = null
        runCatching { session?.close() }
        session = null
        scope.cancel()
        httpClient.close()
        _connectedPlayerIds.value = emptyList()
        _isConnected.value = false
    }

    /** Entrega o evento ao host (não há outro destino possível a partir do cliente). */
    override suspend fun broadcast(event: GameEvent) = sendEvent(event)

    /** Idêntico a [broadcast]: o cliente só fala com o host, que decide o roteamento real. */
    override suspend fun sendTo(
        playerId: String,
        event: GameEvent,
    ) = sendEvent(event)

    override fun observeIncoming(): Flow<GameEvent> = incomingEvents.asSharedFlow()

    private suspend fun sendEvent(event: GameEvent) {
        val ws = session ?: error("Sessão não conectada. Chame start() antes.")
        val text = json.encodeToString(GameEvent.serializer(), event)
        sendMutex.withLock { ws.send(Frame.Text(text)) }
    }

    private suspend fun connectOnce() {
        val ws = httpClient.webSocketSession(host = hostAddress, port = hostPort, path = KtorHostRoomSession.WS_PATH)
        session = ws
        receiveJob = scope.launch { receiveLoop(ws) }
        _isConnected.value = true
        sendEvent(
            GameEvent.JoinRoom(
                eventId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                playerId = playerId,
                playerName = playerName,
                roomCode = roomCode,
            ),
        )
    }

    /**
     * Espera o loop de recebimento atual terminar (a conexão caiu) e tenta
     * reconectar com backoff exponencial (1s, 2s, 4s, ... até
     * [maxReconnectDelayMillis]) — pra sempre, enquanto [stop] não for
     * chamado. Não faz nada se a queda foi provocada por [stop] (checa
     * [stopped] antes de tentar de novo).
     */
    private suspend fun reconnectSupervisor() {
        while (!stopped) {
            receiveJob?.join()
            _isConnected.value = false
            if (stopped) return

            var attempt = 0
            while (!stopped) {
                delay(backoffDelay(attempt))
                if (stopped) return
                attempt++
                if (runCatching { connectOnce() }.isSuccess) break
            }
        }
    }

    private fun backoffDelay(attempt: Int): Long = min(1000L * 2.0.pow(attempt).toLong(), maxReconnectDelayMillis)

    private suspend fun receiveLoop(ws: DefaultClientWebSocketSession) {
        try {
            for (frame in ws.incoming) {
                if (frame !is Frame.Text) continue
                val event =
                    runCatching {
                        json.decodeFromString(GameEvent.serializer(), frame.readText())
                    }.getOrNull() ?: continue

                trackKnownPlayer(event)

                if (idempotencyGuard.isNew(event)) {
                    incomingEvents.emit(event)
                }
            }
        } catch (_: ClosedReceiveChannelException) {
            // Conexão caiu (host encerrou de propósito, ou a rede oscilou).
        }
    }

    private fun trackKnownPlayer(event: GameEvent) {
        val id =
            when (event) {
                is GameEvent.JoinRoom -> event.playerId
                is GameEvent.PlayerReady -> event.playerId
                else -> null
            } ?: return
        _connectedPlayerIds.update { current -> if (id in current) current else current + id }
    }
}
