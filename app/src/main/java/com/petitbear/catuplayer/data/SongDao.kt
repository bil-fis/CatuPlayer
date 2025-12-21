// data/SongDao.kt
package com.petitbear.catuplayer.data

import androidx.room.*
import androidx.paging.PagingSource
import com.petitbear.catuplayer.models.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    // 基础 CRUD 操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<Song>)

    @Update
    suspend fun update(song: Song)

    @Delete
    suspend fun delete(song: Song)

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteById(songId: String)

    @Query("DELETE FROM songs")
    suspend fun deleteAll()

    // 查询单个歌曲
    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: String): Song?

    @Query("SELECT * FROM songs WHERE uri = :uri")
    suspend fun getSongByUri(uri: String): Song?

    @Query("SELECT * FROM songs WHERE filePath = :filePath")
    suspend fun getSongByFilePath(filePath: String): Song?

    // 分页查询
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE")
    fun getSongsPaged(): PagingSource<Int, Song>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY title COLLATE NOCASE")
    fun getSongsByArtistPaged(artist: String): PagingSource<Int, Song>

    @Query("SELECT * FROM songs WHERE album = :album ORDER BY discNumber, trackNumber")
    fun getSongsByAlbumPaged(album: String): PagingSource<Int, Song>

    @Query("SELECT * FROM songs WHERE genre = :genre ORDER BY title COLLATE NOCASE")
    fun getSongsByGenrePaged(genre: String): PagingSource<Int, Song>

    @Query("SELECT * FROM songs WHERE year = :year ORDER BY title COLLATE NOCASE")
    fun getSongsByYearPaged(year: Int): PagingSource<Int, Song>

    // 收藏歌曲
    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE")
    fun getFavoriteSongsPaged(): PagingSource<Int, Song>

    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    suspend fun getFavoriteSongs(): List<Song>

    // 最近播放
    @Query("SELECT * FROM songs WHERE lastPlayedTime > 0 ORDER BY lastPlayedTime DESC")
    fun getRecentlyPlayedPaged(): PagingSource<Int, Song>

    @Query("SELECT * FROM songs WHERE lastPlayedTime > 0 ORDER BY lastPlayedTime DESC LIMIT :limit")
    suspend fun getRecentlyPlayed(limit: Int = 50): List<Song>

    // 最常播放
    @Query("SELECT * FROM songs WHERE playCount > 0 ORDER BY playCount DESC")
    fun getMostPlayedPaged(): PagingSource<Int, Song>

    @Query("SELECT * FROM songs WHERE playCount > 0 ORDER BY playCount DESC LIMIT :limit")
    suspend fun getMostPlayed(limit: Int = 50): List<Song>

    // 最近添加
    @Query("SELECT * FROM songs ORDER BY createdTime DESC")
    fun getRecentlyAddedPaged(): PagingSource<Int, Song>

    @Query("SELECT * FROM songs ORDER BY createdTime DESC LIMIT :limit")
    suspend fun getRecentlyAdded(limit: Int = 50): List<Song>

    // 搜索
    @Query("""
        SELECT * FROM songs 
        WHERE title LIKE '%' || :query || '%' 
           OR artist LIKE '%' || :query || '%' 
           OR album LIKE '%' || :query || '%' 
           OR genre LIKE '%' || :query || '%'
        ORDER BY title COLLATE NOCASE
    """)
    fun searchSongsPaged(query: String): PagingSource<Int, Song>

    @Query("""
        SELECT * FROM songs 
        WHERE title LIKE '%' || :query || '%' 
           OR artist LIKE '%' || :query || '%' 
           OR album LIKE '%' || :query || '%' 
           OR genre LIKE '%' || :query || '%'
    """)
    suspend fun searchSongs(query: String): List<Song>

    // 更新播放统计
    @Query("UPDATE songs SET lastPlayedTime = :timestamp, playCount = playCount + 1 WHERE id = :songId")
    suspend fun updatePlayStats(songId: String, timestamp: Long)

    // 标记收藏
    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun setFavorite(songId: String, isFavorite: Boolean)

    // 更新封面
    @Query("UPDATE songs SET coverUri = :coverUri, hasEmbeddedCover = :hasEmbeddedCover WHERE id = :songId")
    suspend fun updateCover(songId: String, coverUri: String, hasEmbeddedCover: Boolean)

    // 更新歌词信息
    @Query("UPDATE songs SET lrcCachePath = :lrcPath, lyricSource = :source WHERE id = :songId")
    suspend fun updateLyricInfo(songId: String, lrcPath: String, source: String)

    // 获取统计数据
    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM songs WHERE isFavorite = 1")
    suspend fun getFavoriteCount(): Int

    @Query("SELECT COUNT(DISTINCT artist) FROM songs")
    suspend fun getArtistCount(): Int

    @Query("SELECT COUNT(DISTINCT album) FROM songs")
    suspend fun getAlbumCount(): Int

    @Query("SELECT COUNT(DISTINCT genre) FROM songs")
    suspend fun getGenreCount(): Int

    // 获取去重列表
    @Query("SELECT DISTINCT artist FROM songs WHERE artist != '' ORDER BY artist COLLATE NOCASE")
    suspend fun getAllArtists(): List<String>

    @Query("SELECT DISTINCT album FROM songs WHERE album != '' ORDER BY album COLLATE NOCASE")
    suspend fun getAllAlbums(): List<String>

    @Query("SELECT DISTINCT genre FROM songs WHERE genre != '' ORDER BY genre COLLATE NOCASE")
    suspend fun getAllGenres(): List<String>

    // 获取专辑信息
    @Query("SELECT album, artist, COUNT(*) as songCount, SUM(duration) as totalDuration FROM songs WHERE album != '' GROUP BY album, artist ORDER BY album COLLATE NOCASE")
    suspend fun getAlbumInfo(): List<AlbumInfo>

    // 获取艺术家信息
    @Query("SELECT artist, COUNT(*) as songCount, SUM(duration) as totalDuration FROM songs WHERE artist != '' GROUP BY artist ORDER BY artist COLLATE NOCASE")
    suspend fun getArtistInfo(): List<ArtistInfo>

    // 检查重复歌曲
    @Query("SELECT uri, COUNT(*) as count FROM songs GROUP BY uri HAVING count > 1")
    suspend fun findDuplicateUris(): List<DuplicateInfo>

    // 批量操作
    @Query("SELECT * FROM songs WHERE id IN (:songIds)")
    suspend fun getSongsByIds(songIds: List<String>): List<Song>

    @Query("DELETE FROM songs WHERE id IN (:songIds)")
    suspend fun deleteSongsByIds(songIds: List<String>)

    // 数据完整性检查
    @Query("SELECT * FROM songs WHERE uri = '' OR duration <= 0")
    suspend fun findInvalidSongs(): List<Song>

    // 更新歌曲元数据
    @Query("""
        UPDATE songs 
        SET title = :title, artist = :artist, album = :album, 
            duration = :duration, genre = :genre, year = :year,
            trackNumber = :trackNumber, discNumber = :discNumber,
            hasMetadata = :hasMetadata, bitrate = :bitrate,
            sampleRate = :sampleRate, channels = :channels,
            fileFormat = :fileFormat
        WHERE id = :songId
    """)
    suspend fun updateMetadata(
        songId: String,
        title: String,
        artist: String,
        album: String,
        duration: Long,
        genre: String,
        year: Int,
        trackNumber: Int,
        discNumber: Int,
        hasMetadata: Boolean,
        bitrate: Int,
        sampleRate: Int,
        channels: Int,
        fileFormat: String
    )
}





