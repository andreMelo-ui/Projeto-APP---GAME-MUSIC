package com.desafiomusical.app.data.room

import android.content.Context
import com.desafiomusical.app.data.dto.SongDto
import com.desafiomusical.app.data.dto.toEntity
import com.desafiomusical.app.data.room.entity.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Lê o catálogo inicial de `assets/catalog.json`. Mantido separado do banco
 * para que o catálogo possa ser editado (ou substituído por um download
 * remoto, no roadmap V2) sem tocar em código Kotlin — só o JSON muda.
 */
class CatalogAssetSource(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadCatalog(fileName: String = "catalog.json"): List<SongEntity> =
        withContext(Dispatchers.IO) {
            val text = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val dtos = json.decodeFromString<List<SongDto>>(text)
            val now = System.currentTimeMillis()
            dtos.map { it.toEntity(now) }
        }
}
