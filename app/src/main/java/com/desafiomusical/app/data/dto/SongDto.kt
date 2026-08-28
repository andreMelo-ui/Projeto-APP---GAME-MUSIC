package com.desafiomusical.app.data.dto

import com.desafiomusical.app.data.room.entity.SongEntity
import kotlinx.serialization.Serializable

/**
 * Formato de transporte de uma música vinda de um catálogo remoto (V2 —
 * ver roadmap). Mantido separado de [SongEntity] para que mudanças no
 * schema local não quebrem a compatibilidade com o payload remoto.
 */
@Serializable
data class SongDto(
    val id: String,
    val title: String,
    val artist: String,
    val category: String,
    val work: String? = null,
    val difficulty: String,
    val youtubeVideoId: String,
    val hint1: String,
    val hint2: String,
    val hint3: String,
    val tags: List<String> = emptyList(),
    val active: Boolean = true,
)

fun SongDto.toEntity(now: Long): SongEntity =
    SongEntity(
        id = id,
        title = title,
        artist = artist,
        category = category,
        work = work,
        difficulty = difficulty,
        youtubeVideoId = youtubeVideoId,
        hint1 = hint1,
        hint2 = hint2,
        hint3 = hint3,
        tagsCsv = tags.joinToString(","),
        active = active,
        createdAt = now,
        updatedAt = now,
    )
