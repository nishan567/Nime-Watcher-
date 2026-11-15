package com.example.data.repository

import com.example.data.catalog.CatalogProviderConfig
import com.example.data.local.AnimeCatalogDao
import com.example.data.local.CachedAnimeEntity
import com.example.data.local.PlaybackProgressEntity
import com.example.data.local.QuickListItemEntity
import com.example.data.local.SearchHistoryEntity
import com.example.domain.catalog.AnimeCatalogProvider
import com.example.domain.model.AnimeDetails
import com.example.domain.model.AnimeEpisode
import com.example.domain.model.AnimeFormat
import com.example.domain.model.AnimeSearchFilter
import com.example.domain.model.AnimeSeasonGroup
import com.example.domain.model.AnimeSeries
import com.example.domain.model.AnimeSortOption
import com.example.domain.model.CatalogPageResult
import com.example.domain.model.CatalogSectionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

data class HomeCatalogPayload(
    val sections: Map<CatalogSectionType, List<AnimeSeries>>,
    val fromCache: Boolean,
    val providerName: String
)

class AnimeCatalogRepository(
    private val provider: AnimeCatalogProvider,
    private val dao: AnimeCatalogDao,
    private val json: Json = CatalogProviderConfig.sharedJson
) {
    private val detailsCache = ConcurrentHashMap<String, AnimeDetails>()

    val providerDisplayName: String get() = provider.displayName

    fun observeSearchHistory(): Flow<List<String>> =
        dao.observeSearchHistory().map { rows -> rows.map { it.query } }

    fun observeFavorites(): Flow<List<AnimeSeries>> =
        dao.observeQuickList(QuickListItemEntity.TYPE_FAVORITE).map { rows ->
            rows.mapNotNull { runCatching { json.decodeFromString<AnimeSeries>(it.seriesJson) }.getOrNull() }
        }

    fun observeWatchLater(): Flow<List<AnimeSeries>> =
        dao.observeQuickList(QuickListItemEntity.TYPE_WATCH_LATER).map { rows ->
            rows.mapNotNull { runCatching { json.decodeFromString<AnimeSeries>(it.seriesJson) }.getOrNull() }
        }

    fun observeContinueWatching(): Flow<List<PlaybackProgressEntity>> =
        dao.observeContinueWatching()

    suspend fun getSavedPositionMs(animeId: String, seasonId: String, episodeNumber: Int): Long =
        withContext(Dispatchers.IO) {
            val saved = dao.getEpisodeProgress(animeId, seasonId, episodeNumber) ?: return@withContext 0L
            // Reset to start if already >=96% finished
            if (saved.durationMs > 0L && saved.positionMs >= (saved.durationMs * 0.96).toLong()) {
                0L
            } else {
                saved.positionMs
            }
        }

    suspend fun savePlaybackProgress(
        animeId: String,
        animeTitle: String,
        posterUrl: String?,
        seasonId: String,
        episodeNumber: Int,
        episodeTitle: String,
        positionMs: Long,
        durationMs: Long
    ) {
        if (durationMs <= 0L || positionMs < 1500L) return
        withContext(Dispatchers.IO) {
            dao.upsertPlaybackProgress(
                PlaybackProgressEntity(
                    animeId = animeId,
                    animeTitle = animeTitle,
                    posterUrl = posterUrl,
                    seasonId = seasonId,
                    episodeNumber = episodeNumber,
                    episodeTitle = episodeTitle,
                    positionMs = positionMs,
                    durationMs = durationMs
                )
            )
        }
    }

    suspend fun toggleFavorite(series: AnimeSeries): Boolean = withContext(Dispatchers.IO) {
        val exists = dao.isInQuickList(series.id, QuickListItemEntity.TYPE_FAVORITE)
        if (exists) {
            dao.removeQuickListItem(series.id, QuickListItemEntity.TYPE_FAVORITE)
            false
        } else {
            dao.upsertQuickListItem(
                QuickListItemEntity(
                    animeId = series.id,
                    listType = QuickListItemEntity.TYPE_FAVORITE,
                    seriesJson = json.encodeToString(AnimeSeries.serializer(), series)
                )
            )
            true
        }
    }

    suspend fun toggleWatchLater(series: AnimeSeries): Boolean = withContext(Dispatchers.IO) {
        val exists = dao.isInQuickList(series.id, QuickListItemEntity.TYPE_WATCH_LATER)
        if (exists) {
            dao.removeQuickListItem(series.id, QuickListItemEntity.TYPE_WATCH_LATER)
            false
        } else {
            dao.upsertQuickListItem(
                QuickListItemEntity(
                    animeId = series.id,
                    listType = QuickListItemEntity.TYPE_WATCH_LATER,
                    seriesJson = json.encodeToString(AnimeSeries.serializer(), series)
                )
            )
            true
        }
    }

    suspend fun recordSearchQuery(query: String) {
        val cleaned = query.trim()
        if (cleaned.length < 2) return
        withContext(Dispatchers.IO) {
            dao.insertSearchHistory(SearchHistoryEntity(query = cleaned))
        }
    }

    suspend fun deleteSearchHistoryItem(query: String) {
        withContext(Dispatchers.IO) {
            dao.deleteSearchHistoryItem(query)
        }
    }

    suspend fun clearSearchHistory() {
        withContext(Dispatchers.IO) {
            dao.clearSearchHistory()
        }
    }

    suspend fun loadHomeCatalog(): Result<HomeCatalogPayload> = withContext(Dispatchers.IO) {
        try {
            val remoteSections = provider.getHomeSections()
            if (remoteSections.isNotEmpty()) {
                val now = System.currentTimeMillis()
                remoteSections.forEach { (section, items) ->
                    dao.clearSectionCache(section.key)
                    dao.insertCachedAnime(
                        items.mapIndexed { idx, anime ->
                            CachedAnimeEntity(
                                animeId = anime.id,
                                sectionKey = section.key,
                                sortOrder = idx,
                                title = anime.title,
                                seriesJson = json.encodeToString(AnimeSeries.serializer(), anime),
                                cachedAt = now
                            )
                        }
                    )
                }
                return@withContext Result.success(
                    HomeCatalogPayload(
                        sections = remoteSections,
                        fromCache = false,
                        providerName = provider.displayName
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (remoteError: Exception) {
            val cached = readAllCachedSections()
            if (cached.isNotEmpty()) {
                return@withContext Result.success(
                    HomeCatalogPayload(
                        sections = cached,
                        fromCache = true,
                        providerName = "${provider.displayName} (Cached)"
                    )
                )
            }
            return@withContext Result.failure(remoteError)
        }

        val cached = readAllCachedSections()
        if (cached.isNotEmpty()) {
            Result.success(
                HomeCatalogPayload(
                    sections = cached,
                    fromCache = true,
                    providerName = "${provider.displayName} (Cached)"
                )
            )
        } else {
            Result.failure(IllegalStateException("Unable to load anime catalog."))
        }
    }

    suspend fun loadSectionPage(section: CatalogSectionType, page: Int): Result<CatalogPageResult> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(provider.getSectionPage(section, page))
            } catch (e: CancellationException) {
                throw e
            } catch (remoteError: Exception) {
                if (page == 1) {
                    val cached = dao.getCachedSection(section.key)
                        .mapNotNull { runCatching { json.decodeFromString<AnimeSeries>(it.seriesJson) }.getOrNull() }
                    if (cached.isNotEmpty()) {
                        return@withContext Result.success(
                            CatalogPageResult(
                                items = cached,
                                currentPage = 1,
                                hasNextPage = false,
                                fromCache = true,
                                providerName = "${provider.displayName} (Cached)"
                            )
                        )
                    }
                }
                Result.failure(remoteError)
            }
        }

    suspend fun searchAnime(filter: AnimeSearchFilter): Result<CatalogPageResult> =
        withContext(Dispatchers.IO) {
            try {
                val result = provider.searchAnime(filter)
                if (result.items.isNotEmpty() && filter.page == 1 && !filter.hasActiveFilters && filter.query.isBlank()) {
                    dao.insertCachedAnime(
                        result.items.mapIndexed { idx, anime ->
                            CachedAnimeEntity(
                                animeId = anime.id,
                                sectionKey = "search_default",
                                sortOrder = idx,
                                title = anime.title,
                                seriesJson = json.encodeToString(AnimeSeries.serializer(), anime)
                            )
                        }
                    )
                }
                Result.success(result)
            } catch (e: CancellationException) {
                throw e
            } catch (remoteError: Exception) {
                if (filter.page == 1 && filter.query.isNotBlank()) {
                    val cached = dao.searchCachedAnime(filter.query.trim())
                        .mapNotNull { runCatching { json.decodeFromString<AnimeSeries>(it.seriesJson) }.getOrNull() }
                    if (cached.isNotEmpty()) {
                        return@withContext Result.success(
                            CatalogPageResult(
                                items = cached,
                                currentPage = 1,
                                hasNextPage = false,
                                fromCache = true,
                                providerName = "${provider.displayName} (Offline Cache)"
                            )
                        )
                    }
                }
                Result.failure(remoteError)
            }
        }

    suspend fun fetchCategoryAnime(
        queryValue: String,
        filterKind: String,
        sortBy: AnimeSortOption,
        format: AnimeFormat?,
        page: Int = 1
    ): Result<CatalogPageResult> = withContext(Dispatchers.IO) {
        val filter = AnimeSearchFilter(
            selectedGenre = if (filterKind == "genre") queryValue else null,
            selectedTag = if (filterKind == "tag") queryValue else null,
            format = format,
            sortBy = sortBy,
            page = page
        )
        val cacheKey = "discover_${filterKind}_${queryValue.lowercase()}"
        try {
            val result = provider.searchAnime(filter)
            if (page == 1 && format == null && sortBy == AnimeSortOption.TRENDING && result.items.isNotEmpty()) {
                dao.clearSectionCache(cacheKey)
                dao.insertCachedAnime(
                    result.items.mapIndexed { idx, anime ->
                        CachedAnimeEntity(
                            animeId = anime.id,
                            sectionKey = cacheKey,
                            sortOrder = idx,
                            title = anime.title,
                            seriesJson = json.encodeToString(AnimeSeries.serializer(), anime)
                        )
                    }
                )
            }
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (remoteError: Exception) {
            if (page == 1) {
                val cached = dao.getCachedSection(cacheKey)
                    .mapNotNull { runCatching { json.decodeFromString<AnimeSeries>(it.seriesJson) }.getOrNull() }
                if (cached.isNotEmpty()) {
                    return@withContext Result.success(
                        CatalogPageResult(
                            items = cached,
                            currentPage = 1,
                            hasNextPage = false,
                            fromCache = true,
                            providerName = "${provider.displayName} (Offline Cache)"
                        )
                    )
                }
            }
            Result.failure(remoteError)
        }
    }

    suspend fun getSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            provider.getSuggestions(query)
        } catch (_: Exception) {
            dao.searchCachedAnime(query.trim()).map { it.title }.distinct().take(6)
        }
    }

    suspend fun getAnimeDetails(animeId: String): Result<AnimeDetails> = withContext(Dispatchers.IO) {
        detailsCache[animeId]?.let { cachedDetail ->
            return@withContext Result.success(cachedDetail)
        }
        try {
            val details = provider.getAnimeDetails(animeId)
            detailsCache[animeId] = details
            Result.success(details)
        } catch (e: CancellationException) {
            throw e
        } catch (remoteError: Exception) {
            val cachedEntity = dao.getCachedAnimeById(animeId)
            val series = cachedEntity?.let {
                runCatching { json.decodeFromString<AnimeSeries>(it.seriesJson) }.getOrNull()
            }
            if (series != null) {
                val fallbackSeason = AnimeSeasonGroup(
                    seasonId = series.id,
                    animeId = series.id,
                    seasonNumber = 1,
                    label = "Season 1 (${series.format.displayLabel})",
                    format = series.format,
                    episodeCount = series.episodeCount,
                    releaseYear = series.releaseYear,
                    coverUrl = series.posterUrl
                )
                val episodes = (1..(series.episodeCount ?: 0).coerceAtMost(200)).map { epNum ->
                    AnimeEpisode(
                        episodeId = "${series.id}_ep_$epNum",
                        animeId = series.id,
                        seasonId = fallbackSeason.seasonId,
                        episodeNumber = epNum,
                        title = if (series.format == AnimeFormat.MOVIE && series.episodeCount == 1) {
                            "Full Feature"
                        } else {
                            "Episode $epNum"
                        },
                        thumbnailUrl = series.posterUrl,
                        authorizedStreamUrl = null
                    )
                }
                Result.success(
                    AnimeDetails(
                        series = series,
                        seasons = listOf(fallbackSeason),
                        episodesBySeason = mapOf(fallbackSeason.seasonId to episodes),
                        relatedAnime = emptyList(),
                        providerDisplayName = "${provider.displayName} (Offline Cache)",
                        streamConfigurationNotice = "Showing cached catalog metadata while offline. Authorized streams attach in Stage 4."
                    )
                )
            } else {
                Result.failure(remoteError)
            }
        }
    }

    suspend fun getEpisodesForSeason(animeId: String, season: AnimeSeasonGroup): List<AnimeEpisode> =
        withContext(Dispatchers.IO) {
            provider.getEpisodesForSeason(animeId, season)
        }

    private suspend fun readAllCachedSections(): Map<CatalogSectionType, List<AnimeSeries>> {
        val map = linkedMapOf<CatalogSectionType, List<AnimeSeries>>()
        CatalogSectionType.entries.forEach { section ->
            val rows = dao.getCachedSection(section.key)
            if (rows.isNotEmpty()) {
                val parsed = rows.mapNotNull {
                    runCatching { json.decodeFromString<AnimeSeries>(it.seriesJson) }.getOrNull()
                }
                if (parsed.isNotEmpty()) map[section] = parsed
            }
        }
        return map
    }
}
