package com.petitbear.catuplayer.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.petitbear.catuplayer.models.AudioPlayerViewModel
import com.petitbear.catuplayer.models.Screen
import com.petitbear.catuplayer.ui.theme.CatuPlayerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Meme(navController: NavController, viewModel: AudioPlayerViewModel) {
    CatuPlayerTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("我的") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()) // 添加垂直滚动
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 用户头像
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "用户",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        "用户名称",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        "个人中心功能开发中",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 折叠菜单示例
                    ExpandableMenuSection(
                        title = "播放设置",
                        icon = Icons.Default.Settings,
                        content = {
                            Column {
                                ExpandableMenuItem("缓存管理", Icons.Default.DeleteSweep, onClick = {
                                    navController.navigate(Screen.ClearCache.route)
                                })
                            }
                        }
                    )
//
//                    ExpandableMenuSection(
//                        title = "历史记录",
//                        icon = Icons.Default.History,
//                        content = {
//                            Column {
//                                ExpandableMenuItem("播放历史", Icons.Default.History)
//                                ExpandableMenuItem("搜索历史", Icons.Default.History)
//                                ExpandableMenuItem("最近播放", Icons.Default.History)
//                            }
//                        }
//                    )

//                    ExpandableMenuSection(
//                        title = "收藏管理",
//                        icon = Icons.Default.Favorite,
//                        content = {
//                            Column {
//                                ExpandableMenuItem("我的收藏", Icons.Default.Favorite)
//                                ExpandableMenuItem("创建的歌单", Icons.Default.Favorite)
//                                ExpandableMenuItem("收藏的歌单", Icons.Default.Favorite)
//                                ExpandableMenuItem("喜欢的音乐", Icons.Default.Favorite)
//                                ExpandableMenuItem("专辑收藏", Icons.Default.Favorite)
//                            }
//                        }
//                    )

//                    ExpandableMenuSection(
//                        title = "账号设置",
//                        icon = Icons.Default.Person,
//                        content = {
//                            Column {
//                                ExpandableMenuItem("个人信息", Icons.Default.Person)
//                                ExpandableMenuItem("账号安全", Icons.Default.Person)
//                                ExpandableMenuItem("会员信息", Icons.Default.Person)
//                                ExpandableMenuItem("消息通知", Icons.Default.Person)
//                            }
//                        }
//                    )

                    ExpandableMenuSection(
                        title = "关于",
                        icon = Icons.Default.Info,
                        content = {
                            Column {
                                ExpandableMenuItem("版本信息", Icons.Default.Info)
                                ExpandableMenuItem("反馈建议", Icons.Default.Info)
                                ExpandableMenuItem("检查更新", Icons.Default.Info)
                            }
                        }
                    )

                    // 添加一些底部间距
                    Spacer(modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
fun ExpandableMenuSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 标题行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 可折叠内容
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 52.dp, // 与图标对齐
                        end = 16.dp,
                        bottom = 16.dp
                    )
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ExpandableMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}