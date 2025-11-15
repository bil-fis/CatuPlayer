package com.petitbear.catuplayer.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FilePickerUtils {

    // 解析文件名，格式：作者 - 曲名
    fun parseMusicFileName(fileName: String): Pair<String, String> {
        return try {
            val cleanName = fileName.substringBeforeLast(".").substringAfterLast("/") // 移除扩展名

            // 尝试用 " - " 分割
            if (cleanName.contains(" - ")) {
                val parts = cleanName.split(" - ", limit = 2)
                if (parts.size == 2) {
                    val artist = parts[0].trim()
                    val title = parts[1].trim()
                    if (artist.isNotEmpty() && title.isNotEmpty()) {
                        return artist to title
                    }
                }
            }

            // 尝试用 "-" 分割
            if (cleanName.contains("-")) {
                val parts = cleanName.split("-", limit = 2)
                if (parts.size == 2) {
                    val artist = parts[0].trim()
                    val title = parts[1].trim()
                    if (artist.isNotEmpty() && title.isNotEmpty()) {
                        return artist to title
                    }
                }
            }

            // 如果无法解析，使用默认值
            "未知艺术家" to cleanName
        } catch (e: Exception) {
            "未知艺术家" to fileName.substringBeforeLast(".")
        }
    }

    // 从 URI 获取文件名
    suspend fun getFileNameFromUri(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                        if (displayNameIndex != -1) {
                            cursor.getString(displayNameIndex)
                        } else {
                            // 如果无法获取显示名称，使用 URI 的最后路径段
                            uri.lastPathSegment ?: "未知文件"
                        }
                    } else {
                        uri.lastPathSegment ?: "未知文件"
                    }
                } ?: (uri.lastPathSegment ?: "未知文件")
            } catch (e: Exception) {
                e.printStackTrace()
                uri.lastPathSegment ?: "未知文件"
            }
        }
    }
}