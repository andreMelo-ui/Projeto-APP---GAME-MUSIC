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
    suspend fun finishGame(
        gameId: String,
        winnerId: String?,
        finishedAt: Long,
    )

    @Transaction
    suspend fun saveNewGame(
        game: GameEntity,
        players: List<GamePlayerEntity>,
    ) {
        upsertGame(game)
        upsertGamePlayers(players)
    }

    @Query("SELECT * FROM games ORDER BY createdAt DESC")
    fun observeHistory(): Flow<List<GameEntity>>

    @Query("SELECT * FROM game_players WHERE gameId = :gameId")
    suspend fun getPlayersForGame(gameId: String): List<GamePlayerEntity>

    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    suspend fun getGameById(gameId: String): GameEntity?

    @Query(
        """
        SELECT games.* FROM games
        INNER JOIN game_players ON game_players.gameId = games.id
        WHERE game_players.playerId = :playerId
        ORDER BY games.createdAt DESC
        """,
    )
    suspend fun getGamesForPlayer(playerId: String): List<GameEntity>

    @Query("SELECT * FROM game_players WHERE playerId = :playerId")
    suspend fun getGamePlayersForPlayer(playerId: String): List<GamePlayerEntity>
}
