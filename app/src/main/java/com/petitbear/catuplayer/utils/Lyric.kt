package com.petitbear.catuplayer.utils

// Lyric.kt
data class LrcLyric(
    val time: Long, // 时间戳（毫秒）
    val text: String // 歌词文本
)

object LrcParser {

    /**
     * 解析LRC歌词文件
     * 支持格式：[mm:ss.xx] 或 [mm:ss:xx] (xx为百分秒)
     */
    fun parseLrc(lrcContent: String): List<LrcLyric> {
        val lyrics = mutableListOf<LrcLyric>()

        lrcContent.split("\n").forEach { line ->
            try {
                // 匹配时间标签 [mm:ss.xx] 或 [mm:ss:xx]
                val timeRegex = Regex("""\[(\d{2,}):(\d{2})(?:[:.](\d{2,3}))?\]""")
                val matchResult = timeRegex.find(line)

                if (matchResult != null) {
                    val (minutesStr, secondsStr, millisecondsStr) = matchResult.destructured

                    val minutes = minutesStr.toLongOrNull() ?: 0
                    val seconds = secondsStr.toLongOrNull() ?: 0

                    // 处理毫秒/百分秒部分
                    var milliseconds = 0L
                    if (millisecondsStr.isNotEmpty()) {
                        val msValue = millisecondsStr.toLongOrNull() ?: 0
                        // 如果是2位数（百分秒），转换为毫秒
                        if (millisecondsStr.length == 2) {
                            milliseconds = msValue * 10 // 百分秒转毫秒
                        } else if (millisecondsStr.length == 3) {
                            milliseconds = msValue // 已经是毫秒
                        }
                    }

                    val time = minutes * 60000 + seconds * 1000 + milliseconds

                    // 提取歌词文本（移除所有时间标签）
                    val text = timeRegex.replace(line, "").trim()
                    if (text.isNotEmpty()) {
                        lyrics.add(LrcLyric(time, text))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 按时间排序并去重（防止重复行）
        return lyrics.distinctBy { it.time }.sortedBy { it.time }
    }

    /**
     * 根据当前播放位置获取当前歌词索引
     * @return 当前歌词的索引，如果找不到则返回-1
     */
    fun getCurrentLyricIndex(lyrics: List<LrcLyric>, currentPosition: Long): Int {
        if (lyrics.isEmpty()) return -1

        // 如果当前时间小于第一句歌词的时间，返回-1
        if (currentPosition < lyrics.first().time) {
            return -1
        }

        // 如果当前时间大于等于最后一句歌词的时间，返回最后一句
        if (currentPosition >= lyrics.last().time) {
            return lyrics.lastIndex
        }

        // 找到第一个时间大于当前时间的歌词，返回前一句
        for (i in lyrics.indices) {
            if (currentPosition < lyrics[i].time) {
                return i - 1
            }
        }

        return -1
    }

    /**
     * 根据当前播放位置获取当前歌词
     */
    fun getCurrentLyric(lyrics: List<LrcLyric>, currentPosition: Long): LrcLyric? {
        val index = getCurrentLyricIndex(lyrics, currentPosition)
        return if (index >= 0 && index < lyrics.size) lyrics[index] else null
    }
}