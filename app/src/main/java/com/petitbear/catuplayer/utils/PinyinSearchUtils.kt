package com.petitbear.catuplayer.utils

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination
import java.util.regex.Pattern

/**
 * 拼音搜索工具类 (基于Pinyin4j)
 * 为核心搜索功能提供拼音转换支持
 */
object PinyinSearchUtils {

    // 日文字符范围
    private val HIRAGANA_RANGE = '\u3040'..'\u309F'
    private val KATAKANA_RANGE = '\u30A0'..'\u30FF'
    
    // 基本日文假名到罗马音映射
    private val basicRomajiMap = mapOf(
        // 平假名
        "あ" to "a", "い" to "i", "う" to "u", "え" to "e", "お" to "o",
        "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
        "が" to "ga", "ぎ" to "gi", "ぐ" to "gu", "げ" to "ge", "ご" to "go",
        "さ" to "sa", "し" to "shi", "す" to "su", "せ" to "se", "そ" to "so",
        "ざ" to "za", "じ" to "ji", "ず" to "zu", "ぜ" to "ze", "ぞ" to "zo",
        "た" to "ta", "ち" to "chi", "つ" to "tsu", "て" to "te", "と" to "to",
        "だ" to "da", "ぢ" to "ji", "づ" to "zu", "で" to "de", "ど" to "do",
        "な" to "na", "に" to "ni", "ぬ" to "nu", "ね" to "ne", "の" to "no",
        "は" to "ha", "ひ" to "hi", "ふ" to "fu", "へ" to "he", "ほ" to "ho",
        "ば" to "ba", "び" to "bi", "ぶ" to "bu", "べ" to "be", "ぼ" to "bo",
        "ぱ" to "pa", "ぴ" to "pi", "ぷ" to "pu", "ぺ" to "pe", "ぽ" to "po",
        "ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",
        "や" to "ya", "ゆ" to "yu", "よ" to "yo",
        "ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",
        "わ" to "wa", "ゐ" to "wi", "ゑ" to "we", "を" to "wo", "ん" to "n",
        "っ" to "tsu", "ー" to "-",
        
        // 片假名
        "ア" to "a", "イ" to "i", "ウ" to "u", "エ" to "e", "オ" to "o",
        "カ" to "ka", "キ" to "ki", "ク" to "ku", "ケ" to "ke", "コ" to "ko",
        "ガ" to "ga", "ギ" to "gi", "グ" to "gu", "ゲ" to "ge", "ゴ" to "go",
        "サ" to "sa", "シ" to "shi", "ス" to "su", "セ" to "se", "ソ" to "so",
        "ザ" to "za", "ジ" to "ji", "ズ" to "zu", "ゼ" to "ze", "ゾ" to "zo",
        "タ" to "ta", "チ" to "chi", "ツ" to "tsu", "テ" to "te", "ト" to "to",
        "ダ" to "da", "ヂ" to "ji", "ヅ" to "zu", "デ" to "de", "ド" to "do",
        "ナ" to "na", "ニ" to "ni", "ヌ" to "nu", "ネ" to "ne", "ノ" to "no",
        "ハ" to "ha", "ヒ" to "hi", "フ" to "fu", "ヘ" to "he", "ホ" to "ho",
        "バ" to "ba", "ビ" to "bi", "ブ" to "bu", "ベ" to "be", "ボ" to "bo",
        "パ" to "pa", "ピ" to "pi", "プ" to "pu", "ペ" to "pe", "ポ" to "po",
        "マ" to "ma", "ミ" to "mi", "ム" to "mu", "メ" to "me", "モ" to "mo",
        "ヤ" to "ya", "ユ" to "yu", "ヨ" to "yo",
        "ラ" to "ra", "リ" to "ri", "ル" to "ru", "レ" to "re", "ロ" to "ro",
        "ワ" to "wa", "ヰ" to "wi", "ヱ" to "we", "ヲ" to "wo", "ン" to "n",
        "ッ" to "tsu", "ー" to "-"
    )

    // 拼音输出格式配置
    private val outputFormat: HanyuPinyinOutputFormat by lazy {
        HanyuPinyinOutputFormat().apply {
            caseType = HanyuPinyinCaseType.LOWERCASE
            toneType = HanyuPinyinToneType.WITHOUT_TONE
            vCharType = HanyuPinyinVCharType.WITH_V
        }
    }

    /**
     * 将单个中文字符转换为拼音
     */
    private fun charToPinyin(c: Char): String? {
        return try {
            val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, outputFormat)
            pinyinArray?.getOrNull(0)
        } catch (e: BadHanyuPinyinOutputFormatCombination) {
            null
        }
    }

    /**
     * 将文本转换为拼音（无空格）
     */
    fun toPinyin(text: String): String {
        val builder = StringBuilder()
        text.forEach { char ->
            if (isChinese(char)) {
                charToPinyin(char)?.let { builder.append(it) }
            } else {
                builder.append(char.lowercaseChar())
            }
        }
        return builder.toString()
    }

    /**
     * 获取拼音首字母
     */
    fun toPinyinInitials(text: String): String {
        val builder = StringBuilder()
        text.forEach { char ->
            if (isChinese(char)) {
                charToPinyin(char)?.getOrNull(0)?.let { builder.append(it) }
            } else {
                builder.append(char.lowercaseChar())
            }
        }
        return builder.toString()
    }

    /**
     * 判断是否为中文字符
     */
    fun isChinese(c: Char): Boolean {
        val regex = Regex("[\u4E00-\u9FA5]")
        return regex.matches(c.toString())
    }

    /**
     * 判断是否包含中文
     */
    fun containsChinese(text: String): Boolean {
        return text.any { isChinese(it) }
    }

    /**
     * 判断是否为日文字符
     */
    fun isJapanese(c: Char): Boolean {
        return c in HIRAGANA_RANGE || c in KATAKANA_RANGE
    }

    /**
     * 判断是否包含日文
     */
    fun containsJapanese(text: String): Boolean {
        return text.any { isJapanese(it) }
    }

    /**
     * 将日文转换为罗马音
     */
    fun toRomaji(text: String): String {
        val builder = StringBuilder()
        
        text.forEach { char ->
            when {
                isJapanese(char) -> {
                    // 查找映射表
                    basicRomajiMap[char.toString()]?.let { romaji ->
                        builder.append(romaji)
                    } ?: builder.append(char.lowercaseChar())
                }
                else -> {
                    builder.append(char.lowercaseChar())
                }
            }
        }
        
        return builder.toString()
    }

    /**
     * 获取日文罗马音首字母
     */
    fun toRomajiInitials(text: String): String {
        val builder = StringBuilder()
        
        text.forEach { char ->
            when {
                isJapanese(char) -> {
                    basicRomajiMap[char.toString()]?.let { romaji ->
                        builder.append(romaji.firstOrNull() ?: char.lowercaseChar())
                    } ?: builder.append(char.lowercaseChar())
                }
                else -> {
                    builder.append(char.lowercaseChar())
                }
            }
        }
        
        return builder.toString()
    }

    /**
     * 生成搜索关键词集合
     */
    fun generateSearchKeywords(text: String): Set<String> {
        val keywords = mutableSetOf<String>()
        val cleanText = text.trim()

        if (cleanText.isEmpty()) return keywords

        // 1. 原始文本
        keywords.add(cleanText.lowercase())

        // 2. 中文拼音转换
        if (containsChinese(cleanText)) {
            val pinyin = toPinyin(cleanText)
            if (pinyin.isNotEmpty() && pinyin != cleanText.lowercase()) {
                keywords.add(pinyin)
            }

            val initials = toPinyinInitials(cleanText)
            if (initials.isNotEmpty() && initials != cleanText.lowercase()) {
                keywords.add(initials)
            }
        }

        // 3. 日文罗马音转换
        if (containsJapanese(cleanText)) {
            val romaji = toRomaji(cleanText)
            if (romaji.isNotEmpty() && romaji != cleanText.lowercase()) {
                keywords.add(romaji)
            }

            val romajiInitials = toRomajiInitials(cleanText)
            if (romajiInitials.isNotEmpty() && romajiInitials != cleanText.lowercase()) {
                keywords.add(romajiInitials)
            }
        }

        // 4. 分词组合
        val separators = listOf(" ", "-", "·", "——", "《", "》")
        var tempText = cleanText
        separators.forEach { sep ->
            tempText = tempText.replace(sep, " ")
        }

        tempText.split(" ").filter { it.length >= 2 }.forEach { part ->
            keywords.add(part.lowercase())
            
            if (containsChinese(part)) {
                keywords.add(toPinyin(part))
                keywords.add(toPinyinInitials(part))
            }
            
            if (containsJapanese(part)) {
                keywords.add(toRomaji(part))
                keywords.add(toRomajiInitials(part))
            }
        }

        return keywords
    }

    /**
     * 获取所有搜索形式
     */
    fun getAllSearchForms(text: String): List<String> {
        return generateSearchKeywords(text).toList()
    }
}