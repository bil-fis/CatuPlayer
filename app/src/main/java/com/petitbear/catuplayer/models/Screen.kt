package com.petitbear.catuplayer.models

sealed class Screen(val route: String, val title: String, val icon: Int? = null) {
    object Home : Screen("home", "首页", android.R.drawable.ic_menu_view)
    object Playlist : Screen("playlist", "播放列表", android.R.drawable.ic_menu_agenda)
    object NowPlaying : Screen("now_playing", "正在播放", android.R.drawable.ic_media_play)
    object SearchPage : Screen("search", "搜索", android.R.drawable.ic_menu_search)
    object Mine : Screen("mine", "我的", android.R.drawable.ic_menu_manage)
    companion object {
        // 底部导航栏的页面列表
        val bottomNavScreens: List<Screen> by lazy {
            listOf(Home, SearchPage, Playlist, NowPlaying, Mine).filterNotNull()
        }
    }
}