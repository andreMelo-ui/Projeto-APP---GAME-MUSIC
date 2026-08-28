package com.desafiomusical.app.domain.model

enum class Category(val displayName: String) {
    BRASILEIRA("Brasileira"),
    INTERNACIONAL("Internacional"),
    POPULAR("Popular"),
    GAMES("Games"),
    SERIES_MOVIES("Séries/Filmes"),
    ANIME("Anime"),
    ALEATORIO("Aleatório"),
    ;

    companion object {
        val selectable: List<Category> = entries
        val concrete: List<Category> = entries.filter { it != ALEATORIO }
    }
}
