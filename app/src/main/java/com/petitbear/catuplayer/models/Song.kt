package com.petitbear.catuplayer.models

import com.petitbear.catuplayer.utils.MusicMetadataUtils
import kotlinx.serialization.Serializable

// 歌曲模型类
@Serializable
data class Song(
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

) {
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
}