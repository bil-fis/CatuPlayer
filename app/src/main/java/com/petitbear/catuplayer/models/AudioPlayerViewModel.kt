package com.petitbear.catuplayer.models

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.petitbear.catuplayer.utils.LrcLyric
import com.petitbear.catuplayer.utils.LrcParser
import com.petitbear.catuplayer.utils.MusicMetadataUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class AudioPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private var exoPlayer: ExoPlayer?=null
    private val appContext : Context
        get() = getApplication<Application>().applicationContext
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

    // 歌词相关状态
    private val _currentLyrics = MutableStateFlow<List<LrcLyric>>(emptyList())
    val currentLyrics: StateFlow<List<LrcLyric>> = _currentLyrics.asStateFlow()

    private val _currentLyricIndex = MutableStateFlow(-1)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex.asStateFlow()

    // 获取音频焦点
    private var audioFocusRequest: AudioFocusRequest? = null

    // 后台线程
    private val backgroundScope = CoroutineScope(Dispatchers.IO)

    // 防抖相关变量
    private var seekJob: Job? = null
    private val seekDebounceTime = 300L // 防抖延迟时间（毫秒）
    private var _isSeeking = MutableStateFlow(false)
    val isSeeking: StateFlow<Boolean> = _isSeeking.asStateFlow()

    init {
        initializeExoPlayer()
    }

    private fun requestAudioFocus(): Boolean {
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        // 长时间失去焦点，停止播放
                        exoPlayer?.pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        // 短暂失去焦点，暂停播放
                        exoPlayer?.pause()
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        // 重新获得焦点，恢复播放
                        exoPlayer?.play()
                    }
                }
            }
            .build()

        return audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun initializeExoPlayer(){
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // 优化ExoPlayer构建器配置，减少AudioTrack重建
        exoPlayer = ExoPlayer.Builder(appContext)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(5000) // 5秒后退
            .setSeekForwardIncrementMs(15000) // 15秒前进
            .build().apply{
                addListener(object : Player.Listener{
                    override fun onPlaybackStateChanged(playbackState: @Player.State Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                _isLoading.value = false
                                updateProgress()
                            }
                            Player.STATE_BUFFERING -> {
                                _isLoading.value = true
                            }
                            Player.STATE_ENDED -> {
                                _isPlaying.value = false
                                _progress.value = 1f
                                // 延迟播放下一首，避免立即重建AudioTrack
                                viewModelScope.launch {
                                    delay(100) // 给系统一些时间清理资源
                                    playNext(appContext)
                                }
                            }
                            Player.STATE_IDLE -> {
                                _isPlaying.value = false
                                _isLoading.value = false
                            }
                        }
                    }
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (isPlaying) {
                            startProgressUpdates()
                        } else {
                            stopProgressUpdates()
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _errorMessage.value = "播放错误：${error.message}"
                        _isPlaying.value = false
                        _isLoading.value = false
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        updateProgress()
                    }
                })
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    private fun updateProgress() {
        if (_isSeeking.value) return // 如果正在跳转，跳过自动更新

        exoPlayer?.let { player ->
            val currentPos = player.currentPosition
            val duration = player.duration

            _currentPosition.value = currentPos

            if (duration > 0 && duration != C.TIME_UNSET) {
                _progress.value = currentPos.toFloat() / duration
            }
        }
    }

    // 更新当前歌词索引
    fun updateCurrentLyric(currentPosition: Long) {
        val lyrics = _currentLyrics.value
        if (lyrics.isEmpty()) return

        var newIndex = -1
        for (i in lyrics.indices) {
            if (currentPosition >= lyrics[i].time) {
                newIndex = i
            } else {
                break
            }
        }

        if (newIndex != _currentLyricIndex.value) {
            _currentLyricIndex.value = newIndex
        }
    }

    // 加载歌词文件
    suspend fun loadLyricsForSong(song: Song) {
        backgroundScope.launch {
            try {
                val lrcPath = song.uri.substringBeforeLast(".") + ".lrc"
                Log.i("catu_viewmodel", "load lrc from:${lrcPath}")

                // 这里可以添加实际的歌词文件读取逻辑
                val lyrics = LrcParser.parseLrc("[00:00.00] 暂无歌词")

                // 在主线程中更新歌词状态
                withContext(Dispatchers.Main) {
                    _currentLyrics.value = lyrics
                    _currentLyricIndex.value = -1
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 加载失败时设置默认歌词
                withContext(Dispatchers.Main) {
                    _currentLyrics.value = LrcParser.parseLrc("[00:00.00] 暂无歌词")
                    _currentLyricIndex.value = -1
                }
            }
        }
    }

    private fun startProgressUpdates() {
        job?.cancel()
        job = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && _isPlaying.value) {
                updateProgress()
                delay(1000) // 降低更新频率，从500ms改为1000ms
            }
        }
    }

    private fun stopProgressUpdates() {
        job?.cancel()
        job = null
    }

    fun playSong(song: Song) {
        if (song.uri.isEmpty()) {
            _errorMessage.value = "无效的音乐文件路径"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        exoPlayer?.let { player ->
            try {
                // 检查是否正在播放同一首歌曲
                if (_currentSong.value?.id == song.id) {
                    if (!player.isPlaying) {
                        player.play()
                        _isPlaying.value = true
                        startProgressUpdates()
                    }
                    _isLoading.value = false
                    return
                }

                // 设置新歌曲信息
                _currentSong.value = song

                // 重置进度
                _progress.value = 0f
                _currentPosition.value = 0L

                // 使用更高效的媒体项设置方式
                val mediaItem = MediaItem.Builder()
                    .setUri(song.uri)
                    .setMediaId(song.id)
                    .build()

                // 停止当前播放但不释放资源
                player.stop()
                player.setMediaItem(mediaItem)
                player.prepare()

                // 启动播放
                player.play()
                _isPlaying.value = true

                // 启动进度更新
                startProgressUpdates()

                // 在后台加载歌词
                backgroundScope.launch {
                    loadLyricsForSong(song)
                }

            } catch (e: Exception) {
                _errorMessage.value = "无法播放音乐文件: ${e.message}"
                _isLoading.value = false
                e.printStackTrace()
            }
        }
    }

    fun pauseOrResume() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopProgressUpdates()
            } else {
                player.play() // 如果已经准备好，这会恢复播放
                _isPlaying.value = true
                startProgressUpdates()
            }
        }
    }

    fun seekTo(progress: Float) {
        // 设置正在跳转状态
        _isSeeking.value = true

        exoPlayer?.let { player ->
            val duration = player.duration
            if (duration > 0 && duration != C.TIME_UNSET) {
                val newPosition = (duration * progress).toLong()
                player.seekTo(newPosition)
                // 立即更新进度显示，让进度条平滑跟随
                _currentPosition.value = newPosition
                _progress.value = progress

                // 延迟一小段时间后重置跳转状态
                viewModelScope.launch {
                    delay(100) // 给播放器一点时间处理跳转
                    _isSeeking.value = false
                }
            }
        }
    }

    // 实际执行跳转的方法
    private fun performSeek(progress: Float) {
        exoPlayer?.let { player ->
            val duration = player.duration
            if (duration > 0 && duration != C.TIME_UNSET) {
                val newPosition = (duration * progress).toLong()
                player.seekTo(newPosition)
                // 跳转后立即更新进度
                updateProgress()
            }
        }
    }

    fun playNext() {
        val current = _currentSong.value
        val list = _playlist.value
        if (current != null && list.isNotEmpty()) {
            val currentIndex = list.indexOfFirst { it.id == current.id }
            val nextIndex = (currentIndex + 1) % list.size
            // 添加延迟以避免频繁切换
            viewModelScope.launch {
                delay(50) // 给系统一些时间清理
                playSong(list[nextIndex])
            }
        } else if (list.isNotEmpty()) {
            viewModelScope.launch {
                delay(50)
                playSong(list[0])
            }
        }
    }

    fun playPrevious() {
        val current = _currentSong.value
        val list = _playlist.value
        if (current != null && list.isNotEmpty()) {
            val currentIndex = list.indexOfFirst { it.id == current.id }
            val prevIndex = if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
            viewModelScope.launch {
                delay(50)
                playSong(list[prevIndex])
            }
        } else if (list.isNotEmpty()) {
            viewModelScope.launch {
                delay(50)
                playSong(list[list.size - 1])
            }
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

    fun setPlayList(songs:List<Song>){
        _playlist.value = songs
    }

    suspend fun addSongsToPlaylist(context: Context, files: List<Pair<Uri, String>>) {
        _isLoading.value = true

        try {
            // 在 IO 线程中处理文件解析
            val newSongs = withContext(Dispatchers.IO) {
                files.mapNotNull { (uri, fileName) ->
                    createSongFromUri(appContext,uri, fileName)
                }
            }

            // 在主线程中更新状态
            withContext(Dispatchers.Main) {
                val currentList = _playlist.value.toMutableList()
                val uniqueNewSongs = newSongs.filter { newSong ->
                    currentList.none { existingSong -> existingSong.id == newSong.id }
                }

                if (uniqueNewSongs.isNotEmpty()) {
                    currentList.addAll(uniqueNewSongs)
                    _playlist.value = currentList
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // 在主线程中更新错误状态
            withContext(Dispatchers.Main) {
                _errorMessage.value = "添加歌曲失败: ${e.message}"
            }
        } finally {
            // 在主线程中更新加载状态
            withContext(Dispatchers.Main) {
                _isLoading.value = false
            }
        }
    }

    suspend fun createSongFromUri(context: Context, uri: Uri, fileName: String): Song? {
        return withContext(Dispatchers.IO) {
            try {
                val musicInfo = MusicMetadataUtils.parseMusicInfo(appContext, uri, fileName)

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
            }
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
        backgroundScope.cancel()
        seekJob?.cancel() // 取消防抖任务

        // 释放 ExoPlayer
        exoPlayer?.release()
        exoPlayer = null
    }

    fun playNext(context: Context) {
        val current = _currentSong.value
        val list = _playlist.value
        if (current != null && list.isNotEmpty()) {
            val currentIndex = list.indexOfFirst { it.id == current.id }
            val nextIndex = (currentIndex + 1) % list.size
            playSong(list[nextIndex])
        } else if (list.isNotEmpty()) {
            playSong(list[0])
        }
    }

    fun playPrevious(context: Context) {
        val current = _currentSong.value
        val list = _playlist.value
        if (current != null && list.isNotEmpty()) {
            val currentIndex = list.indexOfFirst { it.id == current.id }
            val prevIndex = if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
            playSong(list[prevIndex])
        } else if (list.isNotEmpty()) {
            playSong(list[list.size - 1])
        }
    }
}