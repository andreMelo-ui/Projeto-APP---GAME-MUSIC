package com.desafiomusical.app.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.desafiomusical.app.data.room.entity.AttemptEntity

@Dao
interface AttemptDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(attempt: AttemptEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(attempts: List<AttemptEntity>)

    @Query("SELECT * FROM attempts WHERE roundId = :roundId ORDER BY timestamp ASC")
    suspend fun getForRound(roundId: String): List<AttemptEntity>

    @Query("SELECT * FROM attempts WHERE playerId = :playerId")
    suspend fun getForPlayer(playerId: String): List<AttemptEntity>
}
