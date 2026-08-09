package com.desafiomusical.app.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val category: String,
    val work: String?,
    val difficulty: String,
    val youtubeVideoId: String,
    val hint1: String,
    val hint2: String,
    val hint3: String,
    val tagsCsv: String,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
