package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeCatalogDao {

    @Query("SELECT * FROM cached_anime WHERE sectionKey = :sectionKey ORDER BY sortOrder ASC")
    suspend fun getCachedSection(sectionKey: String): List<CachedAnimeEntity>

    @Query("SELECT * FROM cached_anime WHERE animeId = :animeId LIMIT 1")
    suspend fun getCachedAnimeById(animeId: String): CachedAnimeEntity?

    @Query("SELECT * FROM cached_anime WHERE title LIKE '%' || :query || '%' GROUP BY animeId ORDER BY cachedAt DESC LIMIT 30")
    suspend fun searchCachedAnime(query: String): List<CachedAnimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedAnime(items: List<CachedAnimeEntity>)

    @Query("DELETE FROM cached_anime WHERE sectionKey = :sectionKey")
    suspend fun clearSectionCache(sectionKey: String)

    // Search history
    @Query("SELECT * FROM search_history ORDER BY searchedAt DESC LIMIT 15")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun deleteSearchHistoryItem(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // Quick Favorites & Watch Later persistence (synced with Firestore in Stage 6)
    @Query("SELECT * FROM quick_list_items WHERE listType = :listType ORDER BY addedAt DESC")
    fun observeQuickList(listType: String): Flow<List<QuickListItemEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM quick_list_items WHERE animeId = :animeId AND listType = :listType)")
    suspend fun isInQuickList(animeId: String, listType: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuickListItem(item: QuickListItemEntity)

    @Query("DELETE FROM quick_list_items WHERE animeId = :animeId AND listType = :listType")
    suspend fun removeQuickListItem(animeId: String, listType: String)

    // Playback progress (Continue Watching foundation)
    @Query("SELECT * FROM playback_progress ORDER BY updatedAt DESC LIMIT 20")
    fun observeContinueWatching(): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE animeId = :animeId AND seasonId = :seasonId AND episodeNumber = :episodeNumber LIMIT 1")
    suspend fun getEpisodeProgress(animeId: String, seasonId: String, episodeNumber: Int): PlaybackProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaybackProgress(progress: PlaybackProgressEntity)

    @Query("DELETE FROM playback_progress WHERE animeId = :animeId")
    suspend fun removePlaybackProgress(animeId: String)
}
