package com.desafiomusical.app.data.mapper

import com.desafiomusical.app.data.room.entity.SongEntity
import com.desafiomusical.app.domain.model.Category
import com.desafiomusical.app.domain.model.Difficulty
import com.desafiomusical.app.domain.model.Song

fun SongEntity.toDomain(): Song =
    Song(
        id = id,
        title = title,
        artist = artist,
        category = Category.valueOf(category),
        work = work,
        difficulty = Difficulty.valueOf(difficulty),
        youtubeVideoId = youtubeVideoId,
        hint1 = hint1,
        hint2 = hint2,
        hint3 = hint3,
        tags = if (tagsCsv.isBlank()) emptyList() else tagsCsv.split(",").map { it.trim() },
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Song.toEntity(): SongEntity =
    SongEntity(
        id = id,
        title = title,
        artist = artist,
        category = category.name,
        work = work,
        difficulty = difficulty.name,
        youtubeVideoId = youtubeVideoId,
        hint1 = hint1,
        hint2 = hint2,
        hint3 = hint3,
        tagsCsv = tags.joinToString(","),
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
