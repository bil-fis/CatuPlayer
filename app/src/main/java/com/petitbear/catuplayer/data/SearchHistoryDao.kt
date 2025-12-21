package com.petitbear.catuplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.petitbear.catuplayer.models.SearchHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SearchHistory)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 20): List<SearchHistory>

    @Query("SELECT * FROM search_history WHERE query = :query LIMIT 1")
    suspend fun getByQuery(query: String): SearchHistory?

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM search_history")
    suspend fun deleteAll()

    @Query("UPDATE search_history SET isPinned = :pinned WHERE id = :id")
    suspend fun updatePinStatus(id: String, pinned: Boolean)

    @Query("UPDATE search_history SET clickCount = clickCount + 1, lastClicked = :timestamp WHERE id = :id")
    suspend fun incrementClickCount(id: String, timestamp: Long)

    @Query("SELECT * FROM search_history WHERE isPinned = 1 ORDER BY lastClicked DESC")
    suspend fun getPinnedHistory(): List<SearchHistory>

    @Query("SELECT * FROM search_history WHERE isPinned = 0 ORDER BY timestamp DESC")
    suspend fun getUnpinnedHistory(): List<SearchHistory>

    @Transaction
    suspend fun upsertSearchHistory(query: String, resultCount: Int = 0): SearchHistory {
        val existing = getByQuery(query)
        return if (existing != null) {
            val updated = existing.copy(
                resultCount = resultCount,
                timestamp = System.currentTimeMillis(),
                clickCount = existing.clickCount + 1,
                lastClicked = System.currentTimeMillis()
            )
            insert(updated)
            updated
        } else {
            val newHistory = SearchHistory(
                query = query,
                resultCount = resultCount
            )
            insert(newHistory)
            newHistory
        }
    }
}