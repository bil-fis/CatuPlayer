package com.petitbear.catuplayer.views

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.petitbear.catuplayer.models.AudioPlayerViewModel
import com.petitbear.catuplayer.models.Screen
import com.petitbear.catuplayer.utils.PlaylistFileManager
import com.petitbear.catuplayer.utils.UriPermissionRestorer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(navController: NavController, viewModel: AudioPlayerViewModel) {
    val playlist by viewModel.playlist.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 播放列表管理器
    val playlistManager = PlaylistFileManager(context)
    // 权限恢复
    val permissionRestorer = UriPermissionRestorer(context)

    var showFilePickerDialog by remember { mutableStateOf(false) }

    // 在启动时加载播放列表并恢复权限
    LaunchedEffect(Unit) {
        playlistManager.loadPlaylist().fold(
            onSuccess = { songs ->
                Log.d("MusicPlayer", "播放列表加载成功: ${songs.size} 首歌曲")

                // 恢复所有歌曲的URI权限
                val restoredCount = permissionRestorer.restoreUriPermissions(
                    songs.map { it.uri }
                )
                Log.d("MusicPlayer", "成功恢复 $restoredCount 个URI的权限")

                viewModel.setPlayList(songs)
            },
            onFailure = { error ->
                Log.e("MusicPlayer", "播放列表加载失败: ${error.message}")
            }
        )
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
                title = { Text("播放列表") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showFilePickerDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加音乐")
            }
        }
    ) { padding ->
        // 文件选择对话框
        if (showFilePickerDialog) {
            FilePickerDialog(
                onDismiss = { showFilePickerDialog = false },
                onFilesSelected = { files ->
                    coroutineScope.launch {
                        viewModel.addSongsToPlaylist(context, files)
                        playlistManager.savePlaylist(viewModel.playlist.value)
                    }
                }
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("正在处理音乐文件...")
                }
            }
        } else if (playlist.isEmpty()) {
            // 空状态
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.MusicOff,
                    contentDescription = "空播放列表",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "播放列表为空",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "点击右下角 + 按钮添加音乐",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(playlist) { song ->
                    val isCurrentSong = currentSong?.id == song.id

                    Card(
                        onClick = {
                            if (song.canPlay) {
                                viewModel.playSong(song)
                                navController.navigate(Screen.NowPlaying.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                // 显示无法播放的提示
                                // 在实际应用中可以使用Snackbar
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentSong) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 歌曲序号
                            Text(
                                text = "${playlist.indexOf(song) + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(32.dp)
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isCurrentSong) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (song.hasMetadata) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "有元数据",
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // 显示歌曲时长
                            Text(
                                text = song.formattedDuration,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            if (isCurrentSong) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "正在播放",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else if (!song.canPlay) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "无法播放",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}