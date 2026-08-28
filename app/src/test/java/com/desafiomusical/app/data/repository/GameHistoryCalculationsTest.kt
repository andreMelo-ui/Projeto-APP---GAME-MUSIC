package com.desafiomusical.app.data.repository

import com.desafiomusical.app.data.room.entity.AttemptEntity
import com.desafiomusical.app.data.room.entity.GameEntity
import com.desafiomusical.app.data.room.entity.GamePlayerEntity
import com.desafiomusical.app.data.room.entity.PlayerEntity
import com.desafiomusical.app.data.room.entity.RoundEntity
import com.desafiomusical.app.data.room.entity.RoundScoreEntity
import com.desafiomusical.app.data.room.entity.SongEntity
import com.desafiomusical.app.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun testGame(
    id: String,
    createdAt: Long,
    winnerId: String?,
) = GameEntity(
    id = id,
    createdAt = createdAt,
    playerCount = 3,
    roundCount = 5,
    stealEnabled = true,
    winnerId = winnerId,
    finishedAt = createdAt + 60_000,
)

private fun testGamePlayer(
    gameId: String,
    playerId: String,
    seatOrder: Int,
    finalScore: Int,
) = GamePlayerEntity(
    gameId = gameId,
    playerId = playerId,
    seatOrder = seatOrder,
    finalScore = finalScore,
)

private fun testRound(
    id: String,
    gameId: String,
    roundNumber: Int,
    chooserId: String,
    responderId: String,
    songId: String,
    startedAt: Long,
    endedAt: Long?,
    winnerId: String?,
    hintsUsed: Int = 0,
) = RoundEntity(
    id = id,
    gameId = gameId,
    roundNumber = roundNumber,
    chooserId = chooserId,
    responderId = responderId,
    songId = songId,
    startedAt = startedAt,
    endedAt = endedAt,
    winnerId = winnerId,
    hintsUsed = hintsUsed,
)

private fun testAttempt(
    id: String,
    roundId: String,
    playerId: String,
) = AttemptEntity(
    id = id,
    roundId = roundId,
    playerId = playerId,
    attemptType = "STEAL_ANSWER",
    timestamp = 0L,
    correct = false,
    eliminated = true,
)

private fun testSong(
    id: String,
    category: Category,
) = SongEntity(
    id = id,
    title = "Song $id",
    artist = "Artist",
    category = category.name,
    work = null,
    difficulty = "FACIL",
    youtubeVideoId = "yt-$id",
    hint1 = "h1",
    hint2 = "h2",
    hint3 = "h3",
    tagsCsv = "",
    active = true,
    createdAt = 0L,
    updatedAt = 0L,
)

private fun testPlayer(
    id: String,
    name: String,
) = PlayerEntity(id = id, name = name, createdAt = 0L)

class GameHistoryCalculationsTest {
    private val song1Brasileira = testSong("song1", Category.BRASILEIRA)
    private val song2Games = testSong("song2", Category.GAMES)
    private val song3Popular = testSong("song3", Category.POPULAR)
    private val song4Brasileira = testSong("song4", Category.BRASILEIRA)
    private val song5Anime = testSong("song5", Category.ANIME)
    private val songsById =
        listOf(song1Brasileira, song2Games, song3Popular, song4Brasileira, song5Anime)
            .associateBy { it.id }

    /**
     * Cenário principal: p1 vence a rodada 1 roubando (não é chooser nem
     * responder), vence a rodada 2 como respondente principal, tenta e perde
     * um roubo na rodada 3, erra como respondente principal na rodada 4, e
     * está de fora da rodada 5 inteiramente.
     */
    private val gameARounds =
        listOf(
            testRound(
                id = "r1", gameId = "gameA", roundNumber = 1, chooserId = "p2", responderId = "p3",
                songId = song1Brasileira.id, startedAt = 0L, endedAt = 8_000L, winnerId = "p1",
            ),
            testRound(
                id = "r2", gameId = "gameA", roundNumber = 2, chooserId = "p3", responderId = "p1",
                songId = song2Games.id, startedAt = 10_000L, endedAt = 16_000L, winnerId = "p1",
            ),
            testRound(
                id = "r3", gameId = "gameA", roundNumber = 3, chooserId = "p2", responderId = "p3",
                songId = song3Popular.id, startedAt = 20_000L, endedAt = 27_000L, winnerId = null,
            ),
            testRound(
                id = "r4", gameId = "gameA", roundNumber = 4, chooserId = "p2", responderId = "p1",
                songId = song4Brasileira.id, startedAt = 30_000L, endedAt = 35_000L, winnerId = null, hintsUsed = 2,
            ),
            testRound(
                id = "r5", gameId = "gameA", roundNumber = 5, chooserId = "p3", responderId = "p2",
                songId = song5Anime.id, startedAt = 40_000L, endedAt = 44_000L, winnerId = "p3",
            ),
        )
    private val gameAAttempts = listOf(testAttempt(id = "a1", roundId = "r3", playerId = "p1"))

    @Test
    fun `no games played never divides by zero and returns a zeroed-out stats block`() {
        val stats =
            calculatePlayerAggregateStats(
                playerId = "p1",
                games = emptyList(),
                gamePlayerRows = emptyList(),
                rounds = emptyList(),
                attempts = emptyList(),
                songsById = emptyMap(),
            )

        assertEquals(0, stats.gamesPlayed)
        assertEquals(0.0, stats.winRate, 0.0)
        assertEquals(0.0, stats.averagePoints, 0.0)
        assertEquals(0, stats.bestScore)
        assertNull(stats.bestTimeSeconds)
        assertEquals(Category.concrete.size, stats.categoryBreakdown.size)
        assertTrue(stats.categoryBreakdown.all { it.songsAnswered == 0 && it.correctAnswers == 0 })
    }

    @Test
    fun `wins losses and win rate are computed across every game played`() {
        val games = listOf(testGame("gameA", createdAt = 1_000L, winnerId = "p1"), testGame("gameB", createdAt = 2_000L, winnerId = "p2"))
        val gamePlayerRows =
            listOf(
                testGamePlayer("gameA", "p1", seatOrder = 0, finalScore = 40),
                testGamePlayer("gameB", "p1", seatOrder = 1, finalScore = 90),
            )

        val stats = calculatePlayerAggregateStats("p1", games, gamePlayerRows, gameARounds, gameAAttempts, songsById)

        assertEquals(2, stats.gamesPlayed)
        assertEquals(1, stats.wins)
        assertEquals(1, stats.losses)
        assertEquals(0.5, stats.winRate, 0.0001)
    }

    @Test
    fun `best score and average points are taken across multiple games, not a single round`() {
        val games = listOf(testGame("gameA", createdAt = 1_000L, winnerId = "p1"), testGame("gameB", createdAt = 2_000L, winnerId = "p2"))
        val gamePlayerRows =
            listOf(
                testGamePlayer("gameA", "p1", seatOrder = 0, finalScore = 40),
                testGamePlayer("gameB", "p1", seatOrder = 1, finalScore = 90),
            )

        val stats = calculatePlayerAggregateStats("p1", games, gamePlayerRows, gameARounds, gameAAttempts, songsById)

        assertEquals(90, stats.bestScore)
        assertEquals(65.0, stats.averagePoints, 0.0001)
    }

    @Test
    fun `best time is the fastest round the player actually won`() {
        val games = listOf(testGame("gameA", createdAt = 1_000L, winnerId = "p1"))
        val gamePlayerRows = listOf(testGamePlayer("gameA", "p1", seatOrder = 0, finalScore = 40))

        val stats = calculatePlayerAggregateStats("p1", games, gamePlayerRows, gameARounds, gameAAttempts, songsById)

        // r1 levou 8s e r2 levou 6s — o roubo perdido (r3) e o erro (r4) não têm vencedor e não contam.
        assertEquals(6, stats.bestTimeSeconds)
    }

    @Test
    fun `steal attempts count both the win and the loss, but never a main-answer miss`() {
        val games = listOf(testGame("gameA", createdAt = 1_000L, winnerId = "p1"))
        val gamePlayerRows = listOf(testGamePlayer("gameA", "p1", seatOrder = 0, finalScore = 40))

        val stats = calculatePlayerAggregateStats("p1", games, gamePlayerRows, gameARounds, gameAAttempts, songsById)

        assertEquals(1, stats.stealsWon) // r1
        assertEquals(2, stats.stealsAttempted) // r1 (won) + r3 (perdido)
        assertEquals(2, stats.correctAnswers) // r1 + r2
        assertEquals(4, stats.songsAnswered) // respondente em r2/r4 (2) + roubos em r1/r3 (2)
        assertEquals(2, stats.hintsUsed) // só a rodada 4, onde p1 era o respondente principal
    }

    @Test
    fun `category breakdown matches exactly what was saved per category`() {
        val games = listOf(testGame("gameA", createdAt = 1_000L, winnerId = "p1"))
        val gamePlayerRows = listOf(testGamePlayer("gameA", "p1", seatOrder = 0, finalScore = 40))

        val stats = calculatePlayerAggregateStats("p1", games, gamePlayerRows, gameARounds, gameAAttempts, songsById)
        val byCategory = stats.categoryBreakdown.associateBy { it.category }

        // Brasileira: venceu roubando na r1 e errou como respondente na r4 -> 2 respondidas, 1 acerto.
        assertEquals(2, byCategory.getValue(Category.BRASILEIRA).songsAnswered)
        assertEquals(1, byCategory.getValue(Category.BRASILEIRA).correctAnswers)

        // Games: venceu como respondente principal na r2 -> 1 respondida, 1 acerto.
        assertEquals(1, byCategory.getValue(Category.GAMES).songsAnswered)
        assertEquals(1, byCategory.getValue(Category.GAMES).correctAnswers)

        // Popular: tentou e perdeu um roubo na r3 -> 1 respondida, 0 acertos.
        assertEquals(1, byCategory.getValue(Category.POPULAR).songsAnswered)
        assertEquals(0, byCategory.getValue(Category.POPULAR).correctAnswers)

        // Anime: p1 nem participou da r5 -> nada.
        assertEquals(0, byCategory.getValue(Category.ANIME).songsAnswered)
        assertEquals(0, byCategory.getValue(Category.ANIME).correctAnswers)

        // Categorias sem nenhuma rodada nesta partida continuam zeradas.
        assertEquals(0, byCategory.getValue(Category.INTERNACIONAL).songsAnswered)
        assertEquals(0, byCategory.getValue(Category.SERIES_MOVIES).songsAnswered)

        assertEquals(Category.concrete.toSet(), byCategory.keys)
    }

    @Test
    fun `game history entry sorts the scoreboard by score and resolves the winner`() {
        val game = testGame("gameA", createdAt = 1_000L, winnerId = "p1")
        val gamePlayers =
            listOf(
                testGamePlayer("gameA", "p1", seatOrder = 0, finalScore = 40),
                testGamePlayer("gameA", "p2", seatOrder = 1, finalScore = 70),
                testGamePlayer("gameA", "p3", seatOrder = 2, finalScore = 10),
            )
        val playersById = listOf(testPlayer("p1", "Andre"), testPlayer("p2", "Maria"), testPlayer("p3", "Joao")).associateBy { it.id }

        val entry = buildGameHistoryEntry(game.copy(winnerId = "p2"), gamePlayers, playersById)

        assertEquals(listOf("Maria", "Andre", "Joao"), entry.scoreboard.map { it.player.name })
        assertEquals(listOf(70, 40, 10), entry.scoreboard.map { it.totalScore })
        assertEquals("Maria", entry.winner?.name)
        assertEquals(5, entry.roundCount)
    }

    @Test
    fun `game history detail orders rounds by round number and fills in points and elapsed time`() {
        val game = testGame("gameA", createdAt = 1_000L, winnerId = "p1")
        val gamePlayers = listOf(testGamePlayer("gameA", "p1", seatOrder = 0, finalScore = 40))
        val playersById = listOf(testPlayer("p1", "Andre"), testPlayer("p3", "Joao")).associateBy { it.id }
        val entry = buildGameHistoryEntry(game, gamePlayers, playersById)
        val scoresByRoundId =
            mapOf(
                "r1" to
                    RoundScoreEntity(
                        "r1",
                        "p1",
                        timePoints = 10,
                        titlePoints = 5,
                        artistPoints = 5,
                        workPoints = 0,
                        hintPenalty = 0,
                        totalPoints = 20,
                    ),
            )

        // rounds passados fora de ordem de propósito
        val detail =
            buildGameHistoryDetail(
                entry = entry,
                rounds = gameARounds.reversed(),
                songsById = songsById,
                playersById = playersById,
                scoresByRoundId = scoresByRoundId,
            )

        assertEquals(listOf(1, 2, 3, 4, 5), detail.rounds.map { it.roundNumber })
        val round1 = detail.rounds.first { it.roundNumber == 1 }
        assertEquals(20, round1.pointsAwarded)
        assertEquals(8, round1.elapsedSeconds)
        assertEquals(song1Brasileira.id, round1.song.id)
        // Rodada sem vencedor (r3) não tem entrada em scoresByRoundId -> pontos ficam em 0, não quebra.
        val round3 = detail.rounds.first { it.roundNumber == 3 }
        assertEquals(0, round3.pointsAwarded)
        assertNull(round3.winner)
    }
}
