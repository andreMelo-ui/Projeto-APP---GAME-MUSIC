package com.desafiomusical.app.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.desafiomusical.app.data.room.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringConflicts(players: List<PlayerEntity>): List<Long>

    @Update
    suspend fun update(players: List<PlayerEntity>)

    /**
     * Upsert de verdade (insere o que é novo, atualiza o que já existe) em vez de
     * `@Insert(onConflict = REPLACE)`: REPLACE faz um DELETE seguido de INSERT por
     * baixo dos panos, e isso dispara o `onDelete = CASCADE` de
     * [com.desafiomusical.app.data.room.entity.GamePlayerEntity] — apagaria, em
     * silêncio, o vínculo desse jogador com TODAS as partidas anteriores dele
     * sempre que ele jogasse de novo, exatamente o cenário que passamos a
     * habilitar ao reaproveitar o id de um jogador existente (ver
     * [com.desafiomusical.app.data.repository.PlayerRepository.findOrCreatePlayer]).
     * Mesmo raciocínio do fix em `SongDao.upsertAll`.
     */
    @Transaction
    suspend fun upsertAll(players: List<PlayerEntity>) {
        val insertResults = insertIgnoringConflicts(players)
        val alreadyExisting = players.filterIndexed { index, _ -> insertResults[index] == -1L }
        if (alreadyExisting.isNotEmpty()) update(alreadyExisting)
    }

    suspend fun upsert(player: PlayerEntity) = upsertAll(listOf(player))

    @Query("SELECT * FROM players ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PlayerEntity>>

    /** Leitura única (não-Flow) de todos os jogadores salvos — usada pra buscar por nome em [com.desafiomusical.app.data.repository.PlayerRepository.findOrCreatePlayer]. */
    @Query("SELECT * FROM players")
    suspend fun getAllOnce(): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getById(id: String): PlayerEntity?
}
