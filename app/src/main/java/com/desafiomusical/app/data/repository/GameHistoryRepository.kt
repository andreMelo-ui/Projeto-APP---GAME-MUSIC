package com.desafiomusical.app.data.repository

import com.desafiomusical.app.data.mapper.toAttemptEntities
import com.desafiomusical.app.data.mapper.toGameEntity
import com.desafiomusical.app.data.mapper.toGamePlayerEntities
import com.desafiomusical.app.data.mapper.toRoundEntities
import com.desafiomusical.app.data.mapper.toRoundScoreEntities
import com.desafiomusical.app.data.room.dao.AttemptDao
import com.desafiomusical.app.data.room.dao.GameDao
import com.desafiomusical.app.data.room.dao.PlayerDao
import com.desafiomusical.app.data.room.dao.RoundDao
import com.desafiomusical.app.data.room.dao.SongDao
import com.desafiomusical.app.data.room.entity.GameEntity
import com.desafiomusical.app.data.room.entity.PlayerEntity
import com.desafiomusical.app.domain.model.GameHistoryDetail
import com.desafiomusical.app.domain.model.GameHistoryEntry
import com.desafiomusical.app.domain.model.GameSnapshot
import com.desafiomusical.app.domain.model.PlayerAggregateStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface GameHistoryRepository {
    suspend fun saveFinishedGame(snapshot: GameSnapshot)

    /** Lista o histórico de partidas encerradas, mais recente primeiro. */
    fun observeHistory(): Flow<List<GameHistoryEntry>>

    /** Detalhe completo (rodada a rodada) de uma partida, ou `null` se [gameId] não existir. */
    suspend fun getGameDetail(gameId: String): GameHistoryDetail?

    /** Estatísticas agregadas de [playerId] em todo o histórico salvo (seção 22 da especificação). */
    suspend fun getPlayerStats(playerId: String): PlayerAggregateStats
}

class GameHistoryRepositoryImpl(
    private val gameDao: GameDao,
    private val roundDao: RoundDao,
    private val attemptDao: AttemptDao,
    private val playerDao: PlayerDao,
    private val songDao: SongDao
) : GameHistoryRepository {

    override suspend fun saveFinishedGame(snapshot: GameSnapshot) {
        gameDao.saveNewGame(snapshot.toGameEntity(), snapshot.toGamePlayerEntities())
        snapshot.toRoundEntities().forEach { roundDao.upsertRound(it) }
        roundDao.upsertScores(snapshot.toRoundScoreEntities())
        attemptDao.insertAll(snapshot.toAttemptEntities())
    }

    override fun observeHistory(): Flow<List<GameHistoryEntry>> =
        gameDao.observeHistory().map { games -> games.map { buildEntry(it) } }

    override suspend fun getGameDetail(gameId: String): GameHistoryDetail? {
        val game = gameDao.getGameById(gameId) ?: return null
        val entry = buildEntry(game)
        val rounds = roundDao.getRoundsForGame(gameId)
        val songsById = songDao.getByIds(rounds.map { it.songId }.distinct()).associateBy { it.id }
        val playersById = playersById(gameId)
        val scoresByRoundId = roundDao.getScoresForRounds(rounds.map { it.id }).associateBy { it.roundId }
        return buildGameHistoryDetail(entry, rounds, songsById, playersById, scoresByRoundId)
    }

    override suspend fun getPlayerStats(playerId: String): PlayerAggregateStats {
        val games = gameDao.getGamesForPlayer(playerId)
        if (games.isEmpty()) return PlayerAggregateStats.empty(playerId)
        val gamePlayerRows = gameDao.getGamePlayersForPlayer(playerId)
        val rounds = roundDao.getRoundsForGames(games.map { it.id })
        val attempts = attemptDao.getForPlayer(playerId)
        val songsById = songDao.getByIds(rounds.map { it.songId }.distinct()).associateBy { it.id }
        return calculatePlayerAggregateStats(playerId, games, gamePlayerRows, rounds, attempts, songsById)
    }

    private suspend fun buildEntry(game: GameEntity): GameHistoryEntry =
        buildGameHistoryEntry(game, gameDao.getPlayersForGame(game.id), playersById(game.id))

    private suspend fun playersById(gameId: String): Map<String, PlayerEntity> =
        gameDao.getPlayersForGame(gameId).mapNotNull { playerDao.getById(it.playerId) }.associateBy { it.id }
}
