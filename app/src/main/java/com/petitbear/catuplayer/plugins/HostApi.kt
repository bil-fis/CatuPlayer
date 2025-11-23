package com.petitbear.catuplayer.plugins

import android.content.Context
import android.util.Log
import android.widget.Toast
import org.graalvm.polyglot.HostAccess
import timber.log.Timber
import java.net.URL

class HostApi(private val context: Context) {

    // 暴露给插件的文件操作 API
    @HostAccess.Export
    fun readFile(path: String): String {
        return try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            throw RuntimeException("文件读取失败: ${e.message}")
        }
    }

    @HostAccess.Export
    fun writeFile(path: String, content: String): Boolean {
        return try {
            context.openFileOutput(path, Context.MODE_PRIVATE).use {
                it.write(content.toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // UI 操作 API
    @HostAccess.Export
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // 网络请求 API
    @HostAccess.Export
    fun httpGet(url: String): String {
        return try {
            URL(url).readText()
        } catch (e: Exception) {
            "请求失败: ${e.message}"
        }
    }

    // 日志记录
    @HostAccess.Export
    fun log(level: String, message: String) {
        Timber.tag("PluginSystem").d("[$level] $message")
    }
}