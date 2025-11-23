package com.petitbear.catuplayer.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.petitbear.catuplayer.models.Screen
import com.petitbear.catuplayer.ui.theme.CatuPlayerTheme
import com.petitbear.catuplayer.utils.MusicMetadataUtils
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import com.petitbear.catuplayer.models.AudioPlayerViewModel

// NowPlayingScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(navController: NavController, viewModel: AudioPlayerViewModel) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    // 显示错误消息
    if (errorMessage != null) {
        LaunchedEffect(errorMessage) {
            delay(3000)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("正在播放")
                        if (currentSong != null) {
                            Text(
                                currentSong!!.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        if (currentSong == null) {
            // 没有歌曲播放时的状态
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = "无歌曲",
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text("暂无播放的歌曲")
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        navController.navigate(Screen.Playlist.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                ) {
                    Text("选择歌曲")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // 加载状态
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("正在加载音乐...")
                        }
                    }
                } else {
                    // 专辑封面
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "专辑封面",
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // 歌曲信息
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentSong!!.title,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = currentSong!!.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (currentSong!!.duration > 0) {
                            Text(
                                text = "时长: ${currentSong!!.formattedDuration}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 进度条区域
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 时间显示
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = MusicMetadataUtils.formatDuration(currentPosition),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentSong!!.formattedDuration,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 进度条
                        Slider(
                            value = progress,
                            onValueChange = { viewModel.seekTo(it) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = currentSong!!.duration > 0
                        )
                    }

                    // 播放控制
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 主要控制按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 上一首
                            IconButton(
                                onClick = {
                                    viewModel.playPrevious(context)
                                    // 需要上下文来播放上一首
                                },
                                modifier = Modifier.size(56.dp),
                                enabled = viewModel.playlist.collectAsState().value.size > 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "上一首",
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // 播放/暂停
                            FilledTonalButton(
                                onClick = { viewModel.pauseOrResume() },
                                modifier = Modifier.size(80.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                enabled = currentSong!!.canPlay
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "暂停" else "播放",
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            // 下一首
                            IconButton(
                                onClick = {
                                    viewModel.playNext(context)
                                },
                                modifier = Modifier.size(56.dp),
                                enabled = viewModel.playlist.collectAsState().value.size > 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "下一首",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                // 错误消息显示
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "错误",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
// 格式化时间显示 (毫秒 -> 分:秒)
private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}