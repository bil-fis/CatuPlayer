package com.petitbear.catuplayer.models

import kotlinx.serialization.Serializable

/**
 * 搜索结果
 */
@Serializable
data class SearchResult(
    val song: Song,
    val score: Float, // 匹配分数 (0-100)
    val matchType: String, // 匹配类型
    val matchedKeywords: List<String> = emptyList()
) : Comparable<SearchResult> {
    companion object {
        const val MATCH_TITLE = "title"
        const val MATCH_ARTIST = "artist"
        const val MATCH_ALBUM = "album"
        const val MATCH_COMPOSITE = "composite"
    }

    override fun compareTo(other: SearchResult): Int {
        return other.score.compareTo(score) // 降序排序
    }
}