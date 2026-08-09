package com.desafiomusical.app.domain.usecase

import com.desafiomusical.app.domain.model.Category
import com.desafiomusical.app.domain.model.Song
import kotlin.random.Random

/**
 * Filtra e sorteia músicas do catálogo para o escolhedor, respeitando a
 * categoria selecionada (ou sorteando categoria + música quando "Aleatório")
 * e excluindo músicas já usadas na partida corrente.
 */
class SelectSongUseCase {

    fun candidatesFor(
        catalog: List<Song>,
        category: Category,
        usedSongIds: Set<String>,
        random: Random = Random.Default
    ): List<Song> {
        val active = catalog.filter { it.active && it.id !in usedSongIds }
        val pool = if (category == Category.ALEATORIO) active else active.filter { it.category == category }
        return pool.shuffled(random)
    }

    fun randomPick(
        catalog: List<Song>,
        category: Category,
        usedSongIds: Set<String>,
        random: Random = Random.Default
    ): Song? = candidatesFor(catalog, category, usedSongIds, random).firstOrNull()

    fun availableCategories(catalog: List<Song>, usedSongIds: Set<String>): List<Category> =
        Category.concrete.filter { candidatesFor(catalog, it, usedSongIds).isNotEmpty() }

    fun randomCategory(categories: List<Category>, random: Random = Random.Default): Category =
        categories[random.nextInt(categories.size)]
}
