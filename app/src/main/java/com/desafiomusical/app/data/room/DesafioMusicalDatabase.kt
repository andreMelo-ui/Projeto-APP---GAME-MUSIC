package com.desafiomusical.app.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.desafiomusical.app.data.room.dao.AttemptDao
import com.desafiomusical.app.data.room.dao.GameDao
import com.desafiomusical.app.data.room.dao.PlayerDao
import com.desafiomusical.app.data.room.dao.RoundDao
import com.desafiomusical.app.data.room.dao.SongDao
import com.desafiomusical.app.data.room.entity.AttemptEntity
import com.desafiomusical.app.data.room.entity.GameEntity
import com.desafiomusical.app.data.room.entity.GamePlayerEntity
import com.desafiomusical.app.data.room.entity.PlayerEntity
import com.desafiomusical.app.data.room.entity.RoundEntity
import com.desafiomusical.app.data.room.entity.RoundScoreEntity
import com.desafiomusical.app.data.room.entity.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [
        PlayerEntity::class,
        SongEntity::class,
        GameEntity::class,
        GamePlayerEntity::class,
        RoundEntity::class,
        RoundScoreEntity::class,
        AttemptEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class DesafioMusicalDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao
    abstract fun songDao(): SongDao
    abstract fun gameDao(): GameDao
    abstract fun roundDao(): RoundDao
    abstract fun attemptDao(): AttemptDao

    companion object {
        private const val DATABASE_NAME = "desafio_musical.db"

        @Volatile
        private var instance: DesafioMusicalDatabase? = null

        fun getInstance(context: Context, applicationScope: CoroutineScope): DesafioMusicalDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context, applicationScope).also { instance = it }
            }
        }

        private fun buildDatabase(
            context: Context,
            applicationScope: CoroutineScope
        ): DesafioMusicalDatabase = Room.databaseBuilder(
            context.applicationContext,
            DesafioMusicalDatabase::class.java,
            DATABASE_NAME
        )
            .addCallback(SeedCatalogCallback(context, applicationScope))
            // Fase 1, sem release/usuários em produção ainda — histórico local
            // pode ser descartado com segurança quando o schema evolui.
            .fallbackToDestructiveMigration()
            .build()

        /**
         * Só cobre a primeira instalação (a tabela ainda nem existe antes
         * disso). Reinstalações com `catalog.json` atualizado são cobertas de
         * forma síncrona por [com.desafiomusical.app.data.repository.SongRepositoryImpl.getCatalog],
         * que sempre re-sincroniza antes de ler — um `onOpen` assíncrono aqui
         * criaria uma corrida com a primeira leitura do catálogo ao iniciar
         * uma partida (a leitura pode vencer o upsert e devolver dados
         * velhos).
         */
        private class SeedCatalogCallback(
            private val context: Context,
            private val applicationScope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                applicationScope.launch {
                    val catalog = CatalogAssetSource(context).loadCatalog()
                    getInstance(context, applicationScope).songDao().upsertAll(catalog)
                }
            }
        }
    }
}
