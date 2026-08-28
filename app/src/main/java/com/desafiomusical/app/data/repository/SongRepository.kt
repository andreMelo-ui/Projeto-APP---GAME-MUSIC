package com.desafiomusical.app.data.repository

import com.desafiomusical.app.data.mapper.toDomain
import com.desafiomusical.app.data.room.CatalogAssetSource
import com.desafiomusical.app.data.room.dao.SongDao
import com.desafiomusical.app.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SongRepository {
    fun observeCatalog(): Flow<List<Song>>

    suspend fun getCatalog(): List<Song>
}

class SongRepositoryImpl(
    private val songDao: SongDao,
    private val catalogAssetSource: CatalogAssetSource,
) : SongRepository {
    override fun observeCatalog(): Flow<List<Song>> = songDao.observeActive().map { entities -> entities.map { it.toDomain() } }

    // Sempre resincroniza com o asset antes de ler: cobre tanto a primeira
    // leitura logo após o install (o seed do RoomDatabase.Callback.onCreate
    // roda numa coroutine separada e pode não ter terminado ainda) quanto
    // reinstalações em cima de um banco antigo, cujo catalog.json mudou
    // desde então (novas músicas, IDs do YouTube preenchidos) — sem isso, o
    // Room nunca resincroniza sozinho depois da primeira vez.
    // upsertAll é idempotente (REPLACE por id), então repetir é seguro e
    // nunca apaga músicas que tenham sido removidas do JSON.
    override suspend fun getCatalog(): List<Song> {
        songDao.upsertAll(catalogAssetSource.loadCatalog())
        return songDao.getActiveCatalog().map { it.toDomain() }
    }
}
