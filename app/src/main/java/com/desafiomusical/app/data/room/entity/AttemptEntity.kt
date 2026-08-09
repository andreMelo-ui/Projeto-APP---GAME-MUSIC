package com.desafiomusical.app.data.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attempts",
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
data class AttemptEntity(
    @PrimaryKey val id: String,
    val roundId: String,
    val playerId: String,
    val attemptType: String,
    val timestamp: Long,
    val correct: Boolean,
    val eliminated: Boolean
)
