package com.desafiomusical.app.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.desafiomusical.app.data.room.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringConflicts(songs: List<SongEntity>): List<Long>

    @Update
    suspend fun update(songs: List<SongEntity>)

    /**
     * Upsert de verdade (insere o que é novo, atualiza o que já existe) em vez de
     * `@Insert(onConflict = REPLACE)`: REPLACE faz um DELETE seguido de INSERT por
     * baixo dos panos, e isso viola a FK `RESTRICT` de [com.desafiomusical.app.data.room.entity.RoundEntity]
     * assim que alguma partida no histórico já referenciar aquele `songId` — trava o
     * app com `SQLiteConstraintException` ao (re)sincronizar o catálogo com uma
     * partida salva. `@Insert(IGNORE)` + `@Update` nunca apaga a linha existente.
     */
    @Transaction
    suspend fun upsertAll(songs: List<SongEntity>) {
        val insertResults = insertIgnoringConflicts(songs)
        val alreadyExisting = songs.filterIndexed { index, _ -> insertResults[index] == -1L }
        if (alreadyExisting.isNotEmpty()) update(alreadyExisting)
    }

    @Query("SELECT * FROM songs WHERE active = 1 ORDER BY title ASC")
    fun observeActive(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE active = 1")
    suspend fun getActiveCatalog(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE active = 1 AND category = :category")
    suspend fun getActiveByCategory(category: String): List<SongEntity>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: String): SongEntity?

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<SongEntity>
}
