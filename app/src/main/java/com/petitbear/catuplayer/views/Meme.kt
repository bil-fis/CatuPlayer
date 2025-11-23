package com.petitbear.catuplayer.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.petitbear.catuplayer.models.AudioPlayerViewModel
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
            }
        }
    }
}