package com.desafiomusical.app.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.desafiomusical.app.data.room.entity.RoundEntity
import com.desafiomusical.app.data.room.entity.RoundScoreEntity

@Dao
interface RoundDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRound(round: RoundEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScores(scores: List<RoundScoreEntity>)

    @Query("SELECT * FROM rounds WHERE gameId = :gameId ORDER BY roundNumber ASC")
    suspend fun getRoundsForGame(gameId: String): List<RoundEntity>

    @Query("SELECT * FROM round_scores WHERE roundId = :roundId")
    suspend fun getScoresForRound(roundId: String): List<RoundScoreEntity>

    @Query("SELECT * FROM rounds WHERE gameId IN (:gameIds) ORDER BY gameId, roundNumber ASC")
    suspend fun getRoundsForGames(gameIds: List<String>): List<RoundEntity>

    @Query("SELECT * FROM round_scores WHERE roundId IN (:roundIds)")
    suspend fun getScoresForRounds(roundIds: List<String>): List<RoundScoreEntity>
}
