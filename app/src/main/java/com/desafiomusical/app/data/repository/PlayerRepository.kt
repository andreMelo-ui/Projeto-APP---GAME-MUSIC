package com.desafiomusical.app.data.repository

import com.desafiomusical.app.data.mapper.toDomain
import com.desafiomusical.app.data.mapper.toEntity
import com.desafiomusical.app.data.room.dao.PlayerDao
import com.desafiomusical.app.domain.model.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PlayerRepository {
    fun observeKnownPlayers(): Flow<List<Player>>
    suspend fun saveIfNew(players: List<Player>)
}

class PlayerRepositoryImpl(private val playerDao: PlayerDao) : PlayerRepository {

    override fun observeKnownPlayers(): Flow<List<Player>> =
        playerDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveIfNew(players: List<Player>) {
        playerDao.upsertAll(players.map { it.toEntity() })
    }
}
