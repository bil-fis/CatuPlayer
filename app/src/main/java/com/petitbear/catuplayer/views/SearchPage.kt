package com.petitbear.catuplayer.views

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.petitbear.catuplayer.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(
    navController: NavController,
    viewModel: AudioPlayerViewModel
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var localSearchText by remember { mutableStateOf("") }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    // Tab标题
    val searchTabs = listOf("本地搜索", "网络搜索")

    // 自动请求焦点
    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    // 同步搜索文本
    LaunchedEffect(searchQuery) {
        if (localSearchText != searchQuery) {
            localSearchText = searchQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "搜索：$searchQuery" else "搜索",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索框
            SearchBox(
                searchText = localSearchText,
                onSearchTextChanged = { text ->
                    localSearchText = text
                    // 只更新文本，不执行搜索
                },
                onSearch = {
                    if (localSearchText.isNotEmpty()) {
                        coroutineScope.launch {
                            viewModel.search(localSearchText)
                        }
                        keyboardController?.hide()
                    }
                },
                onClear = {
                    localSearchText = ""
                    viewModel.clearSearch()
                },
                focusRequester = focusRequester,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tab栏（仅在搜索时显示）
            if (searchQuery.isNotEmpty()) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    searchTabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
            }

            // 内容区域
            when {
                // 搜索中
                isSearching -> {
                    LoadingIndicator()
                }

                // 有搜索结果
                searchQuery.isNotEmpty() && searchResults.isNotEmpty() -> {
                    when (selectedTabIndex) {
                        0 -> { // 本地搜索
                            SearchResultsList(
                                searchResults = searchResults,
                                currentSong = currentSong,
                                onSongClick = { song ->
                                    if (song.canPlay) {
                                        viewModel.playSong(song)
                                        navController.navigate(Screen.NowPlaying.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                        1 -> { // 网络搜索
                            NetworkSearchResults(
                                query = searchQuery,
                                onSongClick = { song ->
                                    // TODO: 实现网络搜索结果点击处理
                                }
                            )
                        }
                    }
                }

                // 有搜索词但无结果
                searchQuery.isNotEmpty() -> {
                    when (selectedTabIndex) {
                        0 -> { // 本地搜索
                            EmptySearchResult(
                                query = searchQuery,
                                onRetry = {
                                    coroutineScope.launch {
                                        viewModel.search(searchQuery)
                                    }
                                }
                            )
                        }
                        1 -> { // 网络搜索
                            NetworkSearchResults(
                                query = searchQuery,
                                onSongClick = { song ->
                                    // TODO: 实现网络搜索结果点击处理
                                }
                            )
                        }
                    }
                }

                // 显示历史记录
                else -> {
                    SearchHistorySection(
                        searchHistory = searchHistory,
                        onHistoryClick = { query ->
                            localSearchText = query
                            coroutineScope.launch {
                                viewModel.search(query)
                            }
                        },
                        onDeleteHistory = { historyId ->
                            coroutineScope.launch {
                                viewModel.deleteSearchHistory(historyId)
                            }
                        },
                        onClearAllHistory = {
                            showClearHistoryDialog = true
                        },
                        onTogglePin = { historyId, pinned ->
                            coroutineScope.launch {
                                viewModel.togglePinSearchHistory(historyId, pinned)
                            }
                        }
                    )
                }
            }
        }
    }

    // 清除历史对话框
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("清除搜索历史") },
            text = { Text("确定要清除所有搜索历史吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.clearAllSearchHistory()
                            showClearHistoryDialog = false
                        }
                    }
                ) {
                    Text("清除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBox(
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            TextField(
                value = searchText,
                onValueChange = onSearchTextChanged,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text("搜索歌曲、歌手、专辑...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch() }
                ),
                singleLine = true
            )

            if (searchText.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "清除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultsList(
    searchResults: List<SearchResult>,
    currentSong: Song?,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 结果统计
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "搜索结果 (${searchResults.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 搜索结果列表
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(searchResults) { result ->
                SearchResultCard(
                    result = result,
                    isCurrentSong = result.song.id == currentSong?.id,
                    onClick = { onSongClick(result.song) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SearchResultCard(
    result: SearchResult,
    isCurrentSong: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
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
            // 匹配分数指示器
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            result.score >= 60 -> MaterialTheme.colorScheme.primary
                            result.score >= 40 -> MaterialTheme.colorScheme.secondary
                            result.score >= 20 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${result.score.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = result.song.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrentSong) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = result.song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // 匹配类型标签
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)),
                        color = when (result.matchType) {
                            SearchResult.MATCH_TITLE -> MaterialTheme.colorScheme.primary
                            SearchResult.MATCH_ARTIST -> MaterialTheme.colorScheme.secondary
                            SearchResult.MATCH_ALBUM -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = when (result.matchType) {
                                SearchResult.MATCH_TITLE -> "标题"
                                SearchResult.MATCH_ARTIST -> "歌手"
                                SearchResult.MATCH_ALBUM -> "专辑"
                                else -> "综合"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (result.matchType) {
                                SearchResult.MATCH_TITLE -> MaterialTheme.colorScheme.onPrimary
                                SearchResult.MATCH_ARTIST -> MaterialTheme.colorScheme.onSecondary
                                SearchResult.MATCH_ALBUM -> MaterialTheme.colorScheme.onTertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 显示歌曲时长
            Text(
                text = result.song.formattedDuration,
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
            } else if (!result.song.canPlay) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "无法播放",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun SearchHistorySection(
    searchHistory: List<SearchHistory>,
    onHistoryClick: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onClearAllHistory: () -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 搜索历史标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "搜索历史",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (searchHistory.isNotEmpty()) {
                TextButton(onClick = onClearAllHistory) {
                    Text("清除全部")
                }
            }
        }

        // 搜索历史标签云
        if (searchHistory.isEmpty()) {
            EmptyHistoryState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 置顶的历史记录
                val pinnedHistory = searchHistory.filter { it.isPinned }
                if (pinnedHistory.isNotEmpty()) {
                    item {
                        Text(
                            text = "置顶",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    item {
                        SearchHistoryTags(
                            histories = pinnedHistory,
                            onHistoryClick = onHistoryClick,
                            onDeleteHistory = onDeleteHistory,
                            onTogglePin = onTogglePin,
                            isPinnedSection = true
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 普通历史记录
                val normalHistory = searchHistory.filter { !it.isPinned }
                if (normalHistory.isNotEmpty()) {
                    item {
                        SearchHistoryTags(
                            histories = normalHistory,
                            onHistoryClick = onHistoryClick,
                            onDeleteHistory = onDeleteHistory,
                            onTogglePin = onTogglePin,
                            isPinnedSection = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchHistoryItem(
    history: SearchHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 查询词
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (history.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "已置顶",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = history.query,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 统计信息
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 结果数量
                    if (history.resultCount > 0) {
                        Text(
                            text = "${history.resultCount} 个结果",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // 时间
                    Text(
                        text = formatRelativeTime(history.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 操作按钮
            Row {
                // 置顶按钮
                IconButton(
                    onClick = { onTogglePin(!history.isPinned) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (history.isPinned) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = if (history.isPinned) "取消置顶" else "置顶",
                        tint = if (history.isPinned) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 删除按钮
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SearchHistoryTags(
    histories: List<SearchHistory>,
    onHistoryClick: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    isPinnedSection: Boolean,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        histories.forEach { history ->
            SearchHistoryTag(
                history = history,
                onClick = { onHistoryClick(history.query) },
                onDelete = { onDeleteHistory(history.id) },
                onTogglePin = { onTogglePin(history.id, !history.isPinned) }
            )
        }
    }
}

@Composable
fun SearchHistoryTag(
    history: SearchHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (history.isPinned) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "已置顶",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = history.query,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (history.resultCount > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(${history.resultCount})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 更多选项按钮
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多选项",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (history.isPinned) Icons.Default.StarOutline else Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (history.isPinned) "取消置顶" else "置顶")
                            }
                        },
                        onClick = {
                            onTogglePin()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("删除")
                            }
                        },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = "无搜索历史",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "暂无搜索历史",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "开始搜索后，您的搜索记录将显示在这里",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun NetworkSearchResults(
    query: String,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = "网络搜索",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "网络搜索功能暂未开放",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "请使用本地搜索功能",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptySearchResult(
    query: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = "无搜索结果",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "未找到与 '$query' 相关的结果",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "请尝试其他关键词或检查拼写",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("重新搜索")
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("搜索中...")
        }
    }
}

// 辅助函数：格式化相对时间
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "刚刚"
        diff < 3600000 -> "${diff / 60000}分钟前"
        diff < 86400000 -> "${diff / 3600000}小时前"
        diff < 604800000 -> "${diff / 86400000}天前"
        else -> {
            val date = Date(timestamp)
            val format = SimpleDateFormat("MM-dd", Locale.getDefault())
            format.format(date)
        }
    }
}