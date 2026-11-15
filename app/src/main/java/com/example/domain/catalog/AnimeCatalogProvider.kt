package com.example.domain.catalog

import com.example.domain.model.AnimeDetails
import com.example.domain.model.AnimeEpisode
import com.example.domain.model.AnimeSearchFilter
import com.example.domain.model.AnimeSeasonGroup
import com.example.domain.model.AnimeSeries
import com.example.domain.model.CatalogPageResult
import com.example.domain.model.CatalogSectionType

/**
 * Interface-based anime catalog provider abstraction.
 *
 * Architectural Rule (Nime Watcher):
 * Catalog metadata providers (title, synopsis, genres, verified scores, season relations)
 * are strictly separated from playable video stream providers (Stage 4).
 * A metadata provider MUST NOT invent unauthorized video URLs or fake ratings/episode counts.
 */
interface AnimeCatalogProvider {
    val providerId: String
    val displayName: String
    val providesPlayableStreams: Boolean

    suspend fun getHomeSections(): Map<CatalogSectionType, List<AnimeSeries>>

    suspend fun getSectionPage(section: CatalogSectionType, page: Int = 1): CatalogPageResult

    suspend fun searchAnime(filter: AnimeSearchFilter): CatalogPageResult

    suspend fun getSuggestions(query: String): List<String>

    suspend fun getAnimeDetails(animeId: String): AnimeDetails

    suspend fun getEpisodesForSeason(animeId: String, season: AnimeSeasonGroup): List<AnimeEpisode>
}
