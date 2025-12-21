// models/PlaylistEntity.kt
package com.petitbear.catuplayer.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.petitbear.catuplayer.utils.Converters
import java.util.UUID

@Entity(tableName = "playlists")
@TypeConverters(Converters::class)
data class PlaylistEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val songIds: List<String> = emptyList(),
    val coverUri: String = "",
    val createdTime: Long = System.currentTimeMillis(),
    val lastModifiedTime: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false,
    val isSmartPlaylist: Boolean = false,
    val smartFilter: Map<String, String> = emptyMap() // 智能播放列表过滤条件
) {
    val songCount: Int
        get() = songIds.size

    val formattedDuration: String
        get() = "未知时长" // 可以通过计算获得

    val isStatic: Boolean
        get() = !isSmartPlaylist
}