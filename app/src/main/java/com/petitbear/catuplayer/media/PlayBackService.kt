package com.petitbear.catuplayer.media

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.petitbear.catuplayer.models.AudioPlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlayBackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    // 自定义命令
    companion object {
        const val CUSTOM_ACTION_PREVIOUS = "com.petitbear.catuplayer.PREVIOUS"
        const val CUSTOM_ACTION_NEXT = "com.petitbear.catuplayer.NEXT"
        const val CUSTOM_ACTION_TOGGLE_PLAYBACK = "com.petitbear.catuplayer.TOGGLE_PLAYBACK"
    }

    override fun onCreate() {
        super.onCreate()
        initializeMediaSession()
    }

    private fun initializeMediaSession() {
        val player = ExoPlayer.Builder(this).build()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MediaSessionCallback())
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        // 可以在这里更新自定义布局
                    }
                    Player.STATE_ENDED -> {
                        // 播放结束处理
                    }
                }
            }
        })
    }

    private fun sendPlaybackAction(action: String) {
        val intent = Intent("com.petitbear.catuplayer.PLAYBACK_ACTION").apply {
            putExtra("action", action)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player?.isPlaying != true) {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    // 自定义 MediaSession 回调
    @UnstableApi
    inner class MediaSessionCallback : MediaSession.Callback {
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: android.os.Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CUSTOM_ACTION_PREVIOUS -> {
                    sendPlaybackAction("previous")
                    return Futures.immediateFuture(
                        SessionResult(SessionResult.RESULT_SUCCESS)
                    )
                }
                CUSTOM_ACTION_NEXT -> {
                    sendPlaybackAction("next")
                    return Futures.immediateFuture(
                        SessionResult(SessionResult.RESULT_SUCCESS)
                    )
                }
                CUSTOM_ACTION_TOGGLE_PLAYBACK -> {
                    sendPlaybackAction("toggle")
                    return Futures.immediateFuture(
                        SessionResult(SessionResult.RESULT_SUCCESS)
                    )
                }
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }
}