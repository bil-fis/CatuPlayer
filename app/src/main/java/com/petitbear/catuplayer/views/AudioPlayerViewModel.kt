package com.petitbear.catuplayer.views

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petitbear.catuplayer.models.Song
import com.petitbear.catuplayer.utils.FilePickerUtils
import com.petitbear.catuplayer.utils.MusicMetadataUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import java.io.IOException

class AudioPlayerViewModel : ViewModel() {
    private var mediaPlayer: MediaPlayer? = null
    private var job: Job? = null

    // 播放状态
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // 当前播放歌曲
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    // 播放进度 (0.0 - 1.0)
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    // 当前播放位置 (毫秒)
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    // 播放列表
    private val _playlist = MutableStateFlow(emptyList<Song>())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误状态
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
//        _playlist.value = createSamplePlaylist()
        initializeMediaPlayer()
    }

    private fun initializeMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setOnPreparedListener {
                _isLoading.value = false
                _isPlaying.value = true
                startProgressUpdates()
            }
            setOnCompletionListener {
                _isPlaying.value = false
                _progress.value = 0f
                _currentPosition.value = 0
                stopProgressUpdates()
                // 播放完成后自动播放下一首
                playNext()
            }
            setOnErrorListener { _, what, extra ->
                _isLoading.value = false
                _isPlaying.value = false
                _errorMessage.value = "播放错误: what=$what, extra=$extra"
                false
            }
        }
    }

    private fun startProgressUpdates() {
        job?.cancel()
        job = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val currentPos = player.currentPosition.toLong()
                        val duration = player.duration.toLong()

                        _currentPosition.value = currentPos

                        if (duration > 0) {
                            _progress.value = currentPos.toFloat() / duration
                        }
                    }
                }
                delay(1000) // 每秒更新一次
            }
        }
    }

    private fun stopProgressUpdates() {
        job?.cancel()
        job = null
    }

    fun playSong(song: Song, context: Context? = null) {
        if (song.uri.isEmpty()) {
            _errorMessage.value = "无效的音乐文件路径"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        // 停止当前播放
        mediaPlayer?.reset()

        try {
            val uri = Uri.parse(song.uri)

            if (uri.scheme == "content" || uri.scheme == "file") {
                // 对于 content:// 或 file:// URI
                context?.let { ctx ->
                    mediaPlayer?.setDataSource(ctx, uri)
                } ?: run {
                    _errorMessage.value = "需要上下文来播放音乐文件"
                    _isLoading.value = false
                    return
                }
            } else {
                // 对于其他 URI 类型（如网络流）
                mediaPlayer?.setDataSource(song.uri)
            }

            mediaPlayer?.prepareAsync()
            _currentSong.value = song
        } catch (e: IOException) {
            _errorMessage.value = "无法播放音乐文件: ${e.message}"
            _isLoading.value = false
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            _errorMessage.value = "无效的音乐文件: ${e.message}"
            _isLoading.value = false
            e.printStackTrace()
        } catch (e: SecurityException) {
            _errorMessage.value = "没有权限访问音乐文件"
            _isLoading.value = false
            e.printStackTrace()
        }
    }

    fun pauseOrResume() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopProgressUpdates()
            } else {
                if (_currentSong.value != null) {
                    player.start()
                    _isPlaying.value = true
                    startProgressUpdates()
                }
            }
        }
    }

    fun seekTo(progress: Float) {
        mediaPlayer?.let { player ->
            val duration = player.duration
            if (duration > 0) {
                val newPosition = (duration * progress).toInt()
                player.seekTo(newPosition)
                _currentPosition.value = newPosition.toLong()
                _progress.value = progress
            }
        }
    }

    fun playNext() {
        val current = _currentSong.value
        val list = _playlist.value
        if (current != null && list.isNotEmpty()) {
            val currentIndex = list.indexOfFirst { it.id == current.id }
            val nextIndex = (currentIndex + 1) % list.size
            _currentSong.value?.let { song ->
                // 需要上下文来播放下一首
                // 在实际使用中，需要在调用时传入context
                // playSong(list[nextIndex], context)
            }
        } else if (list.isNotEmpty()) {
            // 需要上下文来播放第一首
            // playSong(list[0], context)
        }
    }

    fun playPrevious() {
        val current = _currentSong.value
        val list = _playlist.value
        if (current != null && list.isNotEmpty()) {
            val currentIndex = list.indexOfFirst { it.id == current.id }
            val prevIndex = if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
            _currentSong.value?.let { song ->
                // 需要上下文来播放上一首
                // playSong(list[prevIndex], context)
            }
        } else if (list.isNotEmpty()) {
            // 需要上下文来播放最后一首
            // playSong(list[list.size - 1], context)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // 添加歌曲相关方法保持不变
    fun addSongToPlaylist(song: Song) {
        val currentList = _playlist.value.toMutableList()
        if (currentList.none { it.id == song.id }) {
            currentList.add(song)
            _playlist.value = currentList
        }
    }

    suspend fun addSongsToPlaylist(context: Context, files: List<Pair<Uri, String>>) {
        _isLoading.value = true

        try {
            val newSongs = mutableListOf<Song>()

            files.forEach { (uri, fileName) ->
                val song = createSongFromUri(context, uri, fileName)
                song?.let { newSongs.add(it) }
            }

            val currentList = _playlist.value.toMutableList()
            val uniqueNewSongs = newSongs.filter { newSong ->
                currentList.none { existingSong -> existingSong.id == newSong.id }
            }

            if (uniqueNewSongs.isNotEmpty()) {
                currentList.addAll(uniqueNewSongs)
                _playlist.value = currentList
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun createSongFromUri(context: Context, uri: Uri, fileName: String): Song? {
        return try {
            _isLoading.value = true

            val musicInfo = MusicMetadataUtils.parseMusicInfo(context, uri, fileName)

            val id = "file_${uri.toString().hashCode()}"

            Song(
                id = id,
                title = musicInfo.title,
                artist = musicInfo.artist,
                duration = musicInfo.duration,
                uri = uri.toString(),
                hasMetadata = musicInfo.hasMetadata
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            _isLoading.value = false
        }
    }

    private fun createSamplePlaylist(): List<Song> {
        return listOf(
            Song(
                id = "1",
                title = "示例歌曲1",
                artist = "示例艺术家",
                duration = 180000,
                uri = "" // 空URI，不会实际播放
            ),
            Song(
                id = "2",
                title = "示例歌曲2",
                artist = "示例艺术家",
                duration = 200000,
                uri = ""
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressUpdates()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun playNext(context: Context) {
        val current = _currentSong.value
        val list = _playlist.value
        if (current != null && list.isNotEmpty()) {
            val currentIndex = list.indexOfFirst { it.id == current.id }
            val nextIndex = (currentIndex + 1) % list.size
            playSong(list[nextIndex], context)
        } else if (list.isNotEmpty()) {
            playSong(list[0], context)
        }
    }

    fun playPrevious(context: Context) {
        val current = _currentSong.value
        val list = _playlist.value
        if (current != null && list.isNotEmpty()) {
            val currentIndex = list.indexOfFirst { it.id == current.id }
            val prevIndex = if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
            playSong(list[prevIndex], context)
        } else if (list.isNotEmpty()) {
            playSong(list[list.size - 1], context)
        }
    }
}