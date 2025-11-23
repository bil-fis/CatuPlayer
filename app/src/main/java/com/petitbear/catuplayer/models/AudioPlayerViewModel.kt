package com.petitbear.catuplayer.models

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petitbear.catuplayer.media.MediaPlaybackService
import com.petitbear.catuplayer.utils.FileReadingUtils
import com.petitbear.catuplayer.utils.LrcLyric
import com.petitbear.catuplayer.utils.LrcParser
import com.petitbear.catuplayer.utils.MusicMetadataUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException

class AudioPlayerViewModel() : ViewModel() {
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

    // 歌词相关状态
    private val _currentLyrics = MutableStateFlow<List<LrcLyric>>(emptyList())
    val currentLyrics: StateFlow<List<LrcLyric>> = _currentLyrics.asStateFlow()

    private val _currentLyricIndex = MutableStateFlow(-1)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex.asStateFlow()

    init {
//        _playlist.value = createSamplePlaylist()
        initializeMediaPlayer()
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

    private val sampleLyrics = """
        [00:00.00] 作词 : 烧鸡/KBShinya/陆棠疏  
        [00:01.00] 作曲 : 崔瀚普TSAR (HOYO-MiX)  
        [00:02.00] 编曲 Arranger：崔瀚普TSAR (HOYO-MiX)  
        [00:03.00] 制作人 Producer：崔瀚普TSAR (HOYO-MiX)/宫奇Gon (HOYO-MiX)  
        
        [00:07.02]曾许下心愿 等待你 的出现  
        [00:14.04]褪色的秋千 有本书 会纪念  
        [00:21.12]我循着时间 捡起梦 的照片  
        [00:28.29]童话还没有兑现 却需要说再见  
        
        [00:37.14]如果有一天 岁月晕开书页  
        [00:43.29]请把我 模糊的字迹当作 笑脸  
        [00:50.52]很久前 想象 今天 画面  
        [00:55.83]哭着 笑着 告别  
        [00:58.44]陌生的情节 熟悉的侧脸 都重叠 oh~  
        [01:04.92]出发前 无数 星光 缱绻  
        [01:09.99]有回忆 落在 指尖  
        [01:15.84]当世界向前 我会在原地 流连  
        
        [01:51.00]当漪涟 一滴 一点 出现  
        [01:56.28]轻轻 去向 天边  
        [01:58.86]不管多遥远 都是追不上 的思念 oh~  
        [02:05.19]会不会 可能 还能 相见  
        [02:10.50]再回头只看一眼  
        
        [02:19.50]当漪涟 消散 万千 不见  
        [02:24.69]永恒和瞬间都 被爱意成全  
        [02:28.77]我的明天叫做昨天 Hoo~  
        [02:33.57]要相信 浪漫 一如 初见  
        [02:38.91]请笑着 向我 道别  
        [02:44.73]最后这一页 就让它无言  
        [02:51.84]我会在扉页 等待你续写 起点  
        
        [03:01.67] 人声 Vocal Artist：张韶涵  
        [03:02.00] 制谱 Music Copyist：吴泽熙 Jersey Wu (HOYO-MiX)  
        [03:02.33] 和声 Backing Vocal：张韶涵/奏Sou  
        [03:02.67] 配唱制作人 Vocal Producer：杨钧尧 Bryan Yang  
        [03:03.00] 音频编辑 Vocal Editing：杨钧尧 Bryan Yang  
        [03:03.33] 乐队 Orchestra：龙之艺交响乐团 Art of Loong Orchestra  
        [03:03.66] 贝斯 Bass：宣一亨Hento  
        [03:04.00] 架子鼓 Drums：眭逸凡Patrick  
        [03:04.33] 人声录音棚 Vocal Recording Studio：52Hz Studio  
        [03:04.66] 人声录音师 Vocal Recording Engineer：徐威 Aaron Xu  
        [03:05.00] 乐器录音棚 Instrumental Recording Studio：升赫录音棚Soundhub Studio/上海音像公司录音棚 YX STUDIO  
        [03:05.33] 乐器录音师 Instrumental Recording Engineer：吴身宝/李仁珏@Soundhub Studios/莫家伟  
        [03:05.66] 混音师 Mixing Engineer：宫奇Gon (HOYO-MiX)  
        [03:06.00] 母带制作 Mastering Engineer：宫奇Gon (HOYO-MiX)  
        [03:06.33] 出品 Produced by：HOYO-MiX
    """.trimIndent()

    // 加载歌词文件（在实际应用中实现）
    suspend fun loadLyricsForSong(song: Song) {
        // 这里应该从文件或网络加载歌词
        // 暂时使用示例歌词
        val lrcPath = song.uri.substringBeforeLast(".") + ".lrc"

        Log.i("catu_viewmodel","load lrc from:${lrcPath}")

        _currentLyrics.value = LrcParser.parseLrc("[00:00.00] 暂无歌词")
        _currentLyricIndex.value = -1
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
        viewModelScope.launch {
            loadLyricsForSong(song)
        }
        mediaPlaybackService?.let { service ->
            // 状态会在 collect 流中自动更新
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
        mediaPlaybackService = null
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

    private var mediaPlaybackService: MediaPlaybackService? = null

    fun setMediaPlaybackService(service: MediaPlaybackService) {
        this.mediaPlaybackService = service
    }
}