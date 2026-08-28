package com.desafiomusical.app.domain.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val category: Category,
    val work: String?,
    val difficulty: Difficulty,
    val youtubeVideoId: String,
    val hint1: String,
    val hint2: String,
    val hint3: String,
    val tags: List<String>,
    val active: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun hintFor(level: Int): String =
        when (level) {
            1 -> hint1
            2 -> hint2
            3 -> hint3
            else -> error("Nível de dica inválido: $level")
        }
}

/**
 * Versão da música sem título/artista/obra — a única forma que deve trafegar
 * para clientes que não são o escolhedor (respondentes, outros jogadores).
 */
data class MaskedSong(
    val id: String,
    val category: Category,
    val difficulty: Difficulty,
    val youtubeVideoId: String,
    val hintsRevealed: List<String>,
    val hasWork: Boolean,
)

fun Song.mask(hintsRevealedCount: Int): MaskedSong =
    MaskedSong(
        id = id,
        category = category,
        difficulty = difficulty,
        youtubeVideoId = youtubeVideoId,
        hintsRevealed = (1..hintsRevealedCount).map { hintFor(it) },
        hasWork = !work.isNullOrBlank(),
    )
