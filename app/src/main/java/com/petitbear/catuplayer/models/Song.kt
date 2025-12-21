// models/Song.kt
package com.petitbear.catuplayer.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.petitbear.catuplayer.utils.MusicMetadataUtils
import kotlinx.serialization.Serializable

// 歌曲模型类
@Serializable
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val uri: String,
    val album: String = "",
    val hasMetadata: Boolean = false,
    val coverUri: String = "", // 专辑封面文件路径
    val hasEmbeddedCover: Boolean = false, // 是否包含内嵌封面
    val lrcCachePath: String = "", // 歌词缓存路径
    val hasEmbeddedLyric: Boolean = false, // 是否包含内嵌歌词
    val lyricDownloadUrl: String = "", // 网络歌词下载URL（可选）
    val lyricSource: String = "", // 歌词来源：embedded, local, network, none
    val filePath: String = "", // 文件路径
    val fileSize: Long = 0, // 文件大小
    val createdTime: Long = System.currentTimeMillis(), // 创建时间
    val lastPlayedTime: Long = 0, // 最后播放时间
    val playCount: Int = 0, // 播放次数
    val bitrate: Int = 0, // 比特率
    val sampleRate: Int = 0, // 采样率
    val channels: Int = 2, // 声道数
    val fileFormat: String = "", // 文件格式
    val genre: String = "", // 流派
    val year: Int = 0, // 年份
    val trackNumber: Int = 0, // 音轨号
    val discNumber: Int = 1, // 碟片号
    val isFavorite: Boolean = false, // 是否收藏
    val tags: String = "", // 标签（逗号分隔）
    val audioFeatures: String = "" // 音频特征（JSON格式，可选）
) {
    companion object {
        fun createId(uri: String): String {
            return "file_${uri.hashCode()}"
        }
    }

    val formattedDuration: String
        get() = MusicMetadataUtils.formatDuration(duration)

    val isValid: Boolean
        get() = duration > 0 && uri.isNotEmpty()

    // 检查是否可以播放（有URI且URI不为空）
    val canPlay: Boolean
        get() = uri.isNotEmpty()

    // 检查是否有封面（内嵌封面或外部封面）
    val hasCover: Boolean
        get() = coverUri.isNotEmpty()

    // 检查是否有歌词（缓存或内嵌）
    val hasLyric: Boolean
        get() = lrcCachePath.isNotEmpty() || hasEmbeddedLyric || lyricSource.isNotEmpty()

    // 获取音频质量等级
    val audioQuality: String
        get() = when {
            bitrate >= 320 -> "高质量"
            bitrate >= 256 -> "高"
            bitrate >= 192 -> "中等"
            bitrate >= 128 -> "标准"
            else -> "低"
        }

    // 获取文件大小格式化显示
    val formattedFileSize: String
        get() = when {
            fileSize == 0L -> "未知大小"
            fileSize < 1024 -> "${fileSize} B"
            fileSize < 1024 * 1024 -> "${String.format("%.1f", fileSize / 1024.0)} KB"
            else -> "${String.format("%.1f", fileSize / (1024.0 * 1024.0))} MB"
        }
}