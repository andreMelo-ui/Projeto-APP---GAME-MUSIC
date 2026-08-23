package com.desafiomusical.app.data.repository

import com.desafiomusical.app.data.mapper.toDomain
import com.desafiomusical.app.data.mapper.toEntity
import com.desafiomusical.app.data.room.dao.PlayerDao
import com.desafiomusical.app.domain.model.Player
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PlayerRepository {
    fun observeKnownPlayers(): Flow<List<Player>>
    suspend fun saveIfNew(players: List<Player>)

    /**
     * Reaproveita o [Player] já salvo com esse nome, ou cria um novo (com id
     * novo) se for a primeira vez que esse nome aparece. É o que permite as
     * estatísticas agregadas (seção 22) enxergarem partidas antigas da mesma
     * pessoa — sem isso, cada partida gerava um id novo e
     * [GameHistoryRepository.getPlayerStats] nunca via mais de uma partida por
     * jogador.
     *
     * A comparação de nome ignora maiúsculas/minúsculas E acentos ("andré" e
     * "André" são a mesma pessoa) — feita em Kotlin com
     * `equals(ignoreCase = true)`, não em SQL: o `COLLATE NOCASE` do SQLite só
     * dobra `A-Z`, então "andré"/"André" cairiam como jogadores diferentes se a
     * comparação fosse feita no banco.
     *
     * Limitação conhecida e aceita: o app não tem contas de verdade, então
     * nome igual = mesma "identidade" pro app. Duas pessoas reais com o mesmo
     * nome (ex.: irmãos "João") compartilham as mesmas estatísticas.
     */
    suspend fun findOrCreatePlayer(name: String): Player
}

class PlayerRepositoryImpl(private val playerDao: PlayerDao) : PlayerRepository {

    override fun observeKnownPlayers(): Flow<List<Player>> =
        playerDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveIfNew(players: List<Player>) {
        playerDao.upsertAll(players.map { it.toEntity() })
    }

    override suspend fun findOrCreatePlayer(name: String): Player {
        val trimmedName = name.trim()
        val existing = playerDao.getAllOnce().firstOrNull { it.name.equals(trimmedName, ignoreCase = true) }
        if (existing != null) return existing.toDomain()

        val created = Player(id = UUID.randomUUID().toString(), name = trimmedName, createdAt = System.currentTimeMillis())
        playerDao.upsert(created.toEntity())
        return created
    }
}
