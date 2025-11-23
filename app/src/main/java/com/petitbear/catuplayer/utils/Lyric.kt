package com.petitbear.catuplayer.utils

// LrcLyric.kt
data class LrcLyric(
    val time: Long, // 时间戳（毫秒）
    val text: String // 歌词文本
)

// LrcParser.kt
object LrcParser {

    /**
     * 解析LRC歌词文件
     * 格式：[00:00.00] 歌词文本
     */
    fun parseLrc(lrcContent: String): List<LrcLyric> {
        val lyrics = mutableListOf<LrcLyric>()

        lrcContent.split("\n").forEach { line ->
            try {
                // 匹配时间标签 [00:00.00]
                val timeRegex = Regex("""\[(\d+):(\d+)\.(\d+)\]""")
                val matches = timeRegex.findAll(line)

                matches.forEach { matchResult ->
                    val (minutes, seconds, milliseconds) = matchResult.destructured
                    val time = minutes.toLong() * 60000 +
                            seconds.toLong() * 1000 +
                            milliseconds.toLong() * 10

                    // 提取歌词文本（移除时间标签）
                    val text = line.substringAfterLast(']').trim()
                    if (text.isNotEmpty()) {
                        lyrics.add(LrcLyric(time, text))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 按时间排序
        return lyrics.sortedBy { it.time }
    }

    /**
     * 根据当前播放位置获取当前歌词和下一句歌词
     */
    fun getCurrentLyric(lyrics: List<LrcLyric>, currentPosition: Long): Pair<LrcLyric?, LrcLyric?> {
        if (lyrics.isEmpty()) return null to null

        var current: LrcLyric? = null
        var next: LrcLyric? = null

        for (i in lyrics.indices) {
            if (currentPosition < lyrics[i].time) {
                next = lyrics[i]
                if (i > 0) {
                    current = lyrics[i - 1]
                }
                break
            }
        }

        // 如果已经到了最后一句
        if (current == null && next == null && lyrics.isNotEmpty()) {
            current = lyrics.last()
        }

        return current to next
    }
}