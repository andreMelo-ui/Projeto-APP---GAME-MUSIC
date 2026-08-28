package com.desafiomusical.app.data.repository

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.desafiomusical.app.data.room.DesafioMusicalDatabase
import com.desafiomusical.app.data.room.entity.SongEntity
import com.desafiomusical.app.domain.model.Category
import com.desafiomusical.app.domain.model.Difficulty
import com.desafiomusical.app.domain.model.GameConfig
import com.desafiomusical.app.domain.model.GameSnapshot
import com.desafiomusical.app.domain.model.Player
import com.desafiomusical.app.domain.model.PlayerScore
import com.desafiomusical.app.domain.model.RoundOutcome
import com.desafiomusical.app.domain.model.ScoreBreakdown
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Regressão da família de bugs "REPLACE/linha-faltando quebra o histórico".
 * Todos exercitam o código real de repositório contra um Room de verdade em
 * memória (Robolectric) — nada de fake/mock de DAO:
 *
 *  1. PR #5 (commit 7dd7565): um jogador que só entrou pela rede não tinha
 *     linha em `players` no host, então o insert em `game_players` ao salvar
 *     o histórico falhava por foreign key.
 *  2. #3 (SongDao): `@Insert(REPLACE)` fazia DELETE+INSERT e batia no
 *     `onDelete = RESTRICT` de `rounds.songId` assim que uma partida salva
 *     referenciava aquela música — travava ao (re)sincronizar o catálogo.
 *  3. PlayerDao: o mesmo `@Insert(REPLACE)` dispararia o `onDelete = CASCADE`
 *     de `game_players.playerId` e apagaria, em silêncio, o vínculo de um
 *     jogador recorrente com as partidas antigas dele.
 *
 * Roda em `src/test` (task `testDebugUnitTest`) via Robolectric, de propósito,
 * para não depender de emulador/dispositivo conectado.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class HistoryPersistenceRegressionTest {
    private lateinit var db: DesafioMusicalDatabase
    private lateinit var playerRepository: PlayerRepositoryImpl
    private lateinit var historyRepository: GameHistoryRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, DesafioMusicalDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        playerRepository = PlayerRepositoryImpl(db.playerDao())
        historyRepository =
            GameHistoryRepositoryImpl(
                gameDao = db.gameDao(),
                roundDao = db.roundDao(),
                attemptDao = db.attemptDao(),
                playerDao = db.playerDao(),
                songDao = db.songDao(),
            )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // --- fixtures --------------------------------------------------------------

    private fun song(
        id: String,
        title: String,
        category: Category,
    ): SongEntity =
        SongEntity(
            id = id,
            title = title,
            artist = "Artista $id",
            category = category.name,
            work = null,
            difficulty = Difficulty.FACIL.name,
            youtubeVideoId = "yt-$id",
            hint1 = "d1",
            hint2 = "d2",
            hint3 = "d3",
            tagsCsv = "",
            active = true,
            createdAt = 0L,
            updatedAt = 0L,
        )

    /**
     * Espelha o que `HostLobbyViewModel.startGame` monta a partir do roster do
     * lobby: 2 rodadas, cada jogador vence uma, o host termina na frente.
     */
    private fun twoRoundSnapshot(
        gameId: String,
        host: Player,
        guest: Player,
    ): GameSnapshot {
        val rounds =
            listOf(
                RoundOutcome(
                    roundNumber = 1,
                    chooserId = host.id,
                    mainResponderId = guest.id,
                    songId = "s1",
                    winnerId = host.id,
                    startedAt = 1_000L,
                    endedAt = 9_000L,
                    scores =
                        listOf(
                            ScoreBreakdown(host.id, timePoints = 10, titlePoints = 5, artistPoints = 5, workPoints = 0, hintPenalty = 0),
                            ScoreBreakdown(guest.id, timePoints = 0, titlePoints = 0, artistPoints = 0, workPoints = 0, hintPenalty = 0),
                        ),
                    hintsUsed = 0,
                    eliminatedIds = emptyList(),
                ),
                RoundOutcome(
                    roundNumber = 2,
                    chooserId = guest.id,
                    mainResponderId = host.id,
                    songId = "s2",
                    winnerId = guest.id,
                    startedAt = 10_000L,
                    endedAt = 18_000L,
                    scores =
                        listOf(
                            ScoreBreakdown(guest.id, timePoints = 10, titlePoints = 5, artistPoints = 5, workPoints = 0, hintPenalty = 0),
                            ScoreBreakdown(host.id, timePoints = 0, titlePoints = 0, artistPoints = 0, workPoints = 0, hintPenalty = 0),
                        ),
                    hintsUsed = 1,
                    eliminatedIds = emptyList(),
                ),
            )
        return GameSnapshot(
            gameId = gameId,
            config = GameConfig(players = listOf(host, guest), roundCount = 5, stealEnabled = true),
            rounds = rounds,
            finalScoreboard =
                listOf(
                    PlayerScore(player = host, totalScore = 30),
                    PlayerScore(player = guest, totalScore = 20),
                ),
            winner = host,
        )
    }

    private fun seedTwoSongs() =
        runBlocking {
            db.songDao().upsertAll(
                listOf(
                    song("s1", "Música 1", Category.BRASILEIRA),
                    song("s2", "Música 2", Category.GAMES),
                ),
            )
        }

    // --- 1. PR #5: identidade do jogador remoto no host ----------------------

    @Test
    fun `host registers the network-only player so saving history does not trip the game_players foreign key`() =
        runBlocking {
            seedTwoSongs()

            // Host abriu "Criar Sala" -> identidade local por nome.
            val host = playerRepository.findOrCreatePlayer("Host")

            // Convidado entrou só pela rede. Replica o branch `GameEvent.JoinRoom`
            // de `HostLobbyViewModel.observeIncoming` (commit 7dd7565): o id já vem
            // decidido pelo aparelho remoto e o host só registra essa identidade.
            val guestId = UUID.randomUUID().toString()
            playerRepository.upsertRemotePlayer(guestId, "Convidado")
            val guest = Player(id = guestId, name = "Convidado", createdAt = 0L)

            historyRepository.saveFinishedGame(twoRoundSnapshot("game-mp", host, guest))

            val savedById = db.gameDao().getPlayersForGame("game-mp").associate { it.playerId to it.finalScore }
            assertEquals(setOf(host.id, guestId), savedById.keys)
            assertEquals(30, savedById.getValue(host.id))
            assertEquals(20, savedById.getValue(guestId))

            // Passo 2d do teste manual: o histórico no host mostra os dois nomes,
            // com o vencedor certo e cada rodada atribuída a quem a venceu.
            val detail = historyRepository.getGameDetail("game-mp")
            assertNotNull(detail)
            assertEquals(setOf("Host", "Convidado"), detail!!.entry.scoreboard.map { it.player.name }.toSet())
            assertEquals("Host", detail.entry.winner?.name)
            assertEquals(listOf("Host", "Convidado"), detail.rounds.map { it.winner?.name })

            // Passo 2e: estatísticas de cada jogador batem com esta partida.
            val hostStats = historyRepository.getPlayerStats(host.id)
            val guestStats = historyRepository.getPlayerStats(guestId)
            assertEquals(1, hostStats.gamesPlayed)
            assertEquals(1, hostStats.wins)
            assertEquals(1, guestStats.gamesPlayed)
            assertEquals(0, guestStats.wins)
            assertEquals(1, guestStats.losses)
        }

    @Test
    fun `without registering the network-only player the game_players insert fails - proves the fix is load-bearing`() {
        seedTwoSongs()
        val host = runBlocking { playerRepository.findOrCreatePlayer("Host") }
        val guest = Player(id = UUID.randomUUID().toString(), name = "Convidado", createdAt = 0L)
        // Sem `playerRepository.upsertRemotePlayer(...)`: é exatamente o estado
        // pré-PR #5 — o convidado nunca virou linha em `players` no host.

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { historyRepository.saveFinishedGame(twoRoundSnapshot("game-mp", host, guest)) }
        }
    }

    // --- 2. SongDao (#3): re-sync de catálogo com partida salva -------------

    @Test
    fun `re-syncing the catalog updates a song referenced by saved history instead of crashing on the RESTRICT foreign key`() =
        runBlocking {
            db.songDao().upsertAll(
                listOf(
                    song("s1", "Título Antigo", Category.BRASILEIRA),
                    song("s2", "Música 2", Category.GAMES),
                ),
            )
            val host = playerRepository.findOrCreatePlayer("Host")
            val guestId = UUID.randomUUID().toString()
            playerRepository.upsertRemotePlayer(guestId, "Convidado")
            val guest = Player(id = guestId, name = "Convidado", createdAt = 0L)
            historyRepository.saveFinishedGame(twoRoundSnapshot("game-mp", host, guest))

            // `SongRepositoryImpl.getCatalog` re-sincroniza o catálogo a cada
            // partida. `@Insert(REPLACE)` faria DELETE+INSERT da MESMA `s1` e o
            // DELETE bateria no `onDelete = RESTRICT` de `rounds.songId`.
            db.songDao().upsertAll(listOf(song("s1", "Título Novo", Category.BRASILEIRA)))

            assertEquals("Título Novo", db.songDao().getById("s1")?.title)
            assertEquals(2, db.roundDao().getRoundsForGame("game-mp").size)
        }

    // --- 3. PlayerDao: jogador recorrente regravado ------------------------

    @Test
    fun `re-upserting a returning player keeps their existing game_players links`() =
        runBlocking {
            seedTwoSongs()
            val host = playerRepository.findOrCreatePlayer("Host")
            val guestId = UUID.randomUUID().toString()
            playerRepository.upsertRemotePlayer(guestId, "Convidado")
            val guest = Player(id = guestId, name = "Convidado", createdAt = 0L)
            historyRepository.saveFinishedGame(twoRoundSnapshot("game-mp", host, guest))

            // Host abre outra sala: `findOrCreatePlayer` reaproveita o id e a linha
            // em `players` é regravada. `@Insert(REPLACE)` dispararia o
            // `onDelete = CASCADE` de `game_players` e apagaria o vínculo com "game-mp".
            val hostAgain = playerRepository.findOrCreatePlayer("Host")
            assertEquals(host.id, hostAgain.id)
            playerRepository.saveIfNew(listOf(hostAgain))

            assertTrue(db.gameDao().getGamePlayersForPlayer(host.id).isNotEmpty())
            assertEquals(1, historyRepository.getPlayerStats(host.id).gamesPlayed)
        }

    // --- 4. reconexão no lobby não reordena o roster ---------------------

    @Test
    fun `upsertRemotePlayer is a no-op when the id already exists so lobby reconnects do not rewrite the row`() =
        runBlocking {
            val guestId = UUID.randomUUID().toString()
            playerRepository.upsertRemotePlayer(guestId, "Convidado")
            val firstCreatedAt = db.playerDao().getById(guestId)!!.createdAt

            playerRepository.upsertRemotePlayer(guestId, "Convidado (renomeado)")
            val row = db.playerDao().getById(guestId)!!

            assertEquals("Convidado", row.name)
            assertEquals(firstCreatedAt, row.createdAt)
        }
}
