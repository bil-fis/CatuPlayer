package com.petitbear.catuplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.petitbear.catuplayer.models.SearchIndex
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchIndexDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(index: SearchIndex)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(indices: List<SearchIndex>)

    @Query("SELECT * FROM search_indices WHERE songId = :songId")
    suspend fun getBySongId(songId: String): List<SearchIndex>

    @Query("SELECT * FROM search_indices WHERE keyword LIKE '%' || :keyword || '%'")
    suspend fun searchByKeyword(keyword: String): List<SearchIndex>

    @Query("SELECT * FROM search_indices WHERE keyword = :keyword")
    suspend fun getExactKeyword(keyword: String): List<SearchIndex>

    @Query("DELETE FROM search_indices WHERE songId = :songId")
    suspend fun deleteBySongId(songId: String)

    @Query("DELETE FROM search_indices")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM search_indices WHERE keyword = :keyword")
    suspend fun getKeywordFrequency(keyword: String): Int

    @Query("SELECT COUNT(DISTINCT songId) FROM search_indices")
    suspend fun getTotalDocuments(): Int

    @Query("SELECT COUNT(DISTINCT songId) FROM search_indices WHERE keyword = :keyword")
    suspend fun getDocumentFrequency(keyword: String): Int

    @Transaction
    @Query("SELECT keyword, COUNT(*) as frequency FROM search_indices GROUP BY keyword")
    suspend fun getAllKeywordFrequencies(): List<KeywordFrequency>

    @Transaction
    @Query("SELECT keyword, COUNT(DISTINCT songId) as frequency FROM search_indices GROUP BY keyword")
    suspend fun getAllDocumentFrequencies(): List<DocumentFrequency>

    @Query("SELECT * FROM search_indices WHERE keyword LIKE :keyword || '%' ORDER BY weight DESC LIMIT :limit")
    suspend fun findKeywordsStartingWith(keyword: String, limit: Int = 50): List<SearchIndex>
}