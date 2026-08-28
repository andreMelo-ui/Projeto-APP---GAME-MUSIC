package com.desafiomusical.app.network.multiplayer

import com.desafiomusical.app.network.payloads.GameEvent
import com.desafiomusical.app.network.payloads.GameEventJson
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.Collections
import java.util.UUID
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets

/**
 * Testa [KtorHostRoomSession] com um servidor real (porta efêmera em
 * 127.0.0.1) e clientes Ktor crus — sem depender de uma implementação de
 * cliente [RoomSession] (isso é a etapa 2).
 */
class KtorHostRoomSessionTest {
    private var host: KtorHostRoomSession? = null
    private var httpClient: HttpClient? = null

    @After
    fun tearDown(): Unit =
        runBlocking {
            httpClient?.close()
            host?.stop()
        }

    private fun newHttpClient(): HttpClient = HttpClient(ClientCIO) { install(ClientWebSockets) }.also { httpClient = it }

    private fun joinEvent(
        playerId: String,
        roomCode: String,
    ) = GameEvent.JoinRoom(
        eventId = UUID.randomUUID().toString(),
        timestamp = System.currentTimeMillis(),
        playerId = playerId,
        playerName = "Jogador $playerId",
        roomCode = roomCode,
    )

    private class RecordingClient(
        scope: CoroutineScope,
        private val session: DefaultClientWebSocketSession,
    ) {
        val received = Channel<GameEvent>(Channel.UNLIMITED)
        private val job: Job =
            scope.launch {
                for (frame in session.incoming) {
                    if (frame is Frame.Text) {
                        received.trySend(GameEventJson.decodeFromString(GameEvent.serializer(), frame.readText()))
                    }
                }
            }

        suspend fun send(event: GameEvent) {
            session.send(Frame.Text(GameEventJson.encodeToString(GameEvent.serializer(), event)))
        }

        suspend fun nextEvent(timeoutMs: Long = 5_000): GameEvent = withTimeout(timeoutMs) { received.receive() }

        suspend fun close() {
            job.cancel()
            session.close()
        }
    }

    @Test
    fun `broadcast chega em todos, sendTo isolado, eventos duplicados sao ignorados e desconexao atualiza jogadores`(): Unit =
        runBlocking {
            val roomCode = "ABCD"
            val session = KtorHostRoomSession(roomCode = roomCode).also { host = it }
            session.start()

            val hostEventIds = Collections.synchronizedList(mutableListOf<String>())
            val hostCollectJob =
                launch {
                    session.observeIncoming().collect { hostEventIds.add(it.eventId) }
                }

            val client = newHttpClient()
            val ws1 = client.webSocketSession(host = "127.0.0.1", port = session.port, path = KtorHostRoomSession.WS_PATH)
            val ws2 = client.webSocketSession(host = "127.0.0.1", port = session.port, path = KtorHostRoomSession.WS_PATH)
            val client1 = RecordingClient(this, ws1)
            val client2 = RecordingClient(this, ws2)

            val joinP1 = joinEvent("p1", roomCode)
            val joinP2 = joinEvent("p2", roomCode)
            client1.send(joinP1)
            client2.send(joinP2)

            withTimeout(5_000) {
                session.connectedPlayerIds.first { it.toSet() == setOf("p1", "p2") }
            }
            withTimeout(5_000) {
                while (joinP1.eventId !in hostEventIds || joinP2.eventId !in hostEventIds) delay(20)
            }

            // Idempotência: reenviar o MESMO evento (mesmo eventId) não deve gerar uma segunda entrega.
            client1.send(joinP1)
            delay(300)
            assertEquals(1, hostEventIds.count { it == joinP1.eventId })

            // Broadcast chega em ambos os clientes.
            val gameStart = GameEvent.GameStart(eventId = UUID.randomUUID().toString(), timestamp = System.currentTimeMillis())
            session.broadcast(gameStart)
            assertEquals(gameStart.eventId, client1.nextEvent().eventId)
            assertEquals(gameStart.eventId, client2.nextEvent().eventId)

            // sendTo só chega no destinatário.
            val readyOnlyForP1 =
                GameEvent.PlayerReady(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    playerId = "p1",
                    ready = true,
                )
            session.sendTo("p1", readyOnlyForP1)
            assertEquals(readyOnlyForP1.eventId, client1.nextEvent().eventId)
            try {
                client2.nextEvent(timeoutMs = 500)
                fail("client2 não deveria receber um evento endereçado só a p1.")
            } catch (_: TimeoutCancellationException) {
                // esperado
            }

            // Desconexão de um cliente atualiza connectedPlayerIds.
            client1.close()
            withTimeout(5_000) {
                session.connectedPlayerIds.first { it == listOf("p2") }
            }

            hostCollectJob.cancel()
            client2.close()
        }

    @Test
    fun `roomCode invalido no JoinRoom faz o host fechar a conexao`(): Unit =
        runBlocking {
            val session = KtorHostRoomSession(roomCode = "REAL1").also { host = it }
            session.start()

            val client = newHttpClient()
            val ws = client.webSocketSession(host = "127.0.0.1", port = session.port, path = KtorHostRoomSession.WS_PATH)
            ws.send(Frame.Text(GameEventJson.encodeToString(GameEvent.serializer(), joinEvent("intruso", "OUTRO2"))))

            val closeReason = withTimeout(5_000) { ws.closeReason.await() }
            assertNotNull(closeReason)
            assertTrue(session.connectedPlayerIds.value.isEmpty())
        }

    @Test
    fun `reconexao com o mesmo playerId substitui a conexao antiga sem derrubar o roster`(): Unit =
        runBlocking {
            val roomCode = "RECN1"
            val session = KtorHostRoomSession(roomCode = roomCode).also { host = it }
            session.start()

            val client = newHttpClient()
            val ws1 = client.webSocketSession(host = "127.0.0.1", port = session.port, path = KtorHostRoomSession.WS_PATH)
            ws1.send(Frame.Text(GameEventJson.encodeToString(GameEvent.serializer(), joinEvent("p1", roomCode))))
            withTimeout(5_000) { session.connectedPlayerIds.first { it == listOf("p1") } }

            // "p1" reconecta (segunda sessão, mesmo playerId) sem que a primeira jamais tenha sido
            // fechada explicitamente — simula o celular perdendo o Wi-Fi sem avisar ninguém.
            val ws2 = client.webSocketSession(host = "127.0.0.1", port = session.port, path = KtorHostRoomSession.WS_PATH)
            ws2.send(Frame.Text(GameEventJson.encodeToString(GameEvent.serializer(), joinEvent("p1", roomCode))))

            // O host força o fechamento da conexão velha (ver KtorHostRoomSession.handleConnection).
            withTimeout(5_000) { ws1.closeReason.await() }

            // Mesmo depois da limpeza (assíncrona) da conexão velha rodar, o roster continua
            // mostrando "p1" — nunca fica vazio por causa da remoção condicional.
            withTimeout(5_000) { session.connectedPlayerIds.first { it == listOf("p1") } }

            val readyEvent =
                GameEvent.PlayerReady(
                    eventId = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    playerId = "p1",
                    ready = true,
                )
            session.sendTo("p1", readyEvent)

            val received =
                withTimeout(5_000) {
                    var frame: Frame
                    do {
                        frame = ws2.incoming.receive()
                    } while (frame !is Frame.Text)
                    GameEventJson.decodeFromString(GameEvent.serializer(), frame.readText())
                }
            assertEquals(readyEvent.eventId, received.eventId)

            ws2.close()
        }
}
