package com.desafiomusical.app.network.multiplayer

import com.desafiomusical.app.network.payloads.GameEvent
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/**
 * Round-trip real: [KtorHostRoomSession] de verdade + dois [KtorClientRoomSession]
 * de verdade conectando por IP:porta (127.0.0.1 + porta efêmera do host).
 */
class KtorClientRoomSessionTest {

    private var host: KtorHostRoomSession? = null
    private val clients = mutableListOf<KtorClientRoomSession>()
    private val collectJobs = mutableListOf<Job>()

    @After
    fun tearDown(): Unit = runBlocking {
        clients.forEach { runCatching { it.stop() } }
        host?.stop()
    }

    /**
     * Assina [RoomSession.observeIncoming] ANTES de qualquer evento ser disparado, evitando a
     * corrida de perder eventos num SharedFlow sem replay. O job fica rodando para sempre (é um
     * `collect` sem fim) — por isso é essencial cancelá-lo DENTRO do mesmo `runBlocking` do teste
     * (ver [cancelCollectors]), já que `runBlocking` só retorna depois que todos os filhos
     * terminam; cancelar só no `@After` deixaria o `runBlocking` do teste travado para sempre.
     */
    private fun CoroutineScope.recordIncoming(session: RoomSession): Channel<GameEvent> {
        val channel = Channel<GameEvent>(Channel.UNLIMITED)
        collectJobs.add(launch { session.observeIncoming().collect { channel.trySend(it) } })
        return channel
    }

    private fun cancelCollectors() {
        collectJobs.forEach { it.cancel() }
        collectJobs.clear()
    }

    private suspend fun newHost(roomCode: String = "ABCD"): KtorHostRoomSession {
        val session = KtorHostRoomSession(roomCode = roomCode)
        host = session
        session.start()
        return session
    }

    private suspend fun newClient(hostSession: KtorHostRoomSession, playerId: String, playerName: String): KtorClientRoomSession {
        val client = KtorClientRoomSession(
            roomCode = hostSession.roomCode,
            playerId = playerId,
            playerName = playerName,
            hostAddress = "127.0.0.1",
            hostPort = hostSession.port
        )
        clients.add(client)
        client.start()
        return client
    }

    @Test
    fun `dois clientes entram, broadcast e sendTo do host chegam corretamente e eventos do cliente chegam no host`(): Unit =
        runBlocking {
            val hostSession = newHost()
            // Assina o fluxo do host ANTES de qualquer cliente entrar (SharedFlow sem replay),
            // para não perder os dois JoinRoom que chegam assim que os clientes conectam.
            val hostReceived = recordIncoming(hostSession)

            val client1 = newClient(hostSession, "p1", "Ana")
            val client2 = newClient(hostSession, "p2", "Beto")
            val client1Received = recordIncoming(client1)
            val client2Received = recordIncoming(client2)

            withTimeout(5_000) {
                hostSession.connectedPlayerIds.first { it.toSet() == setOf("p1", "p2") }
            }

            // Drena os dois JoinRoom (p1 e p2) antes de seguir com as próximas asserções.
            val joinedIds = setOf(
                withTimeout(5_000) { hostReceived.receive() },
                withTimeout(5_000) { hostReceived.receive() }
            ).mapNotNull { (it as? GameEvent.JoinRoom)?.playerId }.toSet()
            assertEquals(setOf("p1", "p2"), joinedIds)

            // Broadcast do host chega em ambos os clientes.
            val gameStart = GameEvent.GameStart(eventId = UUID.randomUUID().toString(), timestamp = System.currentTimeMillis())
            hostSession.broadcast(gameStart)
            assertEquals(gameStart.eventId, withTimeout(5_000) { client1Received.receive() }.eventId)
            assertEquals(gameStart.eventId, withTimeout(5_000) { client2Received.receive() }.eventId)

            // sendTo do host só chega no destinatário.
            val readyForP1 = GameEvent.PlayerReady(
                eventId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                playerId = "p1",
                ready = true
            )
            hostSession.sendTo("p1", readyForP1)
            assertEquals(readyForP1.eventId, withTimeout(5_000) { client1Received.receive() }.eventId)
            try {
                withTimeout(500) { client2Received.receive() }
                fail("client2 não deveria receber um evento endereçado só a p1.")
            } catch (_: TimeoutCancellationException) {
                // esperado
            }

            // Evento enviado por um cliente (broadcast() do cliente == fala com o host) chega no host.
            val client2Ready = GameEvent.PlayerReady(
                eventId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                playerId = "p2",
                ready = true
            )
            client2.broadcast(client2Ready)
            assertEquals(client2Ready.eventId, withTimeout(5_000) { hostReceived.receive() }.eventId)

            // trackKnownPlayer: quando o host repassa um PlayerReady, o cliente registra o playerId.
            hostSession.broadcast(client2Ready)
            withTimeout(5_000) { client1Received.receive() } // consome a entrega do broadcast acima
            withTimeout(5_000) { client1.connectedPlayerIds.first { "p2" in it } }

            // Idempotência do lado do cliente: reenviar o MESMO evento (mesmo eventId) não duplica.
            hostSession.sendTo("p1", readyForP1)
            try {
                withTimeout(500) { client1Received.receive() }
                fail("client1 não deveria receber de novo um evento com o mesmo eventId.")
            } catch (_: TimeoutCancellationException) {
                // esperado
            }

            // Desconexão do cliente atualiza connectedPlayerIds do host.
            client1.stop()
            withTimeout(5_000) {
                hostSession.connectedPlayerIds.first { it == listOf("p2") }
            }

            cancelCollectors()
        }

    @Test
    fun `cliente reconecta sozinho quando a conexao cai inesperadamente e volta a funcionar`(): Unit = runBlocking {
        val hostSession = newHost()

        // maxReconnectDelayMillis baixo só pra não deixar o teste lento — a lógica de backoff em si
        // (ver KtorClientRoomSession.backoffDelay) não muda de comportamento, só o tempo de espera.
        val client1 = KtorClientRoomSession(
            roomCode = hostSession.roomCode,
            playerId = "p1",
            playerName = "Ana",
            hostAddress = "127.0.0.1",
            hostPort = hostSession.port,
            maxReconnectDelayMillis = 200
        )
        clients.add(client1)
        client1.start()

        withTimeout(5_000) { hostSession.connectedPlayerIds.first { it == listOf("p1") } }
        withTimeout(5_000) { client1.isConnected.first { it } }

        val hostReceived = recordIncoming(hostSession)

        // Simula a conexão de client1 caindo "sem avisar": um impostor se conecta como "p1" e o
        // host força o fechamento da conexão antiga (mesmo mecanismo do teste de reconexão em
        // KtorHostRoomSessionTest, agora visto do lado do KtorClientRoomSession real). Para logo em
        // seguida — é só um gatilho de uma vez, não pode ficar competindo pela reconexão de client1.
        val impostor = KtorClientRoomSession(
            roomCode = hostSession.roomCode,
            playerId = "p1",
            playerName = "Impostor",
            hostAddress = "127.0.0.1",
            hostPort = hostSession.port
        )
        impostor.start()
        impostor.stop()

        withTimeout(5_000) { client1.isConnected.first { !it } }
        withTimeout(10_000) { client1.isConnected.first { it } }

        // A reconexão reenviou JoinRoom com o mesmo playerId — o host volta a listar só "p1" (a
        // conexão nova de client1 tomou de volta o lugar do impostor).
        withTimeout(5_000) { hostSession.connectedPlayerIds.first { it == listOf("p1") } }

        // E a sessão reconectada continua funcional nos dois sentidos. hostReceived também recebeu
        // os JoinRoom do impostor e da reconexão de client1 antes disto — dreno até achar a sonda.
        val probe = GameEvent.PlayerReady(UUID.randomUUID().toString(), System.currentTimeMillis(), "p1", true)
        client1.broadcast(probe)
        withTimeout(5_000) {
            var received: GameEvent
            do {
                received = hostReceived.receive()
            } while (received.eventId != probe.eventId)
        }

        cancelCollectors()
    }
}
