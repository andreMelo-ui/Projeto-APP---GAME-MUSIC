@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.desafiomusical.app.network.multiplayer

import com.desafiomusical.app.domain.GameEngine
import com.desafiomusical.app.domain.model.Category
import com.desafiomusical.app.domain.model.Difficulty
import com.desafiomusical.app.domain.model.GameConfig
import com.desafiomusical.app.domain.model.Player
import com.desafiomusical.app.domain.model.Song
import com.desafiomusical.app.network.payloads.GameEvent
import com.desafiomusical.app.network.payloads.GameEventJson
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Testa [HostGameCoordinator] com um [RoomSession] falso, em memória (sem
 * sockets) — cobre a ponte GameEngine↔rede e, principalmente, a regra crítica:
 * nenhum evento de [FakeRoomSession.broadcast] pode revelar título/artista/obra
 * da música da rodada corrente.
 *
 * Usa [kotlinx.coroutines.test.runTest] com `backgroundScope`, igual ao
 * [com.desafiomusical.app.domain.GameEngineTest] — mas aqui, ao contrário de
 * lá, os eventos de rede chegam por um coletor rodando numa coroutine
 * SEPARADA (`observeIncoming().onEach{}.launchIn(scope)`), então cada
 * `emitIncoming` precisa de [runCurrent] para o coletor processar antes das
 * asserções. Nunca use `advanceUntilIdle()` aqui: o cronômetro global do
 * GameEngine também vive nesse mesmo `backgroundScope` e avançaria os 90s
 * inteiros da rodada, disparando o timeout antes da hora.
 */
class HostGameCoordinatorTest {
    private class FakeRoomSession(override val roomCode: String = "TEST") : RoomSession {
        override val isHost: Boolean = true
        private val _connectedPlayerIds = MutableStateFlow<List<String>>(emptyList())
        override val connectedPlayerIds: StateFlow<List<String>> = _connectedPlayerIds.asStateFlow()
        private val incoming = MutableSharedFlow<GameEvent>(extraBufferCapacity = 256)

        val broadcasted = mutableListOf<GameEvent>()
        val sentTo = mutableListOf<Pair<String, GameEvent>>()

        override suspend fun start() = Unit

        override suspend fun stop() = Unit

        override suspend fun broadcast(event: GameEvent) {
            broadcasted.add(event)
        }

        override suspend fun sendTo(
            playerId: String,
            event: GameEvent,
        ) {
            sentTo.add(playerId to event)
        }

        override fun observeIncoming(): Flow<GameEvent> = incoming.asSharedFlow()

        suspend fun emitIncoming(event: GameEvent) = incoming.emit(event)
    }

    private fun song(
        id: String,
        category: Category = Category.BRASILEIRA,
        work: String? = null,
    ) = Song(
        id = id,
        title = "Título $id",
        artist = "Artista $id",
        category = category,
        work = work,
        difficulty = Difficulty.FACIL,
        youtubeVideoId = "yt-$id",
        hint1 = "dica 1",
        hint2 = "dica 2",
        hint3 = "dica 3",
        tags = emptyList(),
        active = true,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun fourPlayers() =
        listOf(
            Player("p1", "Ana", 0L),
            Player("p2", "Beto", 0L),
            Player("p3", "Caio", 0L),
            Player("p4", "Duda", 0L),
        )

    private fun newId() = UUID.randomUUID().toString()

    @Test
    fun `rodada completa via escolhedor remoto nunca vaza titulo artista ou obra em broadcast`() =
        runTest {
            val engine = GameEngine(scope = backgroundScope)
            val catalog = (1..8).map { song("s$it", work = if (it == 1) "Obra $it" else null) }
            engine.setCatalog(catalog)
            val room = FakeRoomSession()
            val coordinator = HostGameCoordinator(engine, room, backgroundScope)
            runCurrent() // deixa o coletor de observeIncoming() assinar o SharedFlow antes de qualquer evento.
            coordinator.startGame(GameConfig(players = fourPlayers(), roundCount = 5, stealEnabled = true))

            // Round 1: chooserIndex=0 (p1), responderIndex=1 (p2) — DistributeRolesUseCase.
            val roundStart = room.broadcasted.filterIsInstance<GameEvent.RoundStart>().single()
            assertEquals(1, roundStart.roundNumber)
            assertEquals("p1", roundStart.chooserId)
            assertEquals("p2", roundStart.mainResponderId)

            // SongOptions só foi enviado (sendTo) ao escolhedor — nunca broadcast.
            assertTrue(room.broadcasted.none { it is GameEvent.SongOptions })
            val (recipient, songOptionsEvent) = room.sentTo.single()
            assertEquals("p1", recipient)
            val songOptions = songOptionsEvent as GameEvent.SongOptions
            assertEquals(roundStart.roundId, songOptions.roundId)
            val pickedSongId = songOptions.candidateSongIdsByCategory.getValue(Category.BRASILEIRA.name).first()

            room.emitIncoming(GameEvent.SongSelected(newId(), 0L, roundStart.roundId, "p1", pickedSongId))
            runCurrent()

            val songPlaying = room.broadcasted.filterIsInstance<GameEvent.SongPlaying>().single()
            assertEquals(roundStart.roundId, songPlaying.roundId)
            assertEquals(Category.BRASILEIRA.name, songPlaying.category)

            room.emitIncoming(GameEvent.AnswerAttempt(newId(), 0L, roundStart.roundId, "p2", titleClaimed = true, artistClaimed = true))
            runCurrent()

            room.emitIncoming(
                GameEvent.AnswerJudged(
                    newId(),
                    0L,
                    roundStart.roundId,
                    "p1",
                    titleCorrect = true,
                    artistCorrect = true,
                    workCorrect = false,
                ),
            )
            runCurrent()

            val answerResult = room.broadcasted.filterIsInstance<GameEvent.AnswerResult>().single()
            assertEquals("p2", answerResult.playerId)
            assertTrue(answerResult.titleCorrect && answerResult.artistCorrect)
            assertTrue(answerResult.pointsAwarded > 0)

            val roundEnd = room.broadcasted.filterIsInstance<GameEvent.RoundEnd>().single()
            assertEquals("p2", roundEnd.winnerId)
            assertEquals(pickedSongId, roundEnd.songId)

            // A regra crítica: em NENHUM evento de broadcast (nunca) aparece título/artista/obra.
            val chosenSong = catalog.first { it.id == pickedSongId }
            room.broadcasted.forEach { event ->
                val serialized = GameEventJson.encodeToString(GameEvent.serializer(), event)
                assertFalse("Vazou título em ${event::class.simpleName}", serialized.contains(chosenSong.title))
                assertFalse("Vazou artista em ${event::class.simpleName}", serialized.contains(chosenSong.artist))
            }
            // E o songId em si só aparece depois de julgada (RoundEnd) — nunca antes.
            room.broadcasted.filterNot { it is GameEvent.RoundEnd }.forEach { event ->
                val serialized = GameEventJson.encodeToString(GameEvent.serializer(), event)
                assertFalse("Vazou songId cedo em ${event::class.simpleName}", serialized.contains("\"$pickedSongId\""))
            }
        }

    @Test
    fun `evento de quem nao e o escolhedor da rodada e ignorado`() =
        runTest {
            val engine = GameEngine(scope = backgroundScope)
            engine.setCatalog((1..8).map { song("s$it") })
            val room = FakeRoomSession()
            val coordinator = HostGameCoordinator(engine, room, backgroundScope)
            runCurrent() // deixa o coletor de observeIncoming() assinar o SharedFlow antes de qualquer evento.
            coordinator.startGame(GameConfig(players = fourPlayers(), roundCount = 5, stealEnabled = true))

            val roundStart = room.broadcasted.filterIsInstance<GameEvent.RoundStart>().single()
            val songOptions = room.sentTo.single().second as GameEvent.SongOptions
            val songId = songOptions.candidateSongIdsByCategory.values.first().first()

            // p3 não é o escolhedor (p1 é) — a tentativa deve ser descartada silenciosamente.
            room.emitIncoming(GameEvent.SongSelected(newId(), 0L, roundStart.roundId, "p3", songId))
            runCurrent()

            assertTrue(room.broadcasted.none { it is GameEvent.SongPlaying })
        }

    @Test
    fun `evento com roundId de rodada antiga e ignorado`() =
        runTest {
            val engine = GameEngine(scope = backgroundScope)
            engine.setCatalog((1..8).map { song("s$it") })
            val room = FakeRoomSession()
            val coordinator = HostGameCoordinator(engine, room, backgroundScope)
            runCurrent() // deixa o coletor de observeIncoming() assinar o SharedFlow antes de qualquer evento.
            coordinator.startGame(GameConfig(players = fourPlayers(), roundCount = 5, stealEnabled = true))

            val songOptions = room.sentTo.single().second as GameEvent.SongOptions
            val songId = songOptions.candidateSongIdsByCategory.values.first().first()

            room.emitIncoming(GameEvent.SongSelected(newId(), 0L, "rodada-que-ja-acabou", "p1", songId))
            runCurrent()

            assertTrue(room.broadcasted.none { it is GameEvent.SongPlaying })
        }

    @Test
    fun `fluxo de roubo remoto e julgado pelo escolhedor, host permanece autoridade da pontuacao`() =
        runTest {
            val engine = GameEngine(scope = backgroundScope)
            engine.setCatalog((1..8).map { song("s$it") })
            val room = FakeRoomSession()
            val coordinator = HostGameCoordinator(engine, room, backgroundScope)
            runCurrent() // deixa o coletor de observeIncoming() assinar o SharedFlow antes de qualquer evento.
            coordinator.startGame(GameConfig(players = fourPlayers(), roundCount = 5, stealEnabled = true))

            val roundStart = room.broadcasted.filterIsInstance<GameEvent.RoundStart>().single()
            val songOptions = room.sentTo.single().second as GameEvent.SongOptions
            val songId = songOptions.candidateSongIdsByCategory.values.first().first()

            room.emitIncoming(GameEvent.SongSelected(newId(), 0L, roundStart.roundId, "p1", songId))
            runCurrent()
            room.emitIncoming(GameEvent.AnswerAttempt(newId(), 0L, roundStart.roundId, "p2", titleClaimed = false, artistClaimed = false))
            runCurrent()
            room.emitIncoming(GameEvent.AnswerJudged(newId(), 0L, roundStart.roundId, "p1", titleCorrect = false, artistCorrect = false))
            runCurrent()

            val answerResult = room.broadcasted.filterIsInstance<GameEvent.AnswerResult>().single()
            assertFalse(answerResult.titleCorrect)
            assertEquals(0, answerResult.pointsAwarded)

            val stealOpen = room.broadcasted.filterIsInstance<GameEvent.StealOpen>().single()
            assertEquals(setOf("p3", "p4"), stealOpen.eligiblePlayerIds.toSet())

            room.emitIncoming(GameEvent.StealClaim(newId(), 0L, roundStart.roundId, "p3"))
            runCurrent()

            // Só o escolhedor (p1) pode julgar o roubo — p4 tentando julgar é ignorado.
            room.emitIncoming(GameEvent.StealJudged(newId(), 0L, roundStart.roundId, "p4", titleCorrect = true, artistCorrect = true))
            runCurrent()
            assertTrue(room.broadcasted.none { it is GameEvent.StealResult })

            room.emitIncoming(GameEvent.StealJudged(newId(), 0L, roundStart.roundId, "p1", titleCorrect = true, artistCorrect = true))
            runCurrent()

            val stealResult = room.broadcasted.filterIsInstance<GameEvent.StealResult>().single()
            assertEquals("p3", stealResult.playerId)
            assertTrue(stealResult.correct)
            assertTrue(stealResult.pointsAwarded > 0)

            val roundEnd = room.broadcasted.filterIsInstance<GameEvent.RoundEnd>().single()
            assertEquals("p3", roundEnd.winnerId)
        }

    /**
     * O cronômetro global do [GameEngine] mede o tempo decorrido com
     * `System.currentTimeMillis()` (tempo real de parede), não contagem de
     * `delay()` — por isso este teste NÃO pode usar `runTest`/`advanceTimeBy`
     * (que só adiantam o relógio virtual) e precisa de tempo real de verdade.
     */
    @Test
    fun `timer sync e transmitido periodicamente enquanto a rodada esta tocando`(): Unit =
        kotlinx.coroutines.runBlocking {
            val jobScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Job())
            try {
                val engine = GameEngine(scope = jobScope)
                engine.setCatalog((1..8).map { song("s$it") })
                val room = FakeRoomSession()
                val coordinator = HostGameCoordinator(engine, room, jobScope)
                kotlinx.coroutines.delay(50) // deixa o coletor de observeIncoming() assinar o SharedFlow antes de qualquer evento.

                coordinator.startGame(GameConfig(players = fourPlayers(), roundCount = 5, stealEnabled = true))
                val roundStart = room.broadcasted.filterIsInstance<GameEvent.RoundStart>().single()
                val songOptions = room.sentTo.single().second as GameEvent.SongOptions
                val songId = songOptions.candidateSongIdsByCategory.values.first().first()
                room.emitIncoming(GameEvent.SongSelected(newId(), 0L, roundStart.roundId, roundStart.chooserId, songId))
                kotlinx.coroutines.delay(1_200) // tempo real: deixa o cronômetro global bater ao menos 1 tick.

                val ticks = room.broadcasted.filterIsInstance<GameEvent.TimerSync>()
                assertTrue(ticks.isNotEmpty())
            } finally {
                jobScope.cancel()
            }
        }

    @Test
    fun `JoinRoom de quem ja esta na partida e tratado como reconexao e ressincroniza sem vazar a musica`() =
        runTest {
            val engine = GameEngine(scope = backgroundScope)
            val catalog = (1..8).map { song("s$it") }
            engine.setCatalog(catalog)
            val room = FakeRoomSession()
            val coordinator = HostGameCoordinator(engine, room, backgroundScope)
            runCurrent()
            coordinator.startGame(GameConfig(players = fourPlayers(), roundCount = 5, stealEnabled = true))

            val roundStart = room.broadcasted.filterIsInstance<GameEvent.RoundStart>().single()
            val songOptions = room.sentTo.single().second as GameEvent.SongOptions
            val songId = songOptions.candidateSongIdsByCategory.values.first().first()
            room.emitIncoming(GameEvent.SongSelected(newId(), 0L, roundStart.roundId, "p1", songId))
            runCurrent()

            room.sentTo.clear()

            // p2 (respondente) reconecta no meio da rodada — KtorClientRoomSession reenvia JoinRoom com o mesmo playerId.
            room.emitIncoming(GameEvent.JoinRoom(newId(), 0L, "p2", "Beto", room.roomCode))
            runCurrent()

            val resentToP2 = room.sentTo.filter { it.first == "p2" }.map { it.second }
            assertTrue(resentToP2.any { it is GameEvent.RoundStart })
            assertTrue(resentToP2.any { it is GameEvent.SongPlaying })
            assertTrue(resentToP2.any { it is GameEvent.TimerSync })

            val chosenSong = catalog.first { it.id == songId }
            resentToP2.forEach { event ->
                val serialized = GameEventJson.encodeToString(GameEvent.serializer(), event)
                assertFalse("Resync vazou título", serialized.contains(chosenSong.title))
                assertFalse("Resync vazou artista", serialized.contains(chosenSong.artist))
            }
        }

    @Test
    fun `JoinRoom de playerId desconhecido nao gera resync`() =
        runTest {
            val engine = GameEngine(scope = backgroundScope)
            engine.setCatalog((1..8).map { song("s$it") })
            val room = FakeRoomSession()
            val coordinator = HostGameCoordinator(engine, room, backgroundScope)
            runCurrent()
            coordinator.startGame(GameConfig(players = fourPlayers(), roundCount = 5, stealEnabled = true))

            val roundStart = room.broadcasted.filterIsInstance<GameEvent.RoundStart>().single()
            val songOptions = room.sentTo.single().second as GameEvent.SongOptions
            val songId = songOptions.candidateSongIdsByCategory.values.first().first()
            room.emitIncoming(GameEvent.SongSelected(newId(), 0L, roundStart.roundId, "p1", songId))
            runCurrent()

            room.sentTo.clear()
            room.emitIncoming(GameEvent.JoinRoom(newId(), 0L, "intruso", "Intruso", room.roomCode))
            runCurrent()

            assertTrue(room.sentTo.none { it.first == "intruso" })
        }
}
