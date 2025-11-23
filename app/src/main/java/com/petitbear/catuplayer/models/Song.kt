package com.petitbear.catuplayer.models

import com.petitbear.catuplayer.utils.MusicMetadataUtils
import kotlinx.serialization.Serializable

// 歌曲模型类
// Song.kt
@Serializable
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val uri: String,
    val album: String = "",
    val hasMetadata: Boolean = false,
    val coverUri: String = "",
    val lrcUri: String = ""

) {
    val formattedDuration: String
        get() = MusicMetadataUtils.formatDuration(duration)

    val isValid: Boolean
        get() = duration > 0 && uri.isNotEmpty()

    // 检查是否可以播放（有URI且URI不为空）
    val canPlay: Boolean
        get() = uri.isNotEmpty()
}