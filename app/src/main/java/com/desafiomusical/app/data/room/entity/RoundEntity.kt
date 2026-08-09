package com.desafiomusical.app.data.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rounds",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("gameId"), Index("songId")]
)
data class RoundEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val roundNumber: Int,
    val chooserId: String,
    val responderId: String,
    val songId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val winnerId: String?,
    val hintsUsed: Int
)
