package com.petitbear.catuplayer.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 搜索索引实体
 */
@Entity(
    tableName = "search_indices",
    indices = [
        Index(value = ["songId"]),
        Index(value = ["keyword"]),
        Index(value = ["fieldType"]),
        Index(value = ["songId", "keyword"])
    ]
)
@Serializable
data class SearchIndex(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val songId: String,
    val keyword: String,
    val fieldType: String, // "title", "artist", "album"
    val originalText: String,
    val weight: Float = 1.0f,
    val matchScore: Float = 0f,
    val lastMatched: Long = System.currentTimeMillis(),
    val matchCount: Int = 0
) {
    companion object {
        const val FIELD_TITLE = "title"
        const val FIELD_ARTIST = "artist"
        const val FIELD_ALBUM = "album"
    }
}