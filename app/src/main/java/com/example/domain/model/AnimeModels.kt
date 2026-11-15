package com.example.domain.model

import kotlinx.serialization.Serializable

enum class AnimeStatus(val displayLabel: String) {
    RELEASING("Ongoing"),
    FINISHED("Completed"),
    NOT_YET_RELEASED("Upcoming"),
    CANCELLED("Cancelled"),
    HIATUS("Hiatus"),
    UNKNOWN("Status Unknown");

    companion object {
        fun fromApi(raw: String?): AnimeStatus = when (raw?.uppercase()) {
            "RELEASING" -> RELEASING
            "FINISHED" -> FINISHED
            "NOT_YET_RELEASED" -> NOT_YET_RELEASED
            "CANCELLED" -> CANCELLED
            "HIATUS" -> HIATUS
            else -> UNKNOWN
        }
    }
}

enum class AnimeFormat(val displayLabel: String) {
    TV("TV Series"),
    TV_SHORT("TV Short"),
    MOVIE("Movie"),
    OVA("OVA"),
    ONA("ONA"),
    SPECIAL("Special"),
    UNKNOWN("Anime");

    companion object {
        fun fromApi(raw: String?): AnimeFormat = when (raw?.uppercase()) {
            "TV" -> TV
            "TV_SHORT" -> TV_SHORT
            "MOVIE" -> MOVIE
            "OVA" -> OVA
            "ONA" -> ONA
            "SPECIAL" -> SPECIAL
            else -> UNKNOWN
        }
    }
}

enum class AnimeReleaseSeason(val apiValue: String, val displayLabel: String) {
    WINTER("WINTER", "Winter"),
    SPRING("SPRING", "Spring"),
    SUMMER("SUMMER", "Summer"),
    FALL("FALL", "Fall")
}

enum class AudioFilter(val displayLabel: String) {
    ALL("All"),
    SUB("Subtitled"),
    DUB("Dubbed")
}

enum class AnimeSortOption(val apiSort: String, val displayLabel: String) {
    TRENDING("TRENDING_DESC", "Trending"),
    POPULARITY("POPULARITY_DESC", "Popular"),
    SCORE("SCORE_DESC", "Top Rated"),
    YEAR_DESC("START_DATE_DESC", "Newest"),
    TITLE_ASC("TITLE_ROMAJI", "Title A–Z")
}

enum class CatalogSectionType(val key: String, val title: String) {
    TRENDING("trending", "Trending Anime"),
    POPULAR("popular", "Popular Anime"),
    RECENTLY_ADDED("recently_added", "Recently Added"),
    RECENTLY_UPDATED("recently_updated", "Recently Updated"),
    MOST_WATCHED("most_watched", "Most Watched"),
    NEW_EPISODES("new_episodes", "New Episodes"),
    RECOMMENDED("recommended", "Recommended For You"),
    ACTION("action", "Action Anime"),
    ADVENTURE("adventure", "Adventure Anime"),
    ROMANCE("romance", "Romance Anime"),
    COMEDY("comedy", "Comedy Anime"),
    FANTASY("fantasy", "Fantasy Anime"),
    ISEKAI("isekai", "Isekai Anime"),
    DRAMA("drama", "Drama Anime"),
    MYSTERY("mystery", "Mystery Anime"),
    SUPERNATURAL("supernatural", "Supernatural Anime");

    companion object {
        fun fromKey(key: String): CatalogSectionType? = entries.firstOrNull { it.key == key }
    }
}

@Serializable
data class AnimeSeries(
    val id: String,
    val providerId: String,
    val externalId: String,
    val title: String,
    val englishTitle: String? = null,
    val nativeTitle: String? = null,
    val alternativeTitles: List<String> = emptyList(),
    val description: String = "",
    val posterUrl: String? = null,
    val bannerUrl: String? = null,
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val releaseYear: Int? = null,
    val seasonLabel: String? = null,
    val status: AnimeStatus = AnimeStatus.UNKNOWN,
    val format: AnimeFormat = AnimeFormat.UNKNOWN,
    val episodeCount: Int? = null,
    val verifiedRating: Double? = null,
    val popularity: Int? = null,
    val audioLanguages: List<String> = listOf("Japanese"),
    val subOrDubLabel: String? = null,
    val isAuthorizedStreamReady: Boolean = false
)

data class AnimeSeasonGroup(
    val seasonId: String,
    val animeId: String,
    val seasonNumber: Int,
    val label: String,
    val format: AnimeFormat,
    val episodeCount: Int? = null,
    val releaseYear: Int? = null,
    val coverUrl: String? = null
)

data class AnimeEpisode(
    val episodeId: String,
    val animeId: String,
    val seasonId: String,
    val episodeNumber: Int,
    val title: String,
    val thumbnailUrl: String? = null,
    val durationMinutes: Int? = null,
    val authorizedStreamUrl: String? = null,
    val streamProviderName: String? = null
)

data class AnimeDetails(
    val series: AnimeSeries,
    val seasons: List<AnimeSeasonGroup>,
    val episodesBySeason: Map<String, List<AnimeEpisode>>,
    val relatedAnime: List<AnimeSeries>,
    val providerDisplayName: String,
    val streamConfigurationNotice: String
)

data class AnimeSearchFilter(
    val query: String = "",
    val selectedGenre: String? = null,
    val selectedTag: String? = null,
    val releaseYear: Int? = null,
    val season: AnimeReleaseSeason? = null,
    val format: AnimeFormat? = null,
    val audioFilter: AudioFilter = AudioFilter.ALL,
    val sortBy: AnimeSortOption = AnimeSortOption.TRENDING,
    val page: Int = 1
) {
    val hasActiveFilters: Boolean
        get() = selectedGenre != null ||
            selectedTag != null ||
            releaseYear != null ||
            season != null ||
            format != null ||
            audioFilter != AudioFilter.ALL ||
            sortBy != AnimeSortOption.TRENDING
}

data class CatalogPageResult(
    val items: List<AnimeSeries>,
    val currentPage: Int,
    val hasNextPage: Boolean,
    val fromCache: Boolean = false,
    val providerName: String = ""
)

object AnimeGenreCatalog {
    val searchFilterGenres: List<String> = listOf(
        "Action",
        "Adventure",
        "Romance",
        "Comedy",
        "Fantasy",
        "Horror",
        "Mystery",
        "Sci-Fi",
        "Isekai",
        "Drama",
        "Supernatural"
    )

    val trendingSearchKeywords: List<String> = listOf(
        "Frieren",
        "Solo Leveling",
        "Attack on Titan",
        "Demon Slayer",
        "Jujutsu Kaisen",
        "One Piece",
        "Spy x Family",
        "Cyberpunk"
    )

    val selectableYears: List<Int> = listOf(
        2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019, 2018, 2016, 2013, 2011, 2009
    )
}
