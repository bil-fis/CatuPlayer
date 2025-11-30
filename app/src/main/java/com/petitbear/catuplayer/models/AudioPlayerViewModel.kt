package com.petitbear.catuplayer.models

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaSession
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.petitbear.catuplayer.media.PlayBackService
import com.petitbear.catuplayer.utils.FileReadingUtils
import com.petitbear.catuplayer.utils.LrcLyric
import com.petitbear.catuplayer.utils.LrcParser
import com.petitbear.catuplayer.utils.MusicMetadataUtils
import com.petitbear.catuplayer.utils.UriPermissionRestorer
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

    // 播放器相关
    private var exoPlayer: ExoPlayer?=null
    private val appContext : Context
        get() = getApplication<Application>().applicationContext
    private var job: Job? = null

    // media session
    private val _mediaController = MutableLiveData<MediaController?>()
    val mediaController: LiveData<MediaController?> = _mediaController
    private val _isMediaControllerReady = MutableStateFlow(false)
    val isMediaControllerReady: StateFlow<Boolean> = _isMediaControllerReady.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null

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

    // 封面加载状态
    private val _isCoverLoading = MutableStateFlow(false)
    val isCoverLoading: StateFlow<Boolean> = _isCoverLoading.asStateFlow()

    // 获取音频焦点
    private var audioFocusRequest: AudioFocusRequest? = null

    // 后台线程
    private val backgroundScope = CoroutineScope(Dispatchers.IO)

    // 防抖相关变量
    private var seekJob: Job? = null
    private val seekDebounceTime = 300L // 防抖延迟时间（毫秒）
    private var _isSeeking = MutableStateFlow(false)
    val isSeeking: StateFlow<Boolean> = _isSeeking.asStateFlow()

    // 添加广播接收器
    private val playbackActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getStringExtra("action")) {
                "previous" -> handleExternalPrevious()
                "next" -> handleExternalNext()
                "toggle" -> handleExternalToggle()
            }
        }
    }

    init {
        initializeMediaController()
        registerPlaybackActionReceiver()
    }

    private fun registerPlaybackActionReceiver() {
        val filter = IntentFilter("com.petitbear.catuplayer.PLAYBACK_ACTION")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ 需要指定标志
            appContext.registerReceiver(
                playbackActionReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED  // 这是应用内广播，不需要导出
            )
        } else {
            // Android 8.0 以下的旧方法
            appContext.registerReceiver(playbackActionReceiver, filter)
        }
    }
    fun initializeMediaController() {
        try {
            val sessionToken = SessionToken(appContext, ComponentName(appContext, PlayBackService::class.java))

            controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()

            controllerFuture?.addListener({
                try {
                    val controller = controllerFuture?.get()
                    _mediaController.postValue(controller)
                    _isMediaControllerReady.value = true

                    // 添加播放器状态监听器
                    controller?.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_READY -> {
                                    _isLoading.value = false
                                    _isPlaying.value = controller.isPlaying
                                    // 确保进度更新在准备好时启动
                                    if (controller.isPlaying) {
                                        startProgressUpdates()
                                    }
                                }
                                Player.STATE_BUFFERING -> {
                                    _isLoading.value = true
                                }
                                Player.STATE_ENDED -> {
                                    _isPlaying.value = false
                                    stopProgressUpdates()
                                    playNext(appContext)
                                }
                                Player.STATE_IDLE -> {
                                    _isPlaying.value = false
                                    stopProgressUpdates()
                                }
                                else -> {
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
                            _errorMessage.value = "播放错误: ${error.message}"
                            _isLoading.value = false
                            stopProgressUpdates()
                        }

                        // 添加位置变化监听，确保进度同步
                        override fun onPositionDiscontinuity(
                            oldPosition: Player.PositionInfo,
                            newPosition: Player.PositionInfo,
                            reason: Int
                        ) {
                            updateProgress()
                        }
                    })

                    Log.d("PlayerViewModel", "MediaController初始化成功")
                } catch (e: Exception) {
                    Log.e("PlayerViewModel", "MediaController创建失败", e)
                    _mediaController.postValue(null)
                    _isMediaControllerReady.value = false
                }
            }, ContextCompat.getMainExecutor(appContext))
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "SessionToken创建失败", e)
            _isMediaControllerReady.value = false
        }
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
                        _mediaController.value?.pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        // 短暂失去焦点，暂停播放
                        _mediaController.value?.pause()
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        // 重新获得焦点，恢复播放
                        _mediaController.value?.play()
                    }
                }
            }
            .build()

        return audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun updateProgress() {
        if (_isSeeking.value) return // 如果正在跳转，跳过自动更新

        _mediaController.value?.let { player ->
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
                var lyrics = LrcParser.parseLrc("[00:00.00] 暂无歌词")



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

    // 异步加载专辑封面
    suspend fun loadAlbumCover(song: Song) {
        _isCoverLoading.value = true

        backgroundScope.launch {
            try {
                // 这里可以添加网络加载封面的逻辑
                // 目前我们只使用内嵌封面或本地封面

                // 模拟网络加载延迟（用于测试）
                delay(500)

                // 在实际应用中，这里可以添加网络请求来获取专辑封面
                // 例如：val networkCoverUri = fetchCoverFromNetwork(song)

                withContext(Dispatchers.Main) {
                    _isCoverLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isCoverLoading.value = false
                }
            }
        }
    }

    private fun startProgressUpdates() {
        job?.cancel()
        job = viewModelScope.launch {
            while (isActive) {
                updateProgress()
                delay(1000) // 每秒更新一次
            }
        }
    }

    private fun stopProgressUpdates() {
        job?.cancel()
        job = null
    }

    fun playSong(song: Song, fromExternal: Boolean = false) {
        if (!_isMediaControllerReady.value) {
            _errorMessage.value = "播放器未就绪，请稍后重试"
            return
        }

        if (song.uri.isEmpty()) {
            _errorMessage.value = "无效的音乐文件路径"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        _mediaController.value?.let { player ->
            try {
                // 先停止当前的进度更新
                stopProgressUpdates()

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

                // 设置媒体项并准备播放
                val mediaItem = MediaItem.fromUri(song.uri)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()

                // 确保播放状态正确更新
                _isPlaying.value = true

                // 启动进度更新
                startProgressUpdates()

                // 在后台加载歌词和封面
                if (!fromExternal) {
                    viewModelScope.launch {
                        loadLyricsForSong(song)
                        loadAlbumCover(song)
                    }
                }

            } catch (e: Exception) {
                _errorMessage.value = "无法播放音乐文件: ${e.message}"
                _isLoading.value = false
                e.printStackTrace()
            }
        } ?: run {
            _errorMessage.value = "播放器未初始化"
            _isLoading.value = false
        }
    }

    fun pauseOrResume() {
        _mediaController.value?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopProgressUpdates()
            } else {
                player.play()
                _isPlaying.value = true
                startProgressUpdates()
            }
        }
    }

    fun seekTo(progress: Float) {
        // 设置正在跳转状态
        _isSeeking.value = true

        _mediaController.value?.let { player ->
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
        _mediaController.value?.let { player ->
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
                    hasMetadata = musicInfo.hasMetadata,
                    coverUri = musicInfo.coverUri, // 添加封面URI
                    hasEmbeddedCover = musicInfo.hasEmbeddedCover // 添加内嵌封面信息
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

        try {
            appContext.unregisterReceiver(playbackActionReceiver)
        } catch (e: Exception) {
            // 忽略取消注册异常
        }


        // 释放 媒体控制器
        _mediaController.value?.run {
            release()
        }
        controllerFuture?.cancel(true)
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

    // 处理来自外部的控制请求（避免递归）
    private fun handleExternalPrevious() {
        viewModelScope.launch {
            delay(50)
            val current = _currentSong.value
            val list = _playlist.value
            if (current != null && list.isNotEmpty()) {
                val currentIndex = list.indexOfFirst { it.id == current.id }
                val prevIndex = if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
                val previousSong = list[prevIndex]
                _currentSong.value = previousSong

                _mediaController.value?.let { player ->
                    // 先停止当前的进度更新
                    stopProgressUpdates()

                    val mediaItem = MediaItem.fromUri(previousSong.uri)
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()

                    // 确保播放状态正确更新
                    _isPlaying.value = true

                    // 立即启动进度更新
                    startProgressUpdates()

                    // 加载歌词和封面
                    viewModelScope.launch {
                        loadLyricsForSong(previousSong)
                        loadAlbumCover(previousSong)
                    }
                }
            }
        }
    }

    private fun handleExternalNext() {
        viewModelScope.launch {
            delay(50)
            val current = _currentSong.value
            val list = _playlist.value
            if (current != null && list.isNotEmpty()) {
                val currentIndex = list.indexOfFirst { it.id == current.id }
                val nextIndex = (currentIndex + 1) % list.size
                val nextSong = list[nextIndex]
                _currentSong.value = nextSong

                _mediaController.value?.let { player ->
                    // 先停止当前的进度更新
                    stopProgressUpdates()

                    val mediaItem = MediaItem.fromUri(nextSong.uri)
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()

                    // 确保播放状态正确更新
                    _isPlaying.value = true

                    // 立即启动进度更新
                    startProgressUpdates()

                    viewModelScope.launch {
                        loadLyricsForSong(nextSong)
                        loadAlbumCover(nextSong)
                    }
                }
            }
        }
    }

    private fun handleExternalToggle() {
        _mediaController.value?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopProgressUpdates()
            } else {
                player.play()
                _isPlaying.value = true
                startProgressUpdates()
            }
        }
    }

    fun forceUpdateProgress() {
        updateProgress()
    }

}