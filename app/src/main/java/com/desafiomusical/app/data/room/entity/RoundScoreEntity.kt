package com.desafiomusical.app.data.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "round_scores",
    primaryKeys = ["roundId", "playerId"],
    foreignKeys = [
        ForeignKey(
            entity = RoundEntity::class,
            parentColumns = ["id"],
            childColumns = ["roundId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("roundId"), Index("playerId")]
)
data class RoundScoreEntity(
    val roundId: String,
    val playerId: String,
    val timePoints: Int,
    val titlePoints: Int,
    val artistPoints: Int,
    val hintPenalty: Int,
    val totalPoints: Int
)
