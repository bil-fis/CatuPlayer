package com.petitbear.catuplayer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petitbear.catuplayer.data.AppDatabase
import com.petitbear.catuplayer.models.AppViewModelFactory
import com.petitbear.catuplayer.models.AudioPlayerViewModel
import com.petitbear.catuplayer.ui.theme.CatuPlayerTheme
import com.petitbear.catuplayer.utils.SearchManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var viewModel: AudioPlayerViewModel? = null
    private var databaseInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 请求存储权限
        if (!checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            requestPermission()
        }

        setContent {
            CatuPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 在 Composable 函数内部正确创建 Factory
                    val appContext = LocalContext.current.applicationContext as android.app.Application
                    val factory = remember { AppViewModelFactory(appContext) }

                    val audioPlayerViewModel: AudioPlayerViewModel = viewModel(
                        factory = factory
                    )

                    // 将 viewModel 保存到 Activity 变量中
                    viewModel = audioPlayerViewModel

                    CatuApp(audioPlayerViewModel)
                }
            }
        }
    }

    private fun initializeDatabase() {
        // 这里只需要获取一次数据库实例，不要在其他地方重复获取
        // AppDatabase.getInstance(this) 已经在 ViewModel Factory 中调用了
        Log.d("MainActivity", "数据库初始化完成")
    }

    suspend fun initializeSearchSystem(context: Context, viewModel: AudioPlayerViewModel) {
        try {
            // 获取数据库实例
            val database = AppDatabase.getInstance(context)
            val searchManager = SearchManager(context, database.searchIndexDao())

            // 获取所有歌曲并建立索引
            val allSongs = viewModel.databasePlaylist.value
            if (allSongs.isNotEmpty()) {
                searchManager.buildIndexForSongs(allSongs)
                Log.d("Search", "搜索系统初始化完成，为 ${allSongs.size} 首歌曲建立了索引")
            }
        } catch (e: Exception) {
            Log.e("Search", "搜索系统初始化失败", e)
        }
    }

    /*
    * 动态申请权限
    * */
    fun checkPermission(permission: String): Boolean {
        val checkSelfPermission = ActivityCompat.checkSelfPermission(applicationContext, permission)
        if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
            return false
        } else {
            return true
        }
    }

    private fun requestPermission() {
        //可以添加多个权限申请
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        requestPermissions(permissions, 1)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //是否获取到权限
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        }
    }
}