package com.petitbear.catuplayer.utils

import android.media.MediaMetadataRetriever
import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
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

                // 获取专辑封面
                val embeddedPicture = retriever.embeddedPicture
                val hasEmbeddedCover = embeddedPicture != null
                var coverUri = ""

                // 如果有内嵌封面，保存到缓存目录
                if (hasEmbeddedCover) {
                    coverUri = saveCoverToCache(context, embeddedPicture, uri.toString())
                }

                MusicMetadata(
                    duration = duration,
                    title = title,
                    artist = artist,
                    album = album,
                    year = year,
                    bitrate = bitrate,
                    mimeType = mimeType,
                    embeddedPicture = embeddedPicture,
                    hasEmbeddedCover = hasEmbeddedCover,
                    coverUri = coverUri
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
     * 将封面保存到缓存目录
     */
    private fun saveCoverToCache(context: Context, embeddedPicture: ByteArray, uriString: String): String {
        return try {
            // 创建缓存目录
            val cacheDir = File(context.getExternalFilesDir(null), "covers")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            // 生成唯一的文件名
            val fileName = "cover_${uriString.hashCode()}.jpg"
            val coverFile = File(cacheDir, fileName)

            // 保存封面图片
            FileOutputStream(coverFile).use { outputStream ->
                outputStream.write(embeddedPicture)
            }

            // 返回文件路径
            coverFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
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
                hasMetadata = true,
                coverUri = metadata.coverUri,
                hasEmbeddedCover = metadata.hasEmbeddedCover
            )
        } else {
            // 回退到文件名解析
            val (artist, title) = FilePickerUtils.parseMusicFileName(fileName)
            MusicInfo(
                title = title,
                artist = artist,
                duration = 0,
                hasMetadata = false,
                coverUri = "",
                hasEmbeddedCover = false
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

    /**
     * 从字节数组创建Bitmap
     */
    fun createBitmapFromEmbeddedPicture(embeddedPicture: ByteArray?): Bitmap? {
        return embeddedPicture?.let {
            try {
                BitmapFactory.decodeStream(ByteArrayInputStream(it))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * 清除封面缓存
     */
    suspend fun clearCoverCache(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val cacheDir = File(context.getExternalFilesDir(null), "covers")
                if (cacheDir.exists() && cacheDir.isDirectory) {
                    cacheDir.listFiles()?.forEach { file ->
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 获取缓存目录大小
     */
    suspend fun getCoverCacheSize(context: Context): Long {
        return withContext(Dispatchers.IO) {
            try {
                val cacheDir = File(context.getExternalFilesDir(null), "covers")
                if (cacheDir.exists() && cacheDir.isDirectory) {
                    cacheDir.listFiles()?.sumOf { it.length() } ?: 0
                } else {
                    0
                }
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }
}

data class MusicMetadata(
    val duration: Long,
    val title: String,
    val artist: String,
    val album: String,
    val year: String,
    val bitrate: String,
    val mimeType: String,
    val embeddedPicture: ByteArray?, // 内嵌封面数据
    val hasEmbeddedCover: Boolean,   // 是否有内嵌封面
    val coverUri: String             // 封面缓存文件路径
) {
    fun isValid(): Boolean {
        return duration > 0 || title.isNotEmpty() || artist.isNotEmpty()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MusicMetadata

        if (duration != other.duration) return false
        if (title != other.title) return false
        if (artist != other.artist) return false
        if (album != other.album) return false
        if (year != other.year) return false
        if (bitrate != other.bitrate) return false
        if (mimeType != other.mimeType) return false
        if (hasEmbeddedCover != other.hasEmbeddedCover) return false
        if (coverUri != other.coverUri) return false
        if (embeddedPicture != null) {
            if (other.embeddedPicture == null) return false
            if (!embeddedPicture.contentEquals(other.embeddedPicture)) return false
        } else if (other.embeddedPicture != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = duration.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + year.hashCode()
        result = 31 * result + bitrate.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (embeddedPicture?.contentHashCode() ?: 0)
        result = 31 * result + hasEmbeddedCover.hashCode()
        result = 31 * result + coverUri.hashCode()
        return result
    }
}

data class MusicInfo(
    val title: String,
    val artist: String,
    val duration: Long,
    val hasMetadata: Boolean,
    val coverUri: String,           // 封面缓存文件路径
    val hasEmbeddedCover: Boolean  // 是否有内嵌封面
)