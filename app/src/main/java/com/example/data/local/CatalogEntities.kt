package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_anime",
    primaryKeys = ["animeId", "sectionKey"]
)
data class CachedAnimeEntity(
    val animeId: String,
    val sectionKey: String,
    val sortOrder: Int,
    val title: String,
    val seriesJson: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "quick_list_items",
    primaryKeys = ["animeId", "listType"]
)
data class QuickListItemEntity(
    val animeId: String,
    val listType: String,
    val seriesJson: String,
    val addedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_FAVORITE = "FAVORITE"
        const val TYPE_WATCH_LATER = "WATCH_LATER"
    }
}

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey val animeId: String,
    val animeTitle: String,
    val posterUrl: String?,
    val seasonId: String,
    val episodeNumber: Int,
    val episodeTitle: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
