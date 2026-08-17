package com.desafiomusical.app.network.multiplayer

import com.desafiomusical.app.network.payloads.GameEvent
import com.desafiomusical.app.network.payloads.GameEventJson
import com.desafiomusical.app.network.payloads.IdempotencyGuard
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementação de [RoomSession] para o host: sobe um servidor Ktor (engine
 * CIO) com uma única rota WebSocket e mantém uma conexão por [GameEvent.playerId]
 * — o `playerId` só é conhecido depois que o cliente manda [GameEvent.JoinRoom].
 *
 * Sem descoberta automática (NSD é a etapa 3) nem QR (etapa 4): quem entra na
 * sala precisa saber o IP do host e a porta escolhida (ver [port] após [start]).
 */
class KtorHostRoomSession(
    override val roomCode: String,
    private val requestedPort: Int = 0,
    private val json: Json = GameEventJson,
    private val idempotencyGuard: IdempotencyGuard = IdempotencyGuard()
) : RoomSession {

    override val isHost: Boolean = true

    private val _connectedPlayerIds = MutableStateFlow<List<String>>(emptyList())
    override val connectedPlayerIds: StateFlow<List<String>> = _connectedPlayerIds.asStateFlow()

    private val incomingEvents = MutableSharedFlow<GameEvent>(extraBufferCapacity = 256)
    private val connections = ConcurrentHashMap<String, HostConnection>()

    /** Porta efetivamente vinculada após [start] (resolvida se [requestedPort] for 0). */
    var port: Int = requestedPort
        private set

    private var server: ApplicationEngine? = null

    override suspend fun start() {
        check(server == null) { "Sessão já iniciada." }
        val engine = embeddedServer(CIO, port = requestedPort, host = "0.0.0.0") {
            install(WebSockets)
            routing {
                webSocket(WS_PATH) { handleConnection(this) }
            }
        }
        engine.start(wait = false)
        server = engine
        port = engine.resolvedConnectors().first().port
    }

    override suspend fun stop() {
        connections.values.toList().forEach { runCatching { it.session.close() } }
        connections.clear()
        _connectedPlayerIds.value = emptyList()
        server?.stop(gracePeriodMillis = 250, timeoutMillis = 1000)
        server = null
    }

    override suspend fun broadcast(event: GameEvent) {
        val text = json.encodeToString(GameEvent.serializer(), event)
        connections.values.toList().forEach { sendText(it, text) }
    }

    override suspend fun sendTo(playerId: String, event: GameEvent) {
        val connection = connections[playerId] ?: return
        val text = json.encodeToString(GameEvent.serializer(), event)
        sendText(connection, text)
    }

    override fun observeIncoming(): Flow<GameEvent> = incomingEvents.asSharedFlow()

    private suspend fun sendText(connection: HostConnection, text: String) {
        runCatching {
            connection.sendMutex.withLock { connection.session.send(Frame.Text(text)) }
        }
    }

    /**
     * Uma conexão por [GameEvent.playerId], mas nada impede um mesmo jogador
     * de reconectar (reenviar [GameEvent.JoinRoom] com o mesmo `playerId`) —
     * é assim que a reconexão da etapa 7 funciona no transporte: a entrada no
     * mapa é simplesmente substituída pela conexão nova. O cuidado aqui é não
     * deixar a limpeza da conexão VELHA (que só descobre que morreu depois,
     * de forma assíncrona) derrubar a entrada da conexão NOVA — por isso o
     * `finally` só remove do mapa se a entrada atual ainda for a MESMA
     * instância criada por esta chamada (remoção condicional).
     */
    private suspend fun handleConnection(session: DefaultWebSocketServerSession) {
        var playerId: String? = null
        var connection: HostConnection? = null
        try {
            for (frame in session.incoming) {
                if (frame !is Frame.Text) continue
                val event = runCatching {
                    json.decodeFromString(GameEvent.serializer(), frame.readText())
                }.getOrNull() ?: continue

                if (event is GameEvent.JoinRoom) {
                    if (event.roomCode != roomCode) {
                        session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "roomCode inválido"))
                        return
                    }
                    playerId = event.playerId
                    connection = HostConnection(event.playerId, session)
                    val previous = connections.put(event.playerId, connection)
                    if (previous != null && previous.session !== session) {
                        runCatching { previous.session.close(CloseReason(CloseReason.Codes.NORMAL, "reconectado em outra sessão")) }
                    }
                    _connectedPlayerIds.value = connections.keys.toList()
                }

                if (idempotencyGuard.isNew(event)) {
                    incomingEvents.emit(event)
                }
            }
        } catch (_: ClosedReceiveChannelException) {
            // Desconexão normal do cliente (fechou o socket).
        } finally {
            val pid = playerId
            val conn = connection
            if (pid != null && conn != null && connections.remove(pid, conn)) {
                _connectedPlayerIds.value = connections.keys.toList()
            }
        }
    }

    private data class HostConnection(
        val playerId: String,
        val session: DefaultWebSocketServerSession,
        val sendMutex: Mutex = Mutex()
    )

    companion object {
        const val WS_PATH = "/room"
    }
}
