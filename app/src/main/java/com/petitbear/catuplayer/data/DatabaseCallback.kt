package com.petitbear.catuplayer.data

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.petitbear.catuplayer.models.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.serialization.json.Json

class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        // 数据库创建时，可以初始化一些数据
        CoroutineScope(Dispatchers.IO).launch {
            // 检查是否有旧的JSON播放列表，如果有则迁移
            migrateFromJsonIfExists(db)
        }
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        // 可以在这里设置一些PRAGMA
        // 使用 try-catch 避免错误
        try {
            // 使用 query 而不是 execSQL 来执行 PRAGMA
            db.query("PRAGMA journal_mode = WAL")?.close()
            db.query("PRAGMA foreign_keys = ON")?.close()
        } catch (e: Exception) {
            // 忽略异常，继续执行
        }
    }

    private suspend fun migrateFromJsonIfExists(db: SupportSQLiteDatabase) {
        val jsonFile = File(context.getExternalFilesDir(null), "catu_playlist_data.json")
        if (jsonFile.exists()) {
            try {
                val jsonString = jsonFile.bufferedReader().use { it.readText() }
                if (jsonString.isNotBlank()) {
                    val songs = json.decodeFromString<List<Song>>(jsonString)

                    // 直接使用 db 执行插入操作
                    if (songs.isNotEmpty()) {
                        // 直接执行 SQL 插入
                        songs.forEach { song ->
                            // 构建插入 SQL
                            val sql = """
                                INSERT OR REPLACE INTO songs (
                                    id, title, artist, duration, uri, album, 
                                    hasMetadata, coverUri, hasEmbeddedCover, lrcCachePath, 
                                    hasEmbeddedLyric, lyricDownloadUrl, lyricSource, filePath, 
                                    fileSize, createdTime, lastPlayedTime, playCount, 
                                    bitrate, sampleRate, channels, fileFormat, genre, 
                                    year, trackNumber, discNumber, isFavorite, 
                                    tags, audioFeatures
                                ) VALUES (
                                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 
                                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 
                                    ?, ?, ?, ?, ?, ?, ?, ?, ?
                                )
                            """.trimIndent()

                            try {
                                db.execSQL(sql, arrayOf(
                                    song.id,
                                    song.title,
                                    song.artist,
                                    song.duration,
                                    song.uri,
                                    song.album,
                                    if (song.hasMetadata) 1 else 0,
                                    song.coverUri,
                                    if (song.hasEmbeddedCover) 1 else 0,
                                    song.lrcCachePath,
                                    if (song.hasEmbeddedLyric) 1 else 0,
                                    song.lyricDownloadUrl,
                                    song.lyricSource,
                                    song.filePath,
                                    song.fileSize,
                                    song.createdTime,
                                    song.lastPlayedTime,
                                    song.playCount,
                                    song.bitrate,
                                    song.sampleRate,
                                    song.channels,
                                    song.fileFormat,
                                    song.genre,
                                    song.year,
                                    song.trackNumber,
                                    song.discNumber,
                                    if (song.isFavorite) 1 else 0,
                                    song.tags,
                                    song.audioFeatures
                                ))
                            } catch (e: Exception) {
                                // 忽略重复插入的错误
                            }
                        }

                        // 迁移成功后，可以重命名旧文件作为备份
                        val backupFile = File(jsonFile.parent, "catu_playlist_data.json.backup")
                        jsonFile.renameTo(backupFile)

                        // 记录迁移日志
                        android.util.Log.i("DatabaseMigration", "成功迁移 ${songs.size} 首歌曲到数据库")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DatabaseMigration", "迁移失败: ${e.message}", e)
                // 迁移失败，保持JSON文件不变
            }
        }
    }
}