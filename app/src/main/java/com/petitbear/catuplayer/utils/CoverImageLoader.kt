package com.petitbear.catuplayer.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 专辑封面加载器
 */
object CoverImageLoader {

    /**
     * 获取专辑封面加载器
     */
    @Composable
    fun rememberCoverPainter(
        context: Context,
        song: com.petitbear.catuplayer.models.Song?,
        currentSong: com.petitbear.catuplayer.models.Song?,
        embeddedPicture: ByteArray?
    ) = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(getCoverData(song, currentSong, embeddedPicture))
            .crossfade(true)
            .build(),
        imageLoader = context.imageLoader
    )

    /**
     * 获取封面数据
     */
    private fun getCoverData(
        song: com.petitbear.catuplayer.models.Song?,
        currentSong: com.petitbear.catuplayer.models.Song?,
        embeddedPicture: ByteArray?
    ): Any? {
        // 优先使用当前歌曲的内嵌封面
        if (song != null && song.hasEmbeddedCover && embeddedPicture != null) {
            return embeddedPicture
        }

        // 其次使用歌曲的封面URI
        if (song != null && song.coverUri.isNotEmpty()) {
            return song.coverUri
        }

        // 最后使用当前歌曲的内嵌封面（备用）
        if (currentSong != null && currentSong.hasEmbeddedCover) {
            return currentSong.coverUri
        }

        return null
    }

    /**
     * 从内嵌图片数据创建Bitmap
     */
    suspend fun createBitmapFromEmbeddedPicture(embeddedPicture: ByteArray?): Bitmap? {
        return withContext(Dispatchers.IO) {
            embeddedPicture?.let {
                try {
                    MusicMetadataUtils.createBitmapFromEmbeddedPicture(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }
}