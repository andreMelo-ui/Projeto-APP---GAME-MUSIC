package com.desafiomusical.app.data.repository

import com.desafiomusical.app.data.mapper.toDomain
import com.desafiomusical.app.data.room.InitialSongCatalog
import com.desafiomusical.app.data.room.dao.SongDao
import com.desafiomusical.app.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SongRepository {
    fun observeCatalog(): Flow<List<Song>>
    suspend fun getCatalog(): List<Song>
}

class SongRepositoryImpl(private val songDao: SongDao) : SongRepository {

    override fun observeCatalog(): Flow<List<Song>> =
        songDao.observeActive().map { entities -> entities.map { it.toDomain() } }

    // O seed inicial (RoomDatabase.Callback.onCreate) roda em uma coroutine separada
    // e pode não ter terminado ainda na primeira leitura (ex.: usuário navega rápido
    // para "Nova Partida" logo após o primeiro install). Semeia sob demanda para não
    // depender dessa corrida.
    override suspend fun getCatalog(): List<Song> {
        var entities = songDao.getActiveCatalog()
        if (entities.isEmpty()) {
            songDao.upsertAll(InitialSongCatalog.songs)
            entities = songDao.getActiveCatalog()
        }
        return entities.map { it.toDomain() }
    }
}
