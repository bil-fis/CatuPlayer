package com.petitbear.catuplayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.petitbear.catuplayer.models.PlaylistEntity
import com.petitbear.catuplayer.models.SearchHistory
import com.petitbear.catuplayer.models.SearchIndex
import com.petitbear.catuplayer.models.Song
import com.petitbear.catuplayer.utils.Converters
import java.io.File

@Database(
    entities = [
        Song::class,
        PlaylistEntity::class,
        SearchIndex::class,
        SearchHistory::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun searchIndexDao(): SearchIndexDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 数据库迁移
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建 search_indices 表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS search_indices (
                        id TEXT NOT NULL,
                        songId TEXT NOT NULL,
                        keyword TEXT NOT NULL,
                        fieldType TEXT NOT NULL,
                        originalText TEXT NOT NULL,
                        weight REAL NOT NULL,
                        matchScore REAL NOT NULL,
                        lastMatched INTEGER NOT NULL,
                        matchCount INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                """)

                // 创建索引
                database.execSQL("CREATE INDEX IF NOT EXISTS index_search_indices_songId ON search_indices(songId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_search_indices_keyword ON search_indices(keyword)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_search_indices_fieldType ON search_indices(fieldType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_search_indices_songId_keyword ON search_indices(songId, keyword)")

                // 创建 search_history 表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS search_history (
                        id TEXT NOT NULL,
                        query TEXT NOT NULL,
                        resultCount INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        clickCount INTEGER NOT NULL,
                        lastClicked INTEGER NOT NULL,
                        isPinned INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                """)

                // 创建索引
                database.execSQL("CREATE INDEX IF NOT EXISTS index_search_history_query ON search_history(query)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_search_history_timestamp ON search_history(timestamp)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_search_history_isPinned ON search_history(isPinned)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // 获取外部存储目录 /Android/data
                val externalFilesDir = context.getExternalFilesDir(null)
                val dbFile = File(externalFilesDir, "databases/music_player.db")
                
                // 确保数据库目录存在
                val dbDir = dbFile.parentFile
                if (dbDir != null && !dbDir.exists()) {
                    dbDir.mkdirs()
                }
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbFile.absolutePath
                )
                    .addCallback(DatabaseCallback(context))
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}