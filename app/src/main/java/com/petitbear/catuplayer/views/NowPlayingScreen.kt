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
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.petitbear.catuplayer.utils.CoverImageLoader
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.text.TextStyle
import java.io.File

// NowPlayingScreen.kt
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(navController: NavController, viewModel: AudioPlayerViewModel) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isSeeking by viewModel.isSeeking.collectAsState()
    val isCoverLoading by viewModel.isCoverLoading.collectAsState()
    val context = LocalContext.current

    // 添加本地状态来管理进度条
    var sliderProgress by remember { mutableStateOf(0f) }
    var isSliderDragging by remember { mutableStateOf(false) }
    var displayPosition by remember { mutableStateOf(0L) } // 用于显示的时间位置

    // 当不是拖动状态且不是在跳转时，同步进度条位置
    LaunchedEffect(progress, isSliderDragging, isSeeking) {
        if (!isSliderDragging && !isSeeking) {
            sliderProgress = progress
            displayPosition = currentPosition
        }
    }

    // 同步显示位置
    LaunchedEffect(currentPosition) {
        if (!isSliderDragging && !isSeeking) {
            displayPosition = currentPosition
        }
    }

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
                    .padding(16.dp), // 减少内边距
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp) // 减少间距
            ) {
                if (isLoading && !isSeeking) {
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
                    // 专辑封面区域 - 使用权重确保自适应
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AlbumCoverDisplay(
                            currentSong = currentSong!!,
                            isCoverLoading = isCoverLoading,
                            modifier = Modifier
                                .fillMaxWidth(0.7f) // 占用70%宽度
                                .aspectRatio(1f) // 保持正方形
                        )
                    }

                    // 歌曲信息区域
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 歌曲标题
                        val songTitle = currentSong!!.title
                        val shouldScroll = songTitle.length > 11

                        if (shouldScroll) {
                            Text(
                                text = songTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        initialDelayMillis = 1000
                                    )
                            )
                        } else {
                            Text(
                                text = songTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }

                        // 艺术家名称
                        val artistName = currentSong!!.artist
                        val shouldScrollArtist = artistName.length > 15

                        if (shouldScrollArtist) {
                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        initialDelayMillis = 1500
                                    )
                            )
                        } else {
                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }

                    // 进度条区域
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 时间显示
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = MusicMetadataUtils.formatDuration(displayPosition),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentSong!!.formattedDuration,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 进度条
                        Slider(
                            value = sliderProgress,
                            onValueChange = { newProgress ->
                                isSliderDragging = true
                                sliderProgress = newProgress
                                val newPosition = (currentSong!!.duration * newProgress).toLong()
                                displayPosition = newPosition
                            },
                            onValueChangeFinished = {
                                isSliderDragging = false
                                viewModel.seekTo(sliderProgress)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = currentSong!!.duration > 0
                        )
                    }

                    // 播放控制区域 - 固定高度确保可见
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp), // 设置最小高度
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                },
                                modifier = Modifier.size(48.dp),
                                enabled = viewModel.playlist.collectAsState().value.size > 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "上一首",
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // 播放/暂停
                            FilledTonalButton(
                                onClick = { viewModel.pauseOrResume() },
                                modifier = Modifier.size(72.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                enabled = currentSong!!.canPlay
                            ) {
                                if (isLoading && !isSeeking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "暂停" else "播放",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            // 下一首
                            IconButton(
                                onClick = {
                                    viewModel.playNext(context)
                                },
                                modifier = Modifier.size(48.dp),
                                enabled = viewModel.playlist.collectAsState().value.size > 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "下一首",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                // 错误消息显示 - 如果有错误，会占用额外空间
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "错误",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
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

/**
 * 专辑封面显示组件
 */
@Composable
fun AlbumCoverDisplay(
    currentSong: com.petitbear.catuplayer.models.Song,
    isCoverLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (currentSong.hasCover && currentSong.coverUri.isNotEmpty()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(currentSong.coverUri))
                    .crossfade(true)
                    .build(),
                contentDescription = "专辑封面",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCoverLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp), // 减小加载指示器
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            DefaultAlbumIcon()
                        }
                    }
                },
                error = {
                    DefaultAlbumIcon()
                }
            )
        } else {
            DefaultAlbumIcon()
        }
    }
}

/**
 * 默认专辑图标
 */
@Composable
fun DefaultAlbumIcon() {
    Icon(
        imageVector = Icons.Default.MusicNote,
        contentDescription = "专辑封面",
        modifier = Modifier.size(64.dp), // 减小默认图标
        tint = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

// 格式化时间显示 (毫秒 -> 分:秒)
private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}