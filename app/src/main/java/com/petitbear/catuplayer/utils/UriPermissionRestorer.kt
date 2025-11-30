package com.petitbear.catuplayer.utils

import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UriPermissionRestorer(private val context: Context) {

    /**
     * 恢复单个 URI 的持久化权限
     * @param uriString 存储的 URI 字符串
     * @return 恢复是否成功
     */
    fun restoreUriPermission(uriString: String): Boolean {
        return try {
            val uri = uriString.toUri()
            restoreUriPermission(uri)
        } catch (e: Exception) {
            Log.e("UriPermission", "恢复 URI 权限失败: $uriString, ${e.message}")
            false
        }
    }

    /**
     * 恢复单个 URI 的持久化权限
     * @param uri 要恢复权限的 URI
     * @return 恢复是否成功
     */
    fun restoreUriPermission(uri: Uri): Boolean {
        return try {
            // 尝试获取读写权限
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            context.contentResolver.takePersistableUriPermission(uri, flags)

            // 验证权限是否恢复成功
            val persistedUriPermissions = context.contentResolver.persistedUriPermissions
            val hasPermission = persistedUriPermissions.any { it.uri == uri }

            if (!hasPermission) {
                Log.w("UriPermission", "URI 权限恢复失败: $uri")
            }

            hasPermission
        } catch (e: SecurityException) {
            Log.e("UriPermission", "安全异常，无法恢复权限: $uri, ${e.message}")
            false
        } catch (e: Exception) {
            Log.e("UriPermission", "恢复权限异常: $uri, ${e.message}")
            false
        }
    }

    /**
     * 批量恢复 URI 权限
     * @param uriStrings URI 字符串列表
     * @return 恢复成功的 URI 数量
     */
    fun restoreUriPermissions(uriStrings: List<String>): Int {
        var successCount = 0

        uriStrings.forEach { uriString ->
            if (restoreUriPermission(uriString)) {
                successCount++
            }
        }

        Log.i("UriPermission", "权限恢复完成: $successCount/${uriStrings.size} 个 URI 恢复成功")
        return successCount
    }

    /**
     * 检查 URI 是否仍然有访问权限
     * @param uriString 要检查的 URI 字符串
     * @return 是否有访问权限
     */
    fun hasUriPermission(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            hasUriPermission(uri)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查 URI 是否仍然有访问权限
     * @param uri 要检查的 URI
     * @return 是否有访问权限
     */
    fun hasUriPermission(uri: Uri): Boolean {
        return try {
            // 方法1: 检查持久化权限列表
            val persistedPermissions = context.contentResolver.persistedUriPermissions
            val hasPersistedPermission = persistedPermissions.any { it.uri == uri }

            if (hasPersistedPermission) {
                return true
            }

            // 方法2: 尝试实际访问文件
            context.contentResolver.openInputStream(uri)?.use {
                // 如果能成功打开输入流，说明有权限
                return true
            }

            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取当前应用所有的持久化 URI 权限
     * @return 持久化权限列表
     */
    fun getAllPersistedUriPermissions(): List<UriPermission> {
        return context.contentResolver.persistedUriPermissions.toList()
    }

    /**
     * 恢复权限并验证可访问性
     * @param uriStrings URI 字符串列表
     * @return 可访问的 URI 列表
     */
    suspend fun restoreAndValidateUris(uriStrings: List<String>): List<String> =
        withContext(Dispatchers.IO) {
            val accessibleUris = mutableListOf<String>()

            uriStrings.forEach { uriString ->
                // 先恢复权限
                if (restoreUriPermission(uriString)) {
                    // 再验证是否真正可访问
                    if (verifyUriAccessibility(uriString)) {
                        accessibleUris.add(uriString)
                    }
                }
            }

            accessibleUris
        }

    /**
     * 验证 URI 是否真正可访问
     * @param uriString URI 字符串
     * @return 是否可访问
     */
    private fun verifyUriAccessibility(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)

            // 尝试查询文件信息
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                // 如果能够成功查询，说明文件可访问
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 安全地恢复 URI 权限，处理各种异常情况
     */
    fun safeRestoreUriPermission(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)

            // 首先检查是否已经有权限
            if (hasUriPermission(uri)) {
                return true
            }

            // 尝试获取持久化权限
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            context.contentResolver.takePersistableUriPermission(uri, flags)

            // 验证权限是否恢复成功
            val hasPermission = hasUriPermission(uri)

            if (hasPermission) {
                Log.d("UriPermission", "成功恢复 URI 权限: $uriString")
            } else {
                Log.w("UriPermission", "URI 权限恢复失败: $uriString")
            }

            hasPermission
        } catch (e: SecurityException) {
            Log.e("UriPermission", "安全异常，无法恢复权限: $uriString, ${e.message}")
            false
        } catch (e: IllegalArgumentException) {
            Log.e("UriPermission", "URI 格式错误: $uriString, ${e.message}")
            false
        } catch (e: Exception) {
            Log.e("UriPermission", "恢复权限时未知异常: $uriString, ${e.message}")
            false
        }
    }

    // 更新批量恢复方法
    fun safeRestoreUriPermissions(uriStrings: List<String>): Int {
        var successCount = 0

        uriStrings.forEach { uriString ->
            if (safeRestoreUriPermission(uriString)) {
                successCount++
            }
        }

        Log.i("UriPermission", "权限恢复完成: $successCount/${uriStrings.size} 个 URI 恢复成功")
        return successCount
    }
}

// 为 List<String> 添加扩展函数，方便批量恢复
fun List<String>.restoreUriPermissions(context: Context): Int {
    val restorer = UriPermissionRestorer(context)
    return restorer.restoreUriPermissions(this)
}

// 为 String 添加扩展函数，方便单个恢复
fun String.restoreUriPermission(context: Context): Boolean {
    val restorer = UriPermissionRestorer(context)
    return restorer.restoreUriPermission(this)
}

// 检查权限的扩展函数
fun String.hasUriPermission(context: Context): Boolean {
    val restorer = UriPermissionRestorer(context)
    return restorer.hasUriPermission(this)
}