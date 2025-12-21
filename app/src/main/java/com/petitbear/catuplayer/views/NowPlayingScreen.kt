package com.petitbear.catuplayer.views

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.petitbear.catuplayer.models.AudioPlayerViewModel
import com.petitbear.catuplayer.models.Screen
import com.petitbear.catuplayer.utils.LrcLyric
import com.petitbear.catuplayer.utils.MusicMetadataUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val playMode by viewModel.playMode.collectAsState()

    // 歌词相关状态
    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 添加本地状态来管理进度条
    var sliderProgress by remember { mutableStateOf(0f) }
    var isSliderDragging by remember { mutableStateOf(false) }
    var displayPosition by remember { mutableStateOf(0L) } // 用于显示的时间位置

    // 底部抽屉状态
    val bottomSheetState = rememberModalBottomSheetState()
    var showLyricBottomSheet by remember { mutableStateOf(false) }

    // 三点菜单状态
    var showMenu by remember { mutableStateOf(false) }

    // 文件选择器启动器
    val lrcFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                handleLrcFileSelected(context, it, currentSong, viewModel, coroutineScope)
            }
        }
    )

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

    // 使用ModalBottomSheet作为根布局
    if (showLyricBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showLyricBottomSheet = false
            },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 8.dp,
            dragHandle = {
                // 自定义拖拽手柄
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        ) {
            // 底部抽屉内容 - 歌词显示，传入ViewModel以持续监听状态
            LyricBottomSheetContent(
                viewModel = viewModel,
                onClose = {
                    showLyricBottomSheet = false
                    coroutineScope.launch {
                        bottomSheetState.hide()
                    }
                }
            )
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
                actions = {
                    // 三点菜单按钮
                    if (currentSong != null) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "更多选项",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 下拉菜单
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.width(160.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("选择本地歌词") },
                                    onClick = {
                                        showMenu = false
                                        // 启动文件选择器，限制为文本文件
                                        lrcFilePickerLauncher.launch("text/*")
                                    }
                                )
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("重新下载歌词") },
                                    onClick = {
                                        showMenu = false
                                        currentSong?.let { song ->
                                            coroutineScope.launch {
                                                // 清除现有歌词缓存
                                                val lrcCacheFile = File(
                                                    context.getExternalFilesDir(null),
                                                    "lyrics/${song.id}_${song.title}.lrc"
                                                )
                                                if (lrcCacheFile.exists()) {
                                                    lrcCacheFile.delete()
                                                }
                                                // 重新下载歌词
                                                viewModel.loadLyricsForSong(song)
                                            }
                                        }
                                    }
                                )
                            }
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
                                .clickable(
                                    enabled = currentLyrics.isNotEmpty(),
                                    onClick = {
                                        // 点击专辑封面打开歌词抽屉
                                        if (currentLyrics.isNotEmpty()) {
                                            showLyricBottomSheet = true
                                            coroutineScope.launch {
                                                bottomSheetState.expand()
                                            }
                                        }
                                    }
                                ),
                            onClick = {
                                // 点击专辑封面打开歌词抽屉
                                if (currentLyrics.isNotEmpty()) {
                                    showLyricBottomSheet = true
                                    coroutineScope.launch {
                                        bottomSheetState.expand()
                                    }
                                }
                            }
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

                        // 歌词指示器（如果当前有歌词）
                        if (currentLyrics.isNotEmpty() && currentLyricIndex >= 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.clickable(
                                    enabled = currentLyrics.isNotEmpty(),
                                    onClick = {
                                        showLyricBottomSheet = true
                                        coroutineScope.launch {
                                            bottomSheetState.expand()
                                        }
                                    }
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "查看歌词",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = currentLyrics[currentLyricIndex].text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
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
                            // 播放模式切换按钮
                            IconButton(
                                onClick = {
                                    viewModel.togglePlayMode()
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                val modeIcon = when (playMode) {
                                    com.petitbear.catuplayer.models.PlayMode.SEQUENTIAL -> Icons.Default.Repeat
                                    com.petitbear.catuplayer.models.PlayMode.SINGLE_LOOP -> Icons.Default.RepeatOne
                                    com.petitbear.catuplayer.models.PlayMode.RANDOM -> Icons.Default.Shuffle
                                }
                                Icon(
                                    imageVector = modeIcon,
                                    contentDescription = "播放模式",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

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

                            // 红心收藏按钮
                            IconButton(
                                onClick = {
                                    currentSong?.let { song ->
                                        coroutineScope.launch {
                                            viewModel.toggleFavorite(song.id, !song.isFavorite)
                                        }
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (currentSong?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (currentSong?.isFavorite == true) "取消收藏" else "收藏",
                                    modifier = Modifier.size(24.dp),
                                    tint = if (currentSong?.isFavorite == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
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
 * 处理用户选择的LRC歌词文件
 */
private fun handleLrcFileSelected(
    context: android.content.Context,
    uri: android.net.Uri,
    currentSong: com.petitbear.catuplayer.models.Song?,
    viewModel: AudioPlayerViewModel,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    if (currentSong == null) return

    coroutineScope.launch {
        try {
            // 读取选择的LRC文件内容
            val inputStream = context.contentResolver.openInputStream(uri)
            val lrcContent = inputStream?.bufferedReader().use { it?.readText() } ?: ""

            if (lrcContent.isNotEmpty()) {
                // 创建lyrics目录（如果不存在）
                val lyricsDir = File(context.getExternalFilesDir(null), "lyrics")
                if (!lyricsDir.exists()) {
                    lyricsDir.mkdirs()
                }

                // 清理文件名中的非法字符
                val safeTitle = currentSong.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")

                // 保存文件：歌曲id_歌曲标题.lrc
                val lrcFile = File(lyricsDir, "${currentSong.id}_${safeTitle}.lrc")
                lrcFile.writeText(lrcContent)

                // 重新加载歌词
                viewModel.loadLyricsForSong(currentSong)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * 歌词底部抽屉内容 - 修改为直接接收ViewModel以持续监听状态
 */
@Composable
fun LyricBottomSheetContent(
    viewModel: AudioPlayerViewModel,
    onClose: () -> Unit
) {
    // 持续监听歌词相关状态
    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp, max = 500.dp)
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "歌词",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandLess,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 歌词显示区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp)
        ) {
            if (currentLyrics.isEmpty()) {
                // 没有歌词时的状态
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = "无歌词",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无歌词",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "首次播放时会自动下载歌词",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                // 有歌词时的滚动列表
                ScrollableLyricList(
                    lyrics = currentLyrics,
                    currentLyricIndex = currentLyricIndex,
                    currentPosition = currentPosition
                )
            }
        }

        // 底部信息栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 歌词统计信息
            Text(
                text = "共 ${currentLyrics.size} 行歌词",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 当前时间
            Text(
                text = MusicMetadataUtils.formatDuration(currentPosition),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 可滚动的歌词列表 - 添加持续监听当前歌词索引
 */
@Composable
fun ScrollableLyricList(
    lyrics: List<LrcLyric>,
    currentLyricIndex: Int,
    currentPosition: Long
) {
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 持续监听当前歌词索引变化，自动滚动到该位置
    LaunchedEffect(currentLyricIndex, lyrics) {
        if (currentLyricIndex >= 0 && currentLyricIndex < lyrics.size) {
            // 延迟一点确保布局已经完成
            delay(50)

            // 计算要滚动到的位置，使当前歌词在中间
            coroutineScope.launch {
                try {
                    // 使用平滑滚动效果
                    scrollState.animateScrollToItem(
                        index = currentLyricIndex,
                        scrollOffset = -150, // 负值表示向上偏移，使歌词在中间位置
                    )
                } catch (e: Exception) {
                    // 如果动画失败，使用普通滚动
                    scrollState.scrollToItem(
                        index = currentLyricIndex,
                        scrollOffset = -150
                    )
                }
            }
        }
    }

    // 监听歌词列表变化，初始化滚动位置
    LaunchedEffect(lyrics) {
        if (lyrics.isNotEmpty() && currentLyricIndex >= 0 && currentLyricIndex < lyrics.size) {
            delay(100)
            coroutineScope.launch {
                scrollState.scrollToItem(
                    index = currentLyricIndex,
                    scrollOffset = -150
                )
            }
        }
    }

    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp) // 增加行间距
    ) {
        itemsIndexed(lyrics) { index, lyric ->
            val isCurrent = index == currentLyricIndex
            val isPast = index < currentLyricIndex
            val isFuture = index > currentLyricIndex

            LyricItem(
                text = lyric.text,
                time = lyric.time,
                currentPosition = currentPosition,
                isCurrent = isCurrent,
                isPast = isPast,
                isFuture = isFuture,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .animateContentSize()
            )
        }
    }
}

/**
 * 单个歌词项 - 改进高亮效果
 */
@Composable
fun LyricItem(
    text: String,
    time: Long,
    currentPosition: Long,
    isCurrent: Boolean,
    isPast: Boolean,
    isFuture: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = when {
        isCurrent -> MaterialTheme.colorScheme.primary
        isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    }

    val textStyle = when {
        isCurrent -> MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
        else -> MaterialTheme.typography.bodyLarge.copy(
            fontSize = 18.sp
        )
    }

    val backgroundColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            style = textStyle,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

/**
 * 专辑封面显示组件 - 修改为支持点击
 */
@Composable
fun AlbumCoverDisplay(
    currentSong: com.petitbear.catuplayer.models.Song,
    isCoverLoading: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
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

        // 添加一个半透明的覆盖层来提示可点击
        if (onClick != {} && currentSong.hasLyric) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    )
            )
        }
    }
}

/**
 * 默认专辑图标
 */
@Composable
fun DefaultAlbumIcon() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "专辑封面",
            modifier = Modifier.size(64.dp), // 减小默认图标
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击查看歌词",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

// 格式化时间显示 (毫秒 -> 分:秒)
private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}