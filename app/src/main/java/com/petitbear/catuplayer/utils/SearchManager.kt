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
     * 建立字段索引 - 支持多艺术家分割
     */
    private fun buildFieldIndices(
        songId: String,
        fieldType: String,
        fieldValue: String,
        baseWeight: Float
    ): List<SearchIndex> {
        if (fieldValue.isBlank()) return emptyList()

        val indices = mutableListOf<SearchIndex>()
        
        // 如果是艺术家字段，先分割再分别建立索引
        if (fieldType == SearchIndex.FIELD_ARTIST) {
            val artists = splitArtists(fieldValue)
            artists.forEach { artist ->
                val searchForms = getAllSearchForms(artist)
                searchForms.forEach { form ->
                    val keywords = tokenizeText(form)
                    keywords.forEach { keyword ->
                        if (keyword.length >= 1) {
                            val weight = calculateKeywordWeight(keyword, artist) * baseWeight
                            val index = SearchIndex(
                                songId = songId,
                                keyword = keyword.lowercase(),
                                fieldType = fieldType,
                                originalText = artist, // 使用分割后的艺术家名称
                                weight = weight,
                                matchScore = 0f,
                                lastMatched = System.currentTimeMillis(),
                                matchCount = 0
                            )
                            indices.add(index)
                        }
                    }
                }
            }
        } else {
            // 非艺术家字段，按原逻辑处理
            val searchForms = getAllSearchForms(fieldValue)
            searchForms.forEach { form ->
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
        }

        return indices.distinctBy { "${it.keyword}_${it.fieldType}_${it.originalText}" }
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
     * 计算歌曲权重（基于播放次数、收藏等，但作为参考因素）
     */
    private fun calculateSongWeight(song: Song): Float {
        var weight = 1.0f

        // 播放次数加成（降低影响）
        weight += ln((song.playCount + 1).toFloat()) * 0.15f // 从0.3f降到0.15f

        // 收藏加成（降低影响，仅作为参考）
        if (song.isFavorite) weight *= 1.2f // 从1.5f降到1.2f

        // 最近播放加成（7天内，降低影响）
        if (song.lastPlayedTime > 0) {
            val days = (System.currentTimeMillis() - song.lastPlayedTime) / (1000 * 60 * 60 * 24)
            if (days < 7) weight *= 1.15f // 从1.3f降到1.15f
        }

        return weight.coerceIn(0.8f, 3.0f) // 缩小权重范围
    }

    /**
     * 计算关键词权重 - 修复优先级问题
     */
    private fun calculateKeywordWeight(keyword: String, originalText: String): Float {
        var weight = 1.0f

        when {
            // 完全匹配权重最高
            originalText.equals(keyword, ignoreCase = true) -> weight *= 5.0f
            // 开头匹配权重次之
            originalText.startsWith(keyword, ignoreCase = true) -> weight *= 4.0f
            // 包含匹配
            originalText.contains(keyword, ignoreCase = true) -> weight *= 3.0f
            else -> weight *= 1.0f
        }

        // 关键词长度权重 - 调整短关键词权重，但不要过度降低
        weight *= when (keyword.length) {
            1 -> 0.5f  // 单字符匹配权重适度降低
            2 -> 0.8f  // 双字符匹配权重略微降低
            3 -> 1.0f  // 三字符匹配权重保持
            4 -> 1.2f
            else -> 1.0f
        }

        // 如果关键词是艺术家名称的一部分，需要特别处理
        if (keyword.length >= 2 && originalText.contains(keyword, ignoreCase = true)) {
            // 检查是否是独立的艺术家名称部分
            val parts = splitArtists(originalText)
            if (parts.any { it.contains(keyword, ignoreCase = true) }) {
                weight *= 2.0f // 艺术家名称匹配额外加成
            }
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
     * 执行搜索（改进的贝叶斯算法 + 优化优先级）
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
                    // 如果没有建立索引，使用改进的简单匹配方式
                    val score = calculateImprovedSimpleScore(song, normalizedQuery, queryKeywords)
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

        // 排序并应用相关度过滤
        results.sortByDescending { it.score }
        
        // 优化过滤逻辑：提高阈值，减少不相关结果
        val highRelevanceResults = results.filter { it.score >= 40f }
        val mediumRelevanceResults = results.filter { it.score in 20f..<40f }
        val lowRelevanceResults = results.filter { it.score in 10f..<20f }
        
        // 最终结果组合：优先显示高相关度，限制中等相关度数量
        val finalResults = mutableListOf<SearchResult>()
        finalResults.addAll(highRelevanceResults)
        finalResults.addAll(mediumRelevanceResults.take(15))
        finalResults.addAll(lowRelevanceResults.take(5))
        
        Log.d(TAG, "搜索完成，高相关度: ${highRelevanceResults.size}, 中等相关度: ${mediumRelevanceResults.size}, 低相关度: ${lowRelevanceResults.size}, 总计: ${finalResults.size}")
        return@withContext finalResults
    }

    /**
     * 改进的简单评分算法（用于未建立索引的歌曲）
     */
    private fun calculateImprovedSimpleScore(song: Song, query: String, queryKeywords: List<String>): Float {
        var score = 0f

        queryKeywords.forEach { keyword ->
            // 标题匹配 - 提高分数
            when {
                song.title.equals(keyword, ignoreCase = true) -> score += 80f
                song.title.startsWith(keyword, ignoreCase = true) -> score += 65f
                song.title.contains(keyword, ignoreCase = true) -> score += 50f
            }

            // 艺术家匹配 - 提高分数
            val artists = splitArtists(song.artist)
            artists.forEach { artist ->
                when {
                    artist.equals(keyword, ignoreCase = true) -> score += 70f
                    artist.startsWith(keyword, ignoreCase = true) -> score += 55f
                    artist.contains(keyword, ignoreCase = true) -> score += 40f
                }
            }

            // 专辑匹配 - 提高分数
            when {
                song.album.equals(keyword, ignoreCase = true) -> score += 35f
                song.album.startsWith(keyword, ignoreCase = true) -> score += 25f
                song.album.contains(keyword, ignoreCase = true) -> score += 15f
            }
        }

        // 添加歌曲自身权重（降低影响）
        val songWeight = calculateSongWeight(song)
        return score * (0.8f + 0.2f * (songWeight / 3.0f))
    }

    /**
     * 改进的贝叶斯评分算法 - 分离字段计算
     */
    private fun calculateBayesianScore(
        song: Song,
        songIndices: List<SearchIndex>,
        queryKeywords: List<String>,
        keywordPriorProb: Map<String, Float>
    ): Float {
        if (queryKeywords.isEmpty()) return 0f

        // 分离不同字段的索引
        val titleIndices = songIndices.filter { it.fieldType == SearchIndex.FIELD_TITLE }
        val artistIndices = songIndices.filter { it.fieldType == SearchIndex.FIELD_ARTIST }
        val albumIndices = songIndices.filter { it.fieldType == SearchIndex.FIELD_ALBUM }

        // 分别计算各字段匹配分数，提高基础权重
        val titleScore = calculateFieldScore(song.title, titleIndices, queryKeywords, keywordPriorProb, 3.0f) // 标题权重最高
        val artistScore = calculateFieldScore(song.artist, artistIndices, queryKeywords, keywordPriorProb, 2.0f) // 艺术家权重次之
        val albumScore = calculateFieldScore(song.album, albumIndices, queryKeywords, keywordPriorProb, 1.0f) // 专辑权重最低

        // 组合分数（加权平均），调整权重分配
        var totalScore = (titleScore * 0.6f + artistScore * 0.3f + albumScore * 0.1f)

        // 如果没有任何匹配，返回0
        if (totalScore == 0f) return 0f

        // 添加歌曲自身权重（收藏等作为参考，不占主导）
        val songWeight = calculateSongWeight(song)
        totalScore *= (0.8f + 0.2f * (songWeight / 3.0f)) // 收藏等最多增加20%权重

        // 标准化分数到0-100范围，提高基础分数
        val normalizedScore = (totalScore * 150).coerceIn(0f, 100f) // 提高乘数从100到150
        
        return normalizedScore
    }

    /**
     * 计算单个字段的匹配分数 - 修复多艺术家匹配逻辑
     */
    private fun calculateFieldScore(
        fieldValue: String,
        fieldIndices: List<SearchIndex>,
        queryKeywords: List<String>,
        keywordPriorProb: Map<String, Float>,
        fieldWeight: Float
    ): Float {
        if (fieldValue.isBlank() || queryKeywords.isEmpty()) return 0f
        
        var fieldScore = 0f
        var matchedKeywordCount = 0

        // 对于艺术家字段，直接使用所有索引，不限制originalText匹配
        // 因为建立索引时已经为每个分割的艺术家创建了独立的索引
        val isArtistField = fieldIndices.isNotEmpty() && fieldIndices.first().fieldType == SearchIndex.FIELD_ARTIST

        queryKeywords.forEach { keyword ->
            val matchingIndices = if (isArtistField) {
                // 艺术家字段：查找所有包含关键词的索引
                fieldIndices.filter { index ->
                    index.keyword.contains(keyword) ||
                    FuzzySearch.ratio(index.keyword, keyword) > 70 ||
                    (PinyinSearchUtils.containsChinese(index.keyword) && 
                     (PinyinSearchUtils.toPinyin(index.keyword).contains(keyword) ||
                      PinyinSearchUtils.toPinyinInitials(index.keyword).contains(keyword))) ||
                    (PinyinSearchUtils.containsJapanese(index.keyword) && 
                     (PinyinSearchUtils.toRomaji(index.keyword).contains(keyword) ||
                      PinyinSearchUtils.toRomajiInitials(index.keyword).contains(keyword)))
                }
            } else {
                // 非艺术家字段：按原逻辑处理，需要originalText匹配
                fieldIndices.filter { index ->
                    index.originalText == fieldValue && (
                        index.keyword.contains(keyword) ||
                        FuzzySearch.ratio(index.keyword, keyword) > 70 ||
                        (PinyinSearchUtils.containsChinese(index.keyword) && 
                         (PinyinSearchUtils.toPinyin(index.keyword).contains(keyword) ||
                          PinyinSearchUtils.toPinyinInitials(index.keyword).contains(keyword))) ||
                        (PinyinSearchUtils.containsJapanese(index.keyword) && 
                         (PinyinSearchUtils.toRomaji(index.keyword).contains(keyword) ||
                          PinyinSearchUtils.toRomajiInitials(index.keyword).contains(keyword)))
                    )
                }
            }

            if (matchingIndices.isNotEmpty()) {
                matchedKeywordCount++
                
                // 计算TF（词频）
                val tf = matchingIndices.size.toFloat() / fieldIndices.size

                // 计算IDF（逆文档频率）
                val docFreq = documentFrequencyCache[keyword] ?: 1
                val idf = if (totalDocumentsCache > 0) {
                    ln((totalDocumentsCache + 1).toFloat() / (docFreq + 1))
                } else {
                    1f
                }

                // 计算权重平均值
                val avgWeight = matchingIndices.map { it.weight }.average().toFloat()

                // 贝叶斯公式：P(关键词|歌曲) = TF * IDF * 权重 * 字段权重
                val likelihood = tf * idf * avgWeight * fieldWeight

                // 先验概率
                val prior = keywordPriorProb[keyword] ?: 0.001f

                // 后验概率近似计算
                val posterior = likelihood * prior
                fieldScore += posterior
            }
        }

        // 如果没有任何匹配的关键词，返回0
        if (matchedKeywordCount == 0) return 0f

        return fieldScore
    }

    /**
     * 分割艺术家字符串（支持多种分隔符）
     */
    private fun splitArtists(artistString: String): List<String> {
        if (artistString.isBlank()) return emptyList()
        
        // 支持多种分隔符：、,&,/,;，；/等
        val separators = setOf('、', ',', '&', '/', ';', '，', '；', '|', '·')
        var result = artistString.split(*separators.toCharArray()).map { it.trim() }.filter { it.isNotEmpty() }
        
        // 如果没有找到分隔符，尝试按空格分割（仅当空格不是正常词语的一部分时）
        if (result.size == 1 && artistString.contains(" ")) {
            val spaceParts = artistString.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
            // 只有当分割后的部分都比较短时才认为是多艺术家
            if (spaceParts.all { it.length <= 10 }) {
                result = spaceParts
            }
        }
        
        return result.distinct()
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