package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CachedAnimeEntity::class,
        SearchHistoryEntity::class,
        QuickListItemEntity::class,
        PlaybackProgressEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NimeCatalogDatabase : RoomDatabase() {
    abstract fun catalogDao(): AnimeCatalogDao

    companion object {
        @Volatile
        private var INSTANCE: NimeCatalogDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `playback_progress` (
                        `animeId` TEXT NOT NULL,
                        `animeTitle` TEXT NOT NULL,
                        `posterUrl` TEXT,
                        `seasonId` TEXT NOT NULL,
                        `episodeNumber` INTEGER NOT NULL,
                        `episodeTitle` TEXT NOT NULL,
                        `positionMs` INTEGER NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`animeId`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): NimeCatalogDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NimeCatalogDatabase::class.java,
                    "nime_watcher_catalog.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
