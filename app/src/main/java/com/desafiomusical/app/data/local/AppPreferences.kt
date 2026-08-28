package com.desafiomusical.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "desafio_musical_prefs")

/**
 * Preferências locais leves (fora do escopo do Room): últimos nomes de
 * jogadores usados e a última escolha de Roubo ON/OFF, para agilizar a
 * criação de uma nova partida.
 */
class AppPreferences(private val context: Context) {
    private object Keys {
        val LAST_PLAYER_NAMES = stringPreferencesKey("last_player_names")
        val STEAL_ENABLED_DEFAULT = booleanPreferencesKey("steal_enabled_default")
    }

    val lastPlayerNames: Flow<List<String>> =
        context.dataStore.data.map { prefs ->
            prefs[Keys.LAST_PLAYER_NAMES]?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
        }

    val stealEnabledDefault: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[Keys.STEAL_ENABLED_DEFAULT] ?: true
        }

    suspend fun saveLastSetup(
        playerNames: List<String>,
        stealEnabled: Boolean,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_PLAYER_NAMES] = playerNames.joinToString("|")
            prefs[Keys.STEAL_ENABLED_DEFAULT] = stealEnabled
        }
    }
}
