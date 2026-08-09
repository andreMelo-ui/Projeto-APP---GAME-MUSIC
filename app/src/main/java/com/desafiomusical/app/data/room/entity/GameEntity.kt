package com.desafiomusical.app.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val playerCount: Int,
    val roundCount: Int,
    val stealEnabled: Boolean,
    val winnerId: String?,
    val finishedAt: Long?
)
