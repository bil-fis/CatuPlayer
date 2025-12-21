package com.petitbear.catuplayer.utils

import android.content.Context
import android.util.Log
import com.petitbear.catuplayer.data.SearchHistoryDao
import com.petitbear.catuplayer.models.SearchHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchHistoryManager(private val searchHistoryDao: SearchHistoryDao) {

    private val TAG = "SearchHistoryManager"

    /**
     * 添加搜索历史
     */
    suspend fun addSearchHistory(query: String, resultCount: Int = 0): SearchHistory {
        return withContext(Dispatchers.IO) {
            try {
                return@withContext searchHistoryDao.upsertSearchHistory(query, resultCount)
            } catch (e: Exception) {
                Log.e(TAG, "添加搜索历史失败", e)
                throw e
            }
        }
    }

    /**
     * 获取搜索历史
     */
    suspend fun getSearchHistory(limit: Int = 20): List<SearchHistory> {
        return withContext(Dispatchers.IO) {
            try {
                val pinned = searchHistoryDao.getPinnedHistory()
                val unpinned = searchHistoryDao.getUnpinnedHistory()
                return@withContext (pinned + unpinned).take(limit)
            } catch (e: Exception) {
                Log.e(TAG, "获取搜索历史失败", e)
                return@withContext emptyList()
            }
        }
    }

    /**
     * 获取热门搜索
     */
    suspend fun getPopularSearches(limit: Int = 10): List<SearchHistory> {
        return withContext(Dispatchers.IO) {
            try {
                val allHistory = searchHistoryDao.getRecent(1000) // 获取足够多的历史记录
                return@withContext allHistory
                    .sortedByDescending { it.clickCount }
                    .take(limit)
            } catch (e: Exception) {
                Log.e(TAG, "获取热门搜索失败", e)
                return@withContext emptyList()
            }
        }
    }

    /**
     * 删除搜索历史
     */
    suspend fun deleteSearchHistory(historyId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                searchHistoryDao.deleteById(historyId)
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "删除搜索历史失败", e)
                return@withContext false
            }
        }
    }

    /**
     * 清空搜索历史
     */
    suspend fun clearAllHistory(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                searchHistoryDao.deleteAll()
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "清空搜索历史失败", e)
                return@withContext false
            }
        }
    }

    /**
     * 置顶/取消置顶
     */
    suspend fun togglePin(historyId: String, pinned: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                searchHistoryDao.updatePinStatus(historyId, pinned)
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "置顶搜索历史失败", e)
                return@withContext false
            }
        }
    }
}