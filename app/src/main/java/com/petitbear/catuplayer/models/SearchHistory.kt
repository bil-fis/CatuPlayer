package com.petitbear.catuplayer.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 搜索历史实体
 */
@Entity(
    tableName = "search_history",
    indices = [
        Index(value = ["query"]),
        Index(value = ["timestamp"]),
        Index(value = ["isPinned"])
    ]
)
@Serializable
data class SearchHistory(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val query: String,
    val resultCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val clickCount: Int = 1,
    val lastClicked: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)