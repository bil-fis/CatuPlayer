package com.petitbear.catuplayer.plugins

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess

class PluginManager(
    private val context: Context,
    private val hostApi: HostApi
) {
    private val pluginRegistry = mutableMapOf<String, Plugin>()
    private val jsContext = createJsContext()

    private fun createJsContext(): Context {
        return Context.newBuilder("js")
            .allowHostAccess(HostAccess.ALL)
            .allowHostClassLookup { true }
            .option("js.ecmascript-version", "2022")
            .build()
    }

    // 插件信息数据类
    data class Plugin(
        val id: String,
        val name: String,
        val version: String,
        val author: String,
        val description: String,
        val script: String,
        val permissions: List<String>
    )

    // 加载插件
    fun loadPlugin(plugin: Plugin): Result<Unit> {
        return try {
            // 验证插件权限
            if (!validatePermissions(plugin.permissions)) {
                return Result.failure(SecurityException("权限不足"))
            }

            // 执行插件初始化脚本
            jsContext.eval(
                "js", """
                // 创建插件命名空间
                if (typeof plugins === 'undefined') {
                    var plugins = {};
                }
                plugins['${plugin.id}'] = (function() {
                    ${plugin.script}
                    
                    // 插件必须导出的接口
                    return {
                        name: '${plugin.name}',
                        version: '${plugin.version}',
                        initialize: initialize,
                        execute: execute,
                        cleanup: cleanup
                    };
                })();
            """.trimIndent()
            )

            pluginRegistry[plugin.id] = plugin
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 执行插件功能
    suspend fun executePlugin(pluginId: String, input: Map<String, Any>): Result<Any> {
        return withContext(Dispatchers.IO) {
            try {
                val plugin = pluginRegistry[pluginId]
                    ?: return@withContext Result.failure(IllegalArgumentException("插件不存在"))

                val result = jsContext.eval(
                    "js", """
                    if (plugins['$pluginId'] && plugins['$pluginId'].execute) {
                        plugins['$pluginId'].execute(${Gson().toJson(input)});
                    } else {
                        throw new Error('插件执行函数不存在');
                    }
                """.trimIndent()
                )

                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun validatePermissions(requested: List<String>): Boolean {
        val availablePermissions = listOf("file_read", "file_write", "network", "ui")
        return requested.all { it in availablePermissions }
    }
}

