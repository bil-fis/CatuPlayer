package com.petitbear.catuplayer.views.settingsView

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petitbear.catuplayer.utils.CacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ClearCachePage() {
    var cacheSize by remember { mutableStateOf("0kb") }
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    // 获取缓存大小
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            // 在后台线程执行可能耗时的操作
            val size = CacheManager.getTotalCacheSize(context)
            val formattedSize = CacheManager.formatCacheSize(size)

            // 更新UI需要在主线程
            withContext(Dispatchers.Main) {
                cacheSize = formattedSize
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 缓存大小显示
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "缓存大小（歌曲封面）：",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = cacheSize,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 清除数据按钮
        Button(
            onClick = {
                // 这里调用你的清除缓存方法
                // clearCache()
                // 清除后更新显示
                coroutineScope.launch(Dispatchers.IO) {
                    // 在后台线程执行可能耗时的操作
                    CacheManager.clearAllCache(context)
                    val size = CacheManager.getTotalCacheSize(context)
                    val formattedSize = CacheManager.formatCacheSize(size)

                    // 更新UI需要在主线程
                    withContext(Dispatchers.Main) {
                        cacheSize = formattedSize
                    }
                }


                // 可以添加Toast提示
                // Toast.makeText(context, "缓存已清除", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "清除",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "清除数据",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 18.sp
            )
        }

        // 提示信息
        Text(
            text = "清除缓存将删除所有离线播放列表和歌曲封面图片",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth(0.9f)
        )
    }
}