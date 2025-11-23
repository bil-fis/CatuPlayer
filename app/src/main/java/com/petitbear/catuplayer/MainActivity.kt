package com.petitbear.catuplayer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petitbear.catuplayer.media.MediaPlaybackService
import com.petitbear.catuplayer.ui.theme.CatuPlayerTheme
import com.petitbear.catuplayer.models.AudioPlayerViewModel
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private var mediaPlaybackService: MediaPlaybackService? = null
    private var isBound = false
    private var viewModel: AudioPlayerViewModel? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MediaPlaybackService.MediaPlaybackBinder
            mediaPlaybackService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            mediaPlaybackService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 先创建 ViewModel
        setContent {
            CatuPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val audioPlayerViewModel: AudioPlayerViewModel = viewModel()
                    this.viewModel = audioPlayerViewModel

                    // 当服务绑定后，设置到 ViewModel
                    if (isBound && mediaPlaybackService != null) {
                        LaunchedEffect(mediaPlaybackService) {
                            audioPlayerViewModel.setMediaPlaybackService(mediaPlaybackService!!)
                            // 同时将 ViewModel 设置到 Service
                            mediaPlaybackService?.setViewModel(audioPlayerViewModel)
                        }
                    }

                    CatuApp(audioPlayerViewModel)
                }
            }
        }

        // 在设置内容后启动服务
        val intent = Intent(this, MediaPlaybackService::class.java)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    /*
    * 动态申请权限
    * */
    fun checkPermission(permission:String):Boolean{
        val checkSelfPermission = ActivityCompat.checkSelfPermission(applicationContext,permission)
        if(checkSelfPermission != PackageManager.PERMISSION_GRANTED){
            return false
        }else{
            return true
        }
    }

    private fun requestPermission() {
        //可以添加多个权限申请
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        requestPermissions(permissions,1)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //是否获取到权限
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED){

        }
    }
}
