package com.petitbear.catuplayer.utils

import android.content.Context
import android.util.Log
import com.petitbear.catuplayer.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 播放列表存储管理类
 */
class PlaylistFileManager(private val context: Context) {

    private val playlistFile = File(context.getExternalFilesDir(null), "catu_playlist_data.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    // 内部状态
    private val _playlist = MutableStateFlow(emptyList<Song>())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    /**
     * 保存播放列表到文件
     * @param songs 要保存的歌曲列表
     * @return 保存结果
     */
    suspend fun savePlaylist(songs: List<Song>): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // 更新状态
            _playlist.value = songs

            // 保存到文件
            val jsonString = json.encodeToString(songs)
            playlistFile.bufferedWriter().use { writer ->
                writer.write(jsonString)
            }

            Log.d("PlaylistFileManager", "播放列表已保存: ${songs.size} 首歌曲")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PlaylistFileManager", "保存播放列表失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 从文件加载播放列表
     * @return 加载的歌曲列表
     */
    suspend fun loadPlaylist(): Result<List<Song>> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!playlistFile.exists()) {
                Log.d("PlaylistFileManager", "播放列表文件不存在，返回空列表")
                return@withContext Result.success(emptyList())
            }

            val jsonString = playlistFile.bufferedReader().use { it.readText() }

            if (jsonString.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            val songs = json.decodeFromString<List<Song>>(jsonString)

            // 更新状态
            _playlist.value = songs

            Log.d("PlaylistFileManager", "播放列表已加载: ${songs.size} 首歌曲")
            Result.success(songs)
        } catch (e: Exception) {
            Log.e("PlaylistFileManager", "加载播放列表失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 添加歌曲到播放列表并保存
     */
    suspend fun addSong(song: Song): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentList = _playlist.value.toMutableList()

            // 检查是否已存在（基于ID或URI）
            if (currentList.any { it.id == song.id || it.uri == song.uri }) {
                return@withContext Result.failure(IllegalStateException("歌曲已存在于播放列表中"))
            }

            currentList.add(song)
            savePlaylist(currentList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 批量添加歌曲到播放列表并保存
     */
    suspend fun addSongs(songs: List<Song>): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentList = _playlist.value.toMutableList()
            val newSongs = songs.filter { newSong ->
                !currentList.any { it.id == newSong.id || it.uri == newSong.uri }
            }

            currentList.addAll(newSongs)
            savePlaylist(currentList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从播放列表移除歌曲并保存
     */
    suspend fun removeSong(songId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentList = _playlist.value.toMutableList()
            currentList.removeAll { it.id == songId }
            savePlaylist(currentList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 清空播放列表并保存
     */
    suspend fun clearPlaylist(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext savePlaylist(emptyList())
    }

    /**
     * 更新歌曲信息并保存
     */
    suspend fun updateSong(updatedSong: Song): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentList = _playlist.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == updatedSong.id }

            if (index != -1) {
                currentList[index] = updatedSong
                savePlaylist(currentList)
            } else {
                Result.failure(IllegalStateException("未找到要更新的歌曲"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 重新排序播放列表并保存
     */
    suspend fun reorderPlaylist(fromIndex: Int, toIndex: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val currentList = _playlist.value.toMutableList()

                if (fromIndex < 0 || fromIndex >= currentList.size ||
                    toIndex < 0 || toIndex >= currentList.size
                ) {
                    return@withContext Result.failure(IndexOutOfBoundsException("索引超出范围"))
                }

                val song = currentList.removeAt(fromIndex)
                currentList.add(toIndex, song)
                savePlaylist(currentList)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 获取播放列表大小
     */
    fun getPlaylistSize(): Int {
        return _playlist.value.size
    }

    /**
     * 检查播放列表是否为空
     */
    fun isPlaylistEmpty(): Boolean {
        return _playlist.value.isEmpty()
    }

    /**
     * 根据ID查找歌曲
     */
    fun findSongById(songId: String): Song? {
        return _playlist.value.find { it.id == songId }
    }

    /**
     * 根据URI查找歌曲
     */
    fun findSongByUri(uri: String): Song? {
        return _playlist.value.find { it.uri == uri }
    }

    /**
     * 获取播放列表文件信息
     */
    fun getFileInfo(): FileInfo {
        return FileInfo(
            exists = playlistFile.exists(),
            fileSize = if (playlistFile.exists()) playlistFile.length() else 0,
            lastModified = if (playlistFile.exists()) playlistFile.lastModified() else 0,
            filePath = playlistFile.absolutePath
        )
    }

    /**
     * 备份播放列表到指定文件
     */
    suspend fun backupPlaylist(backupFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!playlistFile.exists()) {
                return@withContext Result.failure(IllegalStateException("没有播放列表数据可备份"))
            }

            playlistFile.copyTo(backupFile, overwrite = true)
            Log.d("PlaylistFileManager", "播放列表已备份到: ${backupFile.absolutePath}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PlaylistFileManager", "备份播放列表失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 从备份文件恢复播放列表
     */
    suspend fun restoreFromBackup(backupFile: File): Result<List<Song>> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                if (!backupFile.exists()) {
                    return@withContext Result.failure(IllegalStateException("备份文件不存在"))
                }

                val jsonString = backupFile.bufferedReader().use { it.readText() }
                val songs = json.decodeFromString<List<Song>>(jsonString)

                // 保存到主文件并更新状态
                savePlaylist(songs)

                Log.d("PlaylistFileManager", "播放列表已从备份恢复: ${songs.size} 首歌曲")
                Result.success(songs)
            } catch (e: Exception) {
                Log.e("PlaylistFileManager", "从备份恢复播放列表失败: ${e.message}")
                Result.failure(e)
            }
        }

    /**
     * 文件信息数据类
     */
    data class FileInfo(
        val exists: Boolean,
        val fileSize: Long,
        val lastModified: Long,
        val filePath: String
    )
}