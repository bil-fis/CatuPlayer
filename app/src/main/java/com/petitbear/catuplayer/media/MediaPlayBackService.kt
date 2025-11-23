package com.petitbear.catuplayer.media

import android.app.*
import android.content.*
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.petitbear.catuplayer.R
import com.petitbear.catuplayer.models.Song
import com.petitbear.catuplayer.models.AudioPlayerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MediaPlaybackService : LifecycleService() {

    private val binder = MediaPlaybackBinder()
    private lateinit var notificationManager: NotificationManager
    private var viewModel: AudioPlayerViewModel? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> get() = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> get() = _isPlaying

    inner class MediaPlaybackBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        // 立即创建并显示通知，避免 ANR
        createDefaultNotification()
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "音乐播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "音乐播放通知"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createDefaultNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("音乐播放器")
            .setContentText("准备播放音乐")
            .setSmallIcon(R.drawable.ic_music_note)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(false)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    fun setViewModel(viewModel: AudioPlayerViewModel) {
        this.viewModel = viewModel

        lifecycleScope.launch {
            viewModel.currentSong.collect { song ->
                _currentSong.value = song
                updateNotification()
            }
        }

        lifecycleScope.launch {
            viewModel.isPlaying.collect { playing ->
                _isPlaying.value = playing
                updateNotification()
            }
        }
    }

    private fun updateNotification() {
        val song = _currentSong.value ?: return

        // 创建播放/暂停的PendingIntent
        val playPauseIntent = Intent(this, MediaPlaybackService::class.java).apply {
            action = if (_isPlaying.value) ACTION_PAUSE else ACTION_PLAY
        }
        val playPausePendingIntent = PendingIntent.getService(
            this,
            0,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 上一首
        val previousIntent = Intent(this, MediaPlaybackService::class.java).apply {
            action = ACTION_PREVIOUS
        }
        val previousPendingIntent = PendingIntent.getService(
            this,
            1,
            previousIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 下一首
        val nextIntent = Intent(this, MediaPlaybackService::class.java).apply {
            action = ACTION_NEXT
        }
        val nextPendingIntent = PendingIntent.getService(
            this,
            2,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 使用Material Design图标
        val playPauseIcon = if (_isPlaying.value) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play_arrow
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setSmallIcon(R.drawable.ic_music_note)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_skip_previous,
                    "上一首",
                    previousPendingIntent
                )
            )
            .addAction(
                NotificationCompat.Action(
                    playPauseIcon,
                    if (_isPlaying.value) "暂停" else "播放",
                    playPausePendingIntent
                )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_skip_next,
                    "下一首",
                    nextPendingIntent
                )
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(_isPlaying.value)
            .setShowWhen(false)
            .build()

        // 更新通知
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent?.action?.let { action ->
            when (action) {
                ACTION_PLAY -> viewModel?.pauseOrResume()
                ACTION_PAUSE -> viewModel?.pauseOrResume()
                ACTION_NEXT -> viewModel?.playNext(applicationContext)
                ACTION_PREVIOUS -> viewModel?.playPrevious(applicationContext)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }

    companion object {
        const val CHANNEL_ID = "catuplayer_music_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_PLAY = "com.petitbear.catuplayer.ACTION_PLAY"
        const val ACTION_PAUSE = "com.petitbear.catuplayer.ACTION_PAUSE"
        const val ACTION_NEXT = "com.petitbear.catuplayer.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.petitbear.catuplayer.ACTION_PREVIOUS"
    }
}