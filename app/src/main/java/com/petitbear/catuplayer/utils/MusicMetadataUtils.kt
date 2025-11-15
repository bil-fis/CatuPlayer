package com.petitbear.catuplayer.utils

import android.media.MediaMetadataRetriever
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MusicMetadataUtils {
    /**
     * 从音乐文件 URI 获取元数据
     */
    suspend fun getMusicMetadata(context: Context, uri: Uri): MusicMetadata? {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)

                // 获取持续时间（毫秒）
                val duration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0

                // 获取标题
                val title = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_TITLE
                ) ?: ""

                // 获取艺术家
                val artist = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_ARTIST
                ) ?: ""

                // 获取专辑
                val album = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_ALBUM
                ) ?: ""

                // 获取年份
                val year = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_YEAR
                ) ?: ""

                // 获取比特率
                val bitrate = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_BITRATE
                ) ?: ""

                // 获取 MIME 类型
                val mimeType = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_MIMETYPE
                ) ?: ""

                MusicMetadata(
                    duration = duration,
                    title = title,
                    artist = artist,
                    album = album,
                    year = year,
                    bitrate = bitrate,
                    mimeType = mimeType
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                retriever.release()
            }
        }
    }

    /**
     * 智能解析音乐信息
     * 优先使用元数据，如果元数据缺失则回退到文件名解析
     */
    suspend fun parseMusicInfo(context: Context, uri: Uri, fileName: String): MusicInfo {
        val metadata = getMusicMetadata(context, uri)

        return if (metadata != null && metadata.isValid()) {
            // 使用元数据
            MusicInfo(
                title = if (metadata.title.isNotEmpty()) metadata.title else fileName.substringBeforeLast("."),
                artist = if (metadata.artist.isNotEmpty()) metadata.artist else "未知艺术家",
                duration = metadata.duration,
                hasMetadata = true
            )
        } else {
            // 回退到文件名解析
            val (artist, title) = FilePickerUtils.parseMusicFileName(fileName)
            MusicInfo(
                title = title,
                artist = artist,
                duration = 0,
                hasMetadata = false
            )
        }
    }

    /**
     * 格式化持续时间（毫秒 -> 分:秒）
     */
    fun formatDuration(milliseconds: Long): String {
        if (milliseconds <= 0) return "00:00"

        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d", minutes, seconds)
    }
}

data class MusicMetadata(
    val duration: Long,
    val title: String,
    val artist: String,
    val album: String,
    val year: String,
    val bitrate: String,
    val mimeType: String
) {
    fun isValid(): Boolean {
        return duration > 0 || title.isNotEmpty() || artist.isNotEmpty()
    }
}

data class MusicInfo(
    val title: String,
    val artist: String,
    val duration: Long,
    val hasMetadata: Boolean
)