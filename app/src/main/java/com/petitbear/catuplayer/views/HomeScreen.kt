package com.petitbear.catuplayer.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.petitbear.catuplayer.models.AudioPlayerViewModel
import com.petitbear.catuplayer.models.Screen
import com.petitbear.catuplayer.models.Song
import com.petitbear.catuplayer.ui.theme.CatuPlayerTheme
import com.petitbear.catuplayer.utils.LrcLyric

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: AudioPlayerViewModel) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val currentPosition by viewModel.currentPosition.collectAsState()
    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()

    LaunchedEffect(currentPosition, isPlaying) {
        if (isPlaying) {
            viewModel.updateCurrentLyric(currentPosition)
        }
    }

    Scaffold(
        topBar = {
//            CenterAlignedTopAppBar(
//                title = { Text("音乐播放器") }
//            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 当前播放信息卡片
            Card(
                onClick = {
                    navController.navigate(Screen.NowPlaying.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 左侧：歌曲信息
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "当前播放",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currentSong?.title ?: "未播放歌曲",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = currentSong?.artist ?: "请选择一首歌曲开始播放",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 播放状态指示器
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPlaying) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    )
                            )
                            Text(
                                text = if (isPlaying) "播放中" else "已暂停",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPlaying) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // 右侧：歌词显示区域
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(120.dp), // 固定高度确保布局稳定
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (currentSong != null && currentLyrics.isNotEmpty()) {
                            // 显示滚动歌词
                            LyricDisplay(
                                lyrics = currentLyrics,
                                currentIndex = currentLyricIndex,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // 无歌词时的占位显示
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "无歌词",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (currentSong != null) "暂无歌词" else "未播放歌曲",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            // 推荐歌曲区域
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "推荐歌曲",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            // 示例推荐歌曲列表
            val recommendedSongs = remember {
                listOf(
                    Song("4", "经典老歌", "经典艺术家", 240000, ""),
                    Song("5", "热门新歌", "流行歌手", 210000, ""),
                    Song("6", "放松音乐", "自然之声", 180000, "")
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recommendedSongs) { song ->
                    Card(
                        onClick = {
                            // 播放推荐歌曲
                            viewModel.playSong(song)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    song.title,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "播放")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LyricDisplay(
    lyrics: List<LrcLyric>,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()

    // 自动滚动到当前歌词
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            scrollState.animateScrollToItem(
                index = currentIndex,
                scrollOffset = 0
            )
        }
    }

    LazyColumn(
        state = scrollState,
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(lyrics) { index, lyric ->
            val isCurrent = index == currentIndex

            Text(
                text = lyric.text,
                style = if (isCurrent) {
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}