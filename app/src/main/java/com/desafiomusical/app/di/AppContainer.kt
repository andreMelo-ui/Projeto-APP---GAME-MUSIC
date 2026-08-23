package com.desafiomusical.app.di

import android.content.Context
import android.net.nsd.NsdManager
import com.desafiomusical.app.data.local.AppPreferences
import com.desafiomusical.app.data.repository.GameHistoryRepository
import com.desafiomusical.app.data.repository.GameHistoryRepositoryImpl
import com.desafiomusical.app.data.repository.PlayerRepository
import com.desafiomusical.app.data.repository.PlayerRepositoryImpl
import com.desafiomusical.app.data.repository.SongRepository
import com.desafiomusical.app.data.repository.SongRepositoryImpl
import com.desafiomusical.app.data.room.CatalogAssetSource
import com.desafiomusical.app.data.room.DesafioMusicalDatabase
import com.desafiomusical.app.domain.GameEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Contêiner manual de dependências, escopado à Application. Evita puxar um
 * framework de injeção de dependências (Hilt/Koin) para um projeto deste
 * porte — as dependências são poucas e a fiação manual fica mais simples de
 * ler e testar.
 */
class AppContainer(context: Context) {

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: DesafioMusicalDatabase =
        DesafioMusicalDatabase.getInstance(context, applicationScope)

    val songRepository: SongRepository = SongRepositoryImpl(database.songDao(), CatalogAssetSource(context))
    val playerRepository: PlayerRepository = PlayerRepositoryImpl(database.playerDao())
    val gameHistoryRepository: GameHistoryRepository = GameHistoryRepositoryImpl(
        gameDao = database.gameDao(),
        roundDao = database.roundDao(),
        attemptDao = database.attemptDao(),
        playerDao = database.playerDao(),
        songDao = database.songDao()
    )

    val appPreferences: AppPreferences = AppPreferences(context)

    /** Uma única instância por processo — a partida sobrevive à navegação entre telas. */
    val gameEngine: GameEngine by lazy { GameEngine(scope = applicationScope) }

    /** Serviço do sistema usado pra anunciar/descobrir salas na rede local (Fase 3). */
    val nsdManager: NsdManager by lazy { context.getSystemService(Context.NSD_SERVICE) as NsdManager }
}
