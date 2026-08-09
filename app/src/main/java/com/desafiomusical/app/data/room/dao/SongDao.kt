package com.desafiomusical.app.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.desafiomusical.app.data.room.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

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
}
