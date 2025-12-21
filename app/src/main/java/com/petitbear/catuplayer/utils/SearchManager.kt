package com.petitbear.catuplayer.utils

import android.content.Context
import android.util.Log
import com.petitbear.catuplayer.data.SearchIndexDao
import com.petitbear.catuplayer.models.SearchIndex
import com.petitbear.catuplayer.models.SearchResult
import com.petitbear.catuplayer.models.Song
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*
import kotlin.math.*
import me.xdrop.fuzzywuzzy.FuzzySearch

/**
 * 增强的搜索管理器，使用贝叶斯算法进行智能匹配
 */
class SearchManager(
    private val context: Context,
    private val searchIndexDao: SearchIndexDao
) {

    private val TAG = "SearchManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { prettyPrint = true }

    // 贝叶斯搜索相关（内存缓存）
    private val keywordFrequencyCache = mutableMapOf<String, Int>()
    private val documentFrequencyCache = mutableMapOf<String, Int>()
    private var totalDocumentsCache = 0

    /**
     * 为歌曲建立搜索索引并保存到数据库
     */
    suspend fun buildIndexForSong(song: Song) = withContext(Dispatchers.IO) {
        try {
            // 删除旧的索引
            searchIndexDao.deleteBySongId(song.id)

            // 生成所有搜索关键词
            val indices = mutableListOf<SearchIndex>()
            val baseWeight = calculateSongWeight(song)

            // 标题索引
            val titleIndices = buildFieldIndices(
                song.id,
                SearchIndex.FIELD_TITLE,
                song.title,
                baseWeight * 1.5f
            )
            indices.addAll(titleIndices)

            // 艺术家索引
            val artistIndices = buildFieldIndices(
                song.id,
                SearchIndex.FIELD_ARTIST,
                song.artist,
                baseWeight * 1.2f
            )
            indices.addAll(artistIndices)

            // 专辑索引
            val albumIndices = buildFieldIndices(
                song.id,
                SearchIndex.FIELD_ALBUM,
                song.album,
                baseWeight * 1.0f
            )
            indices.addAll(albumIndices)

            // 保存索引到数据库
            searchIndexDao.insertAll(indices)

            // 更新统计信息缓存
            updateStatistics(indices)

            Log.d(TAG, "为歌曲 ${song.title} 建立 ${indices.size} 个索引")
        } catch (e: Exception) {
            Log.e(TAG, "建立索引失败: ${e.message}", e)
        }
    }

    /**
     * 建立字段索引
     */
    private fun buildFieldIndices(
        songId: String,
        fieldType: String,
        fieldValue: String,
        baseWeight: Float
    ): List<SearchIndex> {
        if (fieldValue.isBlank()) return emptyList()

        val indices = mutableListOf<SearchIndex>()
        val searchForms = getAllSearchForms(fieldValue)

        searchForms.forEach { form ->
            // 分词处理
            val keywords = tokenizeText(form)
            keywords.forEach { keyword ->
                if (keyword.length >= 1) {
                    val weight = calculateKeywordWeight(keyword, fieldValue) * baseWeight
                    val index = SearchIndex(
                        songId = songId,
                        keyword = keyword.lowercase(),
                        fieldType = fieldType,
                        originalText = fieldValue,
                        weight = weight,
                        matchScore = 0f,
                        lastMatched = System.currentTimeMillis(),
                        matchCount = 0
                    )
                    indices.add(index)
                }
            }
        }

        return indices.distinctBy { "${it.keyword}_${it.fieldType}" }
    }

    /**
     * 获取所有搜索形式（中文、拼音、日文、罗马音等）
     */
    private fun getAllSearchForms(text: String): List<String> {
        val forms = mutableListOf<String>()

        // 原始文本
        forms.add(text)

        // 小写形式
        forms.add(text.lowercase())

        // 中文拼音转换
        if (PinyinSearchUtils.containsChinese(text)) {
            forms.add(PinyinSearchUtils.toPinyin(text))
            forms.add(PinyinSearchUtils.toPinyinInitials(text))
        }

        // 日文罗马音转换
        if (PinyinSearchUtils.containsJapanese(text)) {
            forms.add(PinyinSearchUtils.toRomaji(text))
            forms.add(PinyinSearchUtils.toRomajiInitials(text))
        }

        // 分词形式
        val tokens = tokenizeText(text)
        forms.addAll(tokens)

        return forms.distinct()
    }

    /**
     * 文本分词
     */
    private fun tokenizeText(text: String): List<String> {
        val tokens = mutableListOf<String>()

        // 按空格、标点分割
        val parts = text.split(Regex("[\\s\\p{Punct}]+"))
        tokens.addAll(parts.filter { it.isNotEmpty() })

        // N-gram 分词 (1-4个字符)
        val cleanText = text.replace(Regex("[\\s\\p{Punct}]+"), "")
        if (cleanText.length >= 2) {
            for (n in 1..min(4, cleanText.length)) {
                for (i in 0..cleanText.length - n) {
                    tokens.add(cleanText.substring(i, i + n).lowercase())
                }
            }
        }

        return tokens.distinct()
    }

    /**
     * 计算歌曲权重（基于播放次数、收藏等）
     */
    private fun calculateSongWeight(song: Song): Float {
        var weight = 1.0f

        // 播放次数加成
        weight += ln((song.playCount + 1).toFloat()) * 0.3f

        // 收藏加成
        if (song.isFavorite) weight *= 1.5f

        // 最近播放加成（7天内）
        if (song.lastPlayedTime > 0) {
            val days = (System.currentTimeMillis() - song.lastPlayedTime) / (1000 * 60 * 60 * 24)
            if (days < 7) weight *= 1.3f
        }

        return weight.coerceIn(0.5f, 5.0f)
    }

    /**
     * 计算关键词权重
     */
    private fun calculateKeywordWeight(keyword: String, originalText: String): Float {
        var weight = 1.0f

        when {
            originalText.equals(keyword, ignoreCase = true) -> weight *= 2.0f
            originalText.startsWith(keyword, ignoreCase = true) -> weight *= 1.8f
            originalText.contains(keyword, ignoreCase = true) -> weight *= 1.5f
            else -> weight *= 1.0f
        }

        // 关键词长度权重
        weight *= when (keyword.length) {
            1 -> 0.5f
            2 -> 0.8f
            3 -> 1.2f
            4 -> 1.5f
            else -> 1.0f
        }

        return weight
    }

    /**
     * 从数据库加载统计信息
     */
    private suspend fun loadStatistics() {
        try {
            // 加载词频统计
            val keywordFrequencies = searchIndexDao.getAllKeywordFrequencies()
            keywordFrequencyCache.clear()
            keywordFrequencies.forEach { kf ->
                keywordFrequencyCache[kf.keyword] = kf.frequency
            }

            // 加载文档频率统计
            val documentFrequencies = searchIndexDao.getAllDocumentFrequencies()
            documentFrequencyCache.clear()
            documentFrequencies.forEach { df ->
                documentFrequencyCache[df.keyword] = df.frequency
            }

            // 加载总文档数
            totalDocumentsCache = searchIndexDao.getTotalDocuments()
        } catch (e: Exception) {
            Log.e(TAG, "加载统计信息失败: ${e.message}", e)
        }
    }

    /**
     * 更新统计信息
     */
    private fun updateStatistics(indices: List<SearchIndex>) {
        totalDocumentsCache++
        indices.forEach { index ->
            keywordFrequencyCache[index.keyword] = keywordFrequencyCache.getOrDefault(index.keyword, 0) + 1
            documentFrequencyCache[index.keyword] = documentFrequencyCache.getOrDefault(index.keyword, 0) + 1
        }
    }

    /**
     * 执行搜索（贝叶斯算法）
     */
    suspend fun search(query: String, allSongs: List<Song>): List<SearchResult> = withContext(Dispatchers.Default) {
        if (query.isBlank() || allSongs.isEmpty()) return@withContext emptyList()

        Log.d(TAG, "开始搜索: $query, 歌曲总数: ${allSongs.size}")

        // 确保统计信息已加载
        if (keywordFrequencyCache.isEmpty()) {
            loadStatistics()
        }

        val normalizedQuery = query.trim().lowercase()
        val results = mutableListOf<SearchResult>()
        val queryKeywords = tokenizeText(normalizedQuery)

        Log.d(TAG, "查询关键词: $queryKeywords")

        // 为每个关键词计算先验概率
        val keywordPriorProb = mutableMapOf<String, Float>()
        queryKeywords.forEach { keyword ->
            val freq = keywordFrequencyCache[keyword] ?: 0
            keywordPriorProb[keyword] = if (totalDocumentsCache > 0) freq.toFloat() / totalDocumentsCache else 0.001f
        }

        // 对每首歌曲计算匹配分数
        allSongs.forEach { song ->
            try {
                val songIndices = searchIndexDao.getBySongId(song.id)
                
                if (songIndices.isNotEmpty()) {
                    val score = calculateBayesianScore(song, songIndices, queryKeywords, keywordPriorProb)
                    if (score > 0) {
                        val result = SearchResult(
                            song = song,
                            score = score,
                            matchType = determineMatchType(song, queryKeywords),
                            matchedKeywords = findMatchedKeywords(songIndices, queryKeywords)
                        )
                        results.add(result)
                    }
                } else {
                    // 如果没有建立索引，使用简单的匹配方式
                    val score = calculateSimpleScore(song, normalizedQuery)
                    if (score > 0) {
                        val result = SearchResult(
                            song = song,
                            score = score,
                            matchType = determineMatchType(song, queryKeywords),
                            matchedKeywords = queryKeywords.filter { keyword ->
                                song.title.contains(keyword, ignoreCase = true) ||
                                        song.artist.contains(keyword, ignoreCase = true) ||
                                        song.album.contains(keyword, ignoreCase = true)
                            }
                        )
                        results.add(result)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "搜索歌曲 ${song.title} 失败: ${e.message}", e)
            }
        }

        // 排序并限制数量
        results.sortByDescending { it.score }
        Log.d(TAG, "搜索完成，找到 ${results.size} 个结果")
        return@withContext results.take(100)
    }

    /**
     * 贝叶斯评分算法
     */
    private fun calculateBayesianScore(
        song: Song,
        songIndices: List<SearchIndex>,
        queryKeywords: List<String>,
        keywordPriorProb: Map<String, Float>
    ): Float {
        if (queryKeywords.isEmpty()) return 0f

        var totalScore = 0f
        var matchedKeywordCount = 0

        // 为每个查询关键词计算与歌曲的匹配度
        queryKeywords.forEach { keyword ->
            val matchingIndices = songIndices.filter {
                it.keyword.contains(keyword) ||
                        FuzzySearch.ratio(it.keyword, keyword) > 70 ||
                        (PinyinSearchUtils.containsChinese(it.keyword) && 
                         (PinyinSearchUtils.toPinyin(it.keyword).contains(keyword) ||
                          PinyinSearchUtils.toPinyinInitials(it.keyword).contains(keyword))) ||
                        (PinyinSearchUtils.containsJapanese(it.keyword) && 
                         (PinyinSearchUtils.toRomaji(it.keyword).contains(keyword) ||
                          PinyinSearchUtils.toRomajiInitials(it.keyword).contains(keyword)))
            }

            if (matchingIndices.isNotEmpty()) {
                matchedKeywordCount++
                
                // 计算TF（词频）
                val tf = matchingIndices.size.toFloat() / songIndices.size

                // 计算IDF（逆文档频率）
                val docFreq = documentFrequencyCache[keyword] ?: 1
                val idf = if (totalDocumentsCache > 0) {
                    ln((totalDocumentsCache + 1).toFloat() / (docFreq + 1))
                } else {
                    1f
                }

                // 计算权重平均值
                val avgWeight = matchingIndices.map { it.weight }.average().toFloat()

                // 贝叶斯公式：P(关键词|歌曲) = TF * IDF * 权重
                val likelihood = tf * idf * avgWeight

                // 先验概率
                val prior = keywordPriorProb[keyword] ?: 0.001f

                // 后验概率近似计算
                val posterior = likelihood * prior
                totalScore += posterior
            }
        }

        // 如果没有匹配的关键词，返回0
        if (matchedKeywordCount == 0) return 0f

        // 添加歌曲自身权重
        val songWeight = calculateSongWeight(song)
        totalScore *= songWeight

        // 标准化分数到0-100范围
        val normalizedScore = (totalScore * 100).coerceIn(0f, 100f)
        
        // 如果分数太低，给予基础分数以确保至少显示一些结果
        return if (normalizedScore < 1f && matchedKeywordCount > 0) {
            1f + (matchedKeywordCount * 0.5f)
        } else {
            normalizedScore
        }
    }

    /**
     * 简单评分算法（用于未建立索引的歌曲）
     */
    private fun calculateSimpleScore(song: Song, query: String): Float {
        var score = 0f

        // 检查标题匹配
        if (song.title.contains(query, ignoreCase = true)) {
            score += 50f
        }

        // 检查艺术家匹配
        if (song.artist.contains(query, ignoreCase = true)) {
            score += 30f
        }

        // 检查专辑匹配
        if (song.album.contains(query, ignoreCase = true)) {
            score += 20f
        }

        return score * calculateSongWeight(song) / 100f
    }

    /**
     * 确定匹配类型
     */
    private fun determineMatchType(song: Song, keywords: List<String>): String {
        val titleForms = getAllSearchForms(song.title)
        val artistForms = getAllSearchForms(song.artist)
        val albumForms = getAllSearchForms(song.album)

        return when {
            keywords.any { kw -> titleForms.any {
                it.contains(kw, ignoreCase = true) ||
                        FuzzySearch.ratio(it, kw) > 70
            }} -> SearchResult.MATCH_TITLE

            keywords.any { kw -> artistForms.any {
                it.contains(kw, ignoreCase = true) ||
                        FuzzySearch.ratio(it, kw) > 70
            }} -> SearchResult.MATCH_ARTIST

            keywords.any { kw -> albumForms.any {
                it.contains(kw, ignoreCase = true) ||
                        FuzzySearch.ratio(it, kw) > 70
            }} -> SearchResult.MATCH_ALBUM

            else -> SearchResult.MATCH_COMPOSITE
        }
    }

    /**
     * 查找匹配的关键词
     */
    private fun findMatchedKeywords(songIndices: List<SearchIndex>, queryKeywords: List<String>): List<String> {
        val matched = mutableSetOf<String>()

        queryKeywords.forEach { queryKeyword ->
            songIndices.forEach { index ->
                if (index.keyword.contains(queryKeyword) ||
                    queryKeyword.contains(index.keyword) ||
                    FuzzySearch.ratio(index.keyword, queryKeyword) > 70) {
                    matched.add(queryKeyword)
                }
            }
        }

        return matched.toList()
    }

    /**
     * 批量建立索引
     */
    suspend fun buildIndexForSongs(songs: List<Song>) = withContext(Dispatchers.IO) {
        // 清空现有索引
        searchIndexDao.deleteAll()

        // 重置统计信息缓存
        keywordFrequencyCache.clear()
        documentFrequencyCache.clear()
        totalDocumentsCache = 0

        // 分批建立索引，避免内存溢出
        val batchSize = 50
        songs.chunked(batchSize).forEachIndexed { batchIndex, batch ->
            batch.forEach { song ->
                buildIndexForSong(song)
            }
            Log.d(TAG, "批量 ${batchIndex + 1}: 为 ${batch.size} 首歌曲建立索引")
        }
        Log.d(TAG, "为 ${songs.size} 首歌曲建立索引完成")

        // 重新加载统计信息
        loadStatistics()
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        scope.cancel()
    }
}