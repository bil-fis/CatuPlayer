package com.petitbear.catuplayer.models

import kotlinx.serialization.Serializable

/**
 * 上一次播放状态数据类
 */
@Serializable
data class LastPlayedState(
    val songId: String,
    val position: Long, // 播放位置（毫秒）
    val timestamp: Long, // 保存时间戳
    val playMode: String // 播放模式
)