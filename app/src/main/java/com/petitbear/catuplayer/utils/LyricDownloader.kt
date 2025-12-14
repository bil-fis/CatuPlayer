package com.petitbear.catuplayer.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.petitbear.catuplayer.ConstantVariables
import com.petitbear.catuplayer.models.LyricResponse
import com.petitbear.catuplayer.models.NetSongResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object LyricDownloader {

    private const val TAG = "LyricDownloader"
    private const val LYRICS_DIRECTORY = "lyrics"

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(500,TimeUnit.SECONDS)
        .readTimeout(500, TimeUnit.SECONDS)
        .build()

    /**
     * 自动从网络获取歌词
     */
    suspend fun autoGetLyricFromNetwork(
        context: Context,
        keyword: String
    ):String = withContext(Dispatchers.IO){
        return@withContext try {
            val encodedKeyWord = URLEncoder.encode(keyword,"UTF-8")
            // 进行搜索获取id
            val searchURL = "${ConstantVariables.SEARCH_URL}?keywords=$encodedKeyWord"
            Log.i("LyricFetcher","开始搜索歌曲：构造的url $searchURL")
            val searchRequest = Request.Builder()
                .url(searchURL)
                .get()
                .build()
            val searchResponse = httpClient.newCall(searchRequest).execute()
            if (searchResponse.isSuccessful) {
                val searchJson = searchResponse.body?.string() ?: ""
                val searchList = gson.fromJson(searchJson, NetSongResponse::class.java)
                val searchResultList = searchList.result.songs
                if(searchResultList.isNotEmpty()){
                    val lrcUrl = "${ConstantVariables.LYRIC_URL}?id=${searchResultList.first().id}"
                    Log.i("LyricFetcher","开始获取歌词：构造的url $lrcUrl")
                    val lrcRequest = Request.Builder()
                        .url(lrcUrl)
                        .get()
                        .build()
                    val lrcResponse = httpClient.newCall(lrcRequest).execute()
                    if(lrcResponse.isSuccessful){
                        val lrcJson = lrcResponse.body?.string()?:""
                        val lrcList = gson.fromJson(lrcJson, LyricResponse::class.java)
                        val lrc = lrcList.lrc?.lyric
                        lrc
                    }else {
                        Log.e(TAG, "lrc获取失败: ${searchResponse.code}")
                        ""
                    }
                }else {
                    Log.e(TAG, "搜索失败: ${searchResponse.code}")
                    ""
                }
            } else {
                Log.e(TAG, "搜索失败: ${searchResponse.code}")
                ""
            }
        }catch (e:Exception){
            ""
        }
    } as String
}