package com.petitbear.catuplayer.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 缓存管理器
 */
object CacheManager {

    /**
     * 清除所有缓存
     */
    suspend fun clearAllCache(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                // 清除封面缓存
                MusicMetadataUtils.clearCoverCache(context)

                // 清除其他缓存目录
                val cacheDirs = listOf(
                    context.cacheDir,
                    context.externalCacheDir
                )

                cacheDirs.forEach { cacheDir ->
                    cacheDir?.let { dir ->
                        if (dir.exists() && dir.isDirectory) {
                            dir.listFiles()?.forEach { file ->
                                if (file.isDirectory) {
                                    file.deleteRecursively()
                                } else {
                                    file.delete()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 获取缓存总大小
     */
    suspend fun getTotalCacheSize(context: Context): Long {
        return withContext(Dispatchers.IO) {
            try {
                var size = 0L

                // 封面缓存大小
                size += MusicMetadataUtils.getCoverCacheSize(context)

                // 其他缓存目录大小
                val cacheDirs = listOf(
                    context.cacheDir,
                    context.externalCacheDir
                )

                cacheDirs.forEach { cacheDir ->
                    cacheDir?.let { dir ->
                        if (dir.exists() && dir.isDirectory) {
                            dir.listFiles()?.forEach { file ->
                                size += if (file.isDirectory) {
                                    getFolderSize(file)
                                } else {
                                    file.length()
                                }
                            }
                        }
                    }
                }

                size
            } catch (e: Exception) {
                e.printStackTrace()
                0
            }
        }
    }

    /**
     * 获取文件夹大小
     */
    private fun getFolderSize(folder: File): Long {
        var length: Long = 0
        folder.listFiles()?.forEach { file ->
            length += if (file.isFile) {
                file.length()
            } else {
                getFolderSize(file)
            }
        }
        return length
    }

    /**
     * 格式化缓存大小
     */
    fun formatCacheSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }
}