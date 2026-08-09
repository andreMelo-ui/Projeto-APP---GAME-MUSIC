package com.desafiomusical.app.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.desafiomusical.app.data.room.entity.GameEntity
import com.desafiomusical.app.data.room.entity.GamePlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGame(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGamePlayers(players: List<GamePlayerEntity>)

    @Query("UPDATE games SET winnerId = :winnerId, finishedAt = :finishedAt WHERE id = :gameId")
    suspend fun finishGame(gameId: String, winnerId: String?, finishedAt: Long)

    @Transaction
    suspend fun saveNewGame(game: GameEntity, players: List<GamePlayerEntity>) {
        upsertGame(game)
        upsertGamePlayers(players)
    }

    @Query("SELECT * FROM games ORDER BY createdAt DESC")
    fun observeHistory(): Flow<List<GameEntity>>

    @Query("SELECT * FROM game_players WHERE gameId = :gameId")
    suspend fun getPlayersForGame(gameId: String): List<GamePlayerEntity>
}
