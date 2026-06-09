package com.example.wikipedia_app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Bookmark::class, History::class, ArticleCache::class], version = 4)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun articleCacheDao(): ArticleCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS history (
                        title TEXT NOT NULL PRIMARY KEY,
                        url TEXT NOT NULL,
                        timestamp INTEGER
                    )
                """)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS article_cache (
                        title TEXT NOT NULL PRIMARY KEY,
                        content TEXT NOT NULL,
                        links TEXT NOT NULL,
                        timestamp INTEGER NOT NULL DEFAULT 0,
                        isGameArticle INTEGER NOT NULL DEFAULT 0,
                        isHyperlink INTEGER NOT NULL DEFAULT 0,
                        parentArticle TEXT
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wikipedia_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 