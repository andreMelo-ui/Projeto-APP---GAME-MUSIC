package com.desafiomusical.app.data.repository

import com.desafiomusical.app.data.mapper.toAttemptEntities
import com.desafiomusical.app.data.mapper.toGameEntity
import com.desafiomusical.app.data.mapper.toGamePlayerEntities
import com.desafiomusical.app.data.mapper.toRoundEntities
import com.desafiomusical.app.data.mapper.toRoundScoreEntities
import com.desafiomusical.app.data.room.dao.AttemptDao
import com.desafiomusical.app.data.room.dao.GameDao
import com.desafiomusical.app.data.room.dao.RoundDao
import com.desafiomusical.app.domain.model.GameSnapshot

interface GameHistoryRepository {
    suspend fun saveFinishedGame(snapshot: GameSnapshot)
}

class GameHistoryRepositoryImpl(
    private val gameDao: GameDao,
    private val roundDao: RoundDao,
    private val attemptDao: AttemptDao
) : GameHistoryRepository {

    override suspend fun saveFinishedGame(snapshot: GameSnapshot) {
        gameDao.saveNewGame(snapshot.toGameEntity(), snapshot.toGamePlayerEntities())
        snapshot.toRoundEntities().forEach { roundDao.upsertRound(it) }
        roundDao.upsertScores(snapshot.toRoundScoreEntities())
        attemptDao.insertAll(snapshot.toAttemptEntities())
    }
}
