package com.example.data.catalog

import com.example.domain.catalog.AnimeCatalogProvider
import com.example.domain.model.AnimeDetails
import com.example.domain.model.AnimeEpisode
import com.example.domain.model.AnimeFormat
import com.example.domain.model.AnimeSearchFilter
import com.example.domain.model.AnimeSeasonGroup
import com.example.domain.model.AnimeSeries
import com.example.domain.model.AnimeSortOption
import com.example.domain.model.AnimeStatus
import com.example.domain.model.AudioFilter
import com.example.domain.model.CatalogPageResult
import com.example.domain.model.CatalogSectionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Public HTTPS Anime Metadata Provider powered by AniList GraphQL v2 (free, no API key).
 * Strictly provides metadata, verified scores, genres, and season relations.
 * Playable streams remain separated and unpopulated (`authorizedStreamUrl = null`)
 * until an authorized video stream adapter is connected in Stage 4.
 */
class AniListCatalogProvider(
    private val httpClient: OkHttpClient,
    private val json: Json,
    private val endpointUrl: String = CatalogProviderConfig.ANILIST_GRAPHQL_ENDPOINT
) : AnimeCatalogProvider {

    override val providerId: String = PROVIDER_ID
    override val displayName: String = "AniList Public Metadata Catalog"
    override val providesPlayableStreams: Boolean = false

    override suspend fun getHomeSections(): Map<CatalogSectionType, List<AnimeSeries>> = coroutineScope {
        val primaryDeferred = async { executeGraphQl(HOME_PRIMARY_QUERY, buildJsonObject {}) }
        val genresDeferred = async { executeGraphQl(HOME_GENRES_QUERY, buildJsonObject {}) }

        val primaryData = primaryDeferred.await()["data"]?.jsonObject ?: JsonObject(emptyMap())
        val genresData = genresDeferred.await()["data"]?.jsonObject ?: JsonObject(emptyMap())

        buildMap {
            putSection(primaryData, "trending", CatalogSectionType.TRENDING)
            putSection(primaryData, "popular", CatalogSectionType.POPULAR)
            putSection(primaryData, "recentAdded", CatalogSectionType.RECENTLY_ADDED)
            putSection(primaryData, "recentUpdated", CatalogSectionType.RECENTLY_UPDATED)
            putSection(primaryData, "mostWatched", CatalogSectionType.MOST_WATCHED)
            putSection(primaryData, "newEpisodes", CatalogSectionType.NEW_EPISODES)
            putSection(primaryData, "recommended", CatalogSectionType.RECOMMENDED)

            putSection(genresData, "action", CatalogSectionType.ACTION)
            putSection(genresData, "adventure", CatalogSectionType.ADVENTURE)
            putSection(genresData, "romance", CatalogSectionType.ROMANCE)
            putSection(genresData, "comedy", CatalogSectionType.COMEDY)
            putSection(genresData, "fantasy", CatalogSectionType.FANTASY)
            putSection(genresData, "isekai", CatalogSectionType.ISEKAI)
            putSection(genresData, "drama", CatalogSectionType.DRAMA)
            putSection(genresData, "mystery", CatalogSectionType.MYSTERY)
            putSection(genresData, "supernatural", CatalogSectionType.SUPERNATURAL)
        }
    }

    override suspend fun getSectionPage(section: CatalogSectionType, page: Int): CatalogPageResult {
        val filter = when (section) {
            CatalogSectionType.TRENDING -> AnimeSearchFilter(sortBy = AnimeSortOption.TRENDING, page = page)
            CatalogSectionType.POPULAR -> AnimeSearchFilter(sortBy = AnimeSortOption.POPULARITY, page = page)
            CatalogSectionType.RECENTLY_ADDED -> AnimeSearchFilter(sortBy = AnimeSortOption.YEAR_DESC, page = page)
            CatalogSectionType.RECENTLY_UPDATED -> AnimeSearchFilter(sortBy = AnimeSortOption.TRENDING, page = page)
            CatalogSectionType.MOST_WATCHED -> AnimeSearchFilter(sortBy = AnimeSortOption.POPULARITY, page = page)
            CatalogSectionType.NEW_EPISODES -> AnimeSearchFilter(format = AnimeFormat.TV, sortBy = AnimeSortOption.TRENDING, page = page)
            CatalogSectionType.RECOMMENDED -> AnimeSearchFilter(sortBy = AnimeSortOption.SCORE, page = page)
            CatalogSectionType.ACTION -> AnimeSearchFilter(selectedGenre = "Action", sortBy = AnimeSortOption.POPULARITY, page = page)
            CatalogSectionType.ADVENTURE -> AnimeSearchFilter(selectedGenre = "Adventure", sortBy = AnimeSortOption.POPULARITY, page = page)
            CatalogSectionType.ROMANCE -> AnimeSearchFilter(selectedGenre = "Romance", sortBy = AnimeSortOption.POPULARITY, page = page)
            CatalogSectionType.COMEDY -> AnimeSearchFilter(selectedGenre = "Comedy", sortBy = AnimeSortOption.POPULARITY, page = page)
            CatalogSectionType.FANTASY -> AnimeSearchFilter(selectedGenre = "Fantasy", sortBy = AnimeSortOption.POPULARITY, page = page)
            CatalogSectionType.ISEKAI -> AnimeSearchFilter(selectedGenre = "Isekai", sortBy = AnimeSortOption.POPULARITY, page = page)
            CatalogSectionType.DRAMA -> AnimeSearchFilter(selectedGenre = "Drama", sortBy = AnimeSortOption.POPULARITY, page = page)
            CatalogSectionType.MYSTERY -> AnimeSearchFilter(selectedGenre = "Mystery", sortBy = AnimeSortOption.POPULARITY, page = page)
            CatalogSectionType.SUPERNATURAL -> AnimeSearchFilter(selectedGenre = "Supernatural", sortBy = AnimeSortOption.POPULARITY, page = page)
        }
        return searchAnime(filter)
    }

    override suspend fun searchAnime(filter: AnimeSearchFilter): CatalogPageResult {
        val variables = buildJsonObject {
            put("page", filter.page)
            put("perPage", CatalogProviderConfig.DEFAULT_PAGE_SIZE)
            if (filter.query.isNotBlank()) {
                put("search", filter.query.trim())
            }
            val explicitTag = filter.selectedTag?.takeIf { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
            val genre = filter.selectedGenre?.takeIf { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
            if (explicitTag != null) {
                putJsonArray("tagIn") { add(JsonPrimitive(explicitTag)) }
            }
            if (genre != null) {
                if (genre.equals("Isekai", ignoreCase = true)) {
                    putJsonArray("tagIn") { add(JsonPrimitive("Isekai")) }
                } else {
                    putJsonArray("genreIn") { add(JsonPrimitive(genre)) }
                }
            }
            filter.releaseYear?.let { put("seasonYear", it) }
            filter.season?.let { put("season", it.apiValue) }
            filter.format?.let { fmt ->
                if (fmt != AnimeFormat.UNKNOWN) put("format", fmt.name)
            }
            val sortKey = if (filter.query.isNotBlank() && filter.sortBy == AnimeSortOption.TRENDING) {
                "SEARCH_MATCH"
            } else {
                filter.sortBy.apiSort
            }
            putJsonArray("sort") { add(JsonPrimitive(sortKey)) }
        }

        val response = executeGraphQl(SEARCH_QUERY, variables)
        val pageObj = response["data"]?.jsonObject?.get("Page")?.jsonObject
        val pageInfo = pageObj?.get("pageInfo")?.jsonObject
        val rawItems = pageObj?.get("media")?.jsonArray ?: JsonArray(emptyList())

        var mapped = rawItems.mapNotNull { parseMediaToSeries(it.jsonObject) }
        if (filter.audioFilter == AudioFilter.DUB) {
            mapped = mapped.filter { it.audioLanguages.any { lang -> lang.equals("English", ignoreCase = true) } }
        } else if (filter.audioFilter == AudioFilter.SUB) {
            mapped = mapped.filter { it.subOrDubLabel?.contains("Sub", ignoreCase = true) == true }
        }

        return CatalogPageResult(
            items = mapped,
            currentPage = pageInfo?.get("currentPage")?.jsonPrimitive?.intOrNull ?: filter.page,
            hasNextPage = pageInfo?.get("hasNextPage")?.jsonPrimitive?.booleanOrNull ?: false,
            fromCache = false,
            providerName = displayName
        )
    }

    override suspend fun getSuggestions(query: String): List<String> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()
        val variables = buildJsonObject {
            put("search", trimmed)
        }
        val response = executeGraphQl(SUGGESTIONS_QUERY, variables)
        val mediaArray = response["data"]?.jsonObject
            ?.get("Page")?.jsonObject
            ?.get("media")?.jsonArray ?: return emptyList()
        return mediaArray.mapNotNull { elem ->
            val titleObj = elem.jsonObject["title"]?.jsonObject ?: return@mapNotNull null
            titleObj.stringOrNull("english")
                ?: titleObj.stringOrNull("romaji")
                ?: titleObj.stringOrNull("native")
        }.distinct()
    }

    override suspend fun getAnimeDetails(animeId: String): AnimeDetails {
        val numericId = animeId.removePrefix("$PROVIDER_ID:").toIntOrNull()
            ?: throw IllegalArgumentException("Invalid AniList anime ID: $animeId")

        val variables = buildJsonObject { put("id", numericId) }
        val response = executeGraphQl(DETAILS_QUERY, variables)
        val media = response["data"]?.jsonObject?.get("Media")?.jsonObject
            ?: throw IOException("Anime details unavailable for ID $numericId")

        val mainSeries = parseMediaToSeries(media)
            ?: throw IOException("Unable to parse anime details for ID $numericId")

        val seasons = buildSeasonGroups(mainSeries, media)
        val initialSeason = seasons.first()
        val episodes = buildMetadataEpisodes(mainSeries.id, initialSeason)
        val related = extractRelatedAnime(media)

        return AnimeDetails(
            series = mainSeries,
            seasons = seasons,
            episodesBySeason = mapOf(initialSeason.seasonId to episodes),
            relatedAnime = related,
            providerDisplayName = displayName,
            streamConfigurationNotice = "Catalog metadata is served via AniList GraphQL. Authorized HLS/DASH stream sources attach in Stage 4."
        )
    }

    override suspend fun getEpisodesForSeason(
        animeId: String,
        season: AnimeSeasonGroup
    ): List<AnimeEpisode> {
        return buildMetadataEpisodes(animeId, season)
    }

    private fun buildSeasonGroups(
        mainSeries: AnimeSeries,
        media: JsonObject
    ): List<AnimeSeasonGroup> {
        val groups = mutableListOf<AnimeSeasonGroup>()
        groups.add(
            AnimeSeasonGroup(
                seasonId = mainSeries.id,
                animeId = mainSeries.id,
                seasonNumber = 1,
                label = when (mainSeries.format) {
                    AnimeFormat.MOVIE -> "Movie"
                    AnimeFormat.OVA -> "OVA"
                    AnimeFormat.ONA -> "ONA"
                    AnimeFormat.SPECIAL -> "Special"
                    else -> "Season 1 (${mainSeries.format.displayLabel})"
                },
                format = mainSeries.format,
                episodeCount = mainSeries.episodeCount,
                releaseYear = mainSeries.releaseYear,
                coverUrl = mainSeries.posterUrl
            )
        )

        val edges = media["relations"]?.jsonObject?.get("edges")?.jsonArray ?: JsonArray(emptyList())
        var seasonCounter = 2
        edges.forEach { edgeEl ->
            val edge = edgeEl.jsonObject
            val relationType = edge.stringOrNull("relationType") ?: return@forEach
            val node = edge["node"]?.jsonObject ?: return@forEach
            if (node.stringOrNull("type") != "ANIME") return@forEach
            if (node["isAdult"]?.jsonPrimitive?.booleanOrNull == true) return@forEach

            val relSeries = parseMediaToSeries(node) ?: return@forEach
            val label = when {
                relationType == "SEQUEL" && relSeries.format == AnimeFormat.TV -> "Season ${seasonCounter++} • Sequel"
                relationType == "PREQUEL" && relSeries.format == AnimeFormat.TV -> "Prequel Series"
                relSeries.format == AnimeFormat.MOVIE -> "Movie • ${relSeries.title}"
                relSeries.format == AnimeFormat.OVA -> "OVA • ${relSeries.title}"
                relSeries.format == AnimeFormat.ONA -> "ONA • ${relSeries.title}"
                relSeries.format == AnimeFormat.SPECIAL -> "Special • ${relSeries.title}"
                relationType in listOf("SEQUEL", "SIDE_STORY", "SPIN_OFF") -> "${relSeries.format.displayLabel} • ${relSeries.title}"
                else -> return@forEach
            }
            if (groups.none { it.seasonId == relSeries.id }) {
                groups.add(
                    AnimeSeasonGroup(
                        seasonId = relSeries.id,
                        animeId = relSeries.id,
                        seasonNumber = groups.size + 1,
                        label = label,
                        format = relSeries.format,
                        episodeCount = relSeries.episodeCount,
                        releaseYear = relSeries.releaseYear,
                        coverUrl = relSeries.posterUrl
                    )
                )
            }
        }
        return groups
    }

    private fun buildMetadataEpisodes(
        animeId: String,
        season: AnimeSeasonGroup
    ): List<AnimeEpisode> {
        val count = season.episodeCount ?: return emptyList()
        if (count <= 0) return emptyList()
        val cappedCount = count.coerceAtMost(200)
        return (1..cappedCount).map { epNum ->
            AnimeEpisode(
                episodeId = "${season.seasonId}_ep_$epNum",
                animeId = animeId,
                seasonId = season.seasonId,
                episodeNumber = epNum,
                title = if (season.format == AnimeFormat.MOVIE && count == 1) {
                    "Full Feature (${season.label})"
                } else {
                    "Episode $epNum"
                },
                thumbnailUrl = season.coverUrl,
                durationMinutes = null,
                authorizedStreamUrl = null,
                streamProviderName = null
            )
        }
    }

    private fun extractRelatedAnime(media: JsonObject): List<AnimeSeries> {
        val out = mutableListOf<AnimeSeries>()
        val relEdges = media["relations"]?.jsonObject?.get("edges")?.jsonArray ?: JsonArray(emptyList())
        relEdges.forEach { el ->
            val node = el.jsonObject["node"]?.jsonObject ?: return@forEach
            if (node.stringOrNull("type") == "ANIME" && node["isAdult"]?.jsonPrimitive?.booleanOrNull != true) {
                parseMediaToSeries(node)?.let { out.add(it) }
            }
        }
        val recNodes = media["recommendations"]?.jsonObject?.get("nodes")?.jsonArray ?: JsonArray(emptyList())
        recNodes.forEach { el ->
            val recMedia = el.jsonObject["mediaRecommendation"]?.jsonObject ?: return@forEach
            if (recMedia["isAdult"]?.jsonPrimitive?.booleanOrNull != true) {
                parseMediaToSeries(recMedia)?.let { out.add(it) }
            }
        }
        return out.distinctBy { it.id }.take(14)
    }

    private fun MutableMap<CatalogSectionType, List<AnimeSeries>>.putSection(
        rootData: JsonObject,
        alias: String,
        section: CatalogSectionType
    ) {
        val mediaArray = rootData[alias]?.jsonObject?.get("media")?.jsonArray ?: return
        val items = mediaArray.mapNotNull { parseMediaToSeries(it.jsonObject) }
        if (items.isNotEmpty()) {
            put(section, items)
        }
    }

    private fun parseMediaToSeries(media: JsonObject): AnimeSeries? {
        val numericId = media["id"]?.jsonPrimitive?.intOrNull ?: return null
        if (media["isAdult"]?.jsonPrimitive?.booleanOrNull == true) return null

        val titleObj = media["title"]?.jsonObject
        val romaji = titleObj?.stringOrNull("romaji")
        val english = titleObj?.stringOrNull("english")
        val native = titleObj?.stringOrNull("native")
        val primaryTitle = english ?: romaji ?: native ?: "Untitled Anime"

        val synonyms = media["synonyms"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.content.takeIf { s -> s.isNotBlank() } }
            ?: emptyList()
        val altTitles = listOfNotNull(romaji, native, *synonyms.toTypedArray())
            .filter { it.isNotBlank() && !it.equals(primaryTitle, ignoreCase = true) }
            .distinct()

        val coverObj = media["coverImage"]?.jsonObject
        val posterUrl = coverObj?.stringOrNull("extraLarge") ?: coverObj?.stringOrNull("large")
        val bannerUrl = media.stringOrNull("bannerImage") ?: posterUrl

        val genres = media["genres"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.content.takeIf { g -> g.isNotBlank() } }
            ?: emptyList()
        val tags = media["tags"]?.jsonArray
            ?.mapNotNull { it.jsonObject.stringOrNull("name") }
            ?: emptyList()
        val mergedGenres = (genres + tags.filter { it.equals("Isekai", ignoreCase = true) }).distinct()

        val seasonRaw = media.stringOrNull("season")?.lowercase()?.replaceFirstChar { it.uppercase() }
        val year = media["seasonYear"]?.jsonPrimitive?.intOrNull
        val seasonLabel = listOfNotNull(seasonRaw, year?.toString()).joinToString(" ").ifBlank { null }

        val scoreRaw = media["averageScore"]?.jsonPrimitive?.intOrNull
        val verifiedRating = scoreRaw?.let { it / 10.0 }
        val popularity = media["popularity"]?.jsonPrimitive?.intOrNull
        val episodes = media["episodes"]?.jsonPrimitive?.intOrNull

        val cleanDescription = stripHtml(media.stringOrNull("description").orEmpty())
            .ifBlank { "No synopsis available from catalog provider yet." }

        return AnimeSeries(
            id = "$PROVIDER_ID:$numericId",
            providerId = PROVIDER_ID,
            externalId = numericId.toString(),
            title = primaryTitle,
            englishTitle = english,
            nativeTitle = native,
            alternativeTitles = altTitles,
            description = cleanDescription,
            posterUrl = posterUrl,
            bannerUrl = bannerUrl,
            genres = mergedGenres,
            tags = tags.distinct().take(8),
            releaseYear = year,
            seasonLabel = seasonLabel,
            status = AnimeStatus.fromApi(media.stringOrNull("status")),
            format = AnimeFormat.fromApi(media.stringOrNull("format")),
            episodeCount = episodes,
            verifiedRating = verifiedRating,
            popularity = popularity,
            audioLanguages = listOf("Japanese"),
            subOrDubLabel = "Sub",
            isAuthorizedStreamReady = false
        )
    }

    private suspend fun executeGraphQl(query: String, variables: JsonObject): JsonObject =
        withContext(Dispatchers.IO) {
            val payload = buildJsonObject {
                put("query", query)
                put("variables", variables)
            }
            val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(endpointUrl)
                .post(body)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("Catalog API error (${response.code}): ${response.message}")
                }
                if (raw.isBlank()) {
                    throw IOException("Empty response from catalog API")
                }
                json.parseToJsonElement(raw).jsonObject
            }
        }

    private fun JsonObject.stringOrNull(key: String): String? {
        val el: JsonElement = this[key] ?: return null
        if (el !is JsonPrimitive) return null
        return if (el.isString) el.content.takeIf { it.isNotBlank() } else null
    }

    private fun stripHtml(input: String): String {
        return input
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    companion object {
        const val PROVIDER_ID = "anilist"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private const val MEDIA_FIELDS = """
            id
            type
            isAdult
            title { romaji english native }
            synonyms
            description(asHtml: false)
            coverImage { extraLarge large }
            bannerImage
            genres
            tags { name }
            seasonYear
            season
            status
            format
            episodes
            averageScore
            popularity
        """

        private val HOME_PRIMARY_QUERY = """
            query HomePrimary {
              trending: Page(page: 1, perPage: 10) {
                media(type: ANIME, isAdult: false, sort: TRENDING_DESC) { $MEDIA_FIELDS }
              }
              popular: Page(page: 1, perPage: 10) {
                media(type: ANIME, isAdult: false, sort: POPULARITY_DESC) { $MEDIA_FIELDS }
              }
              recentAdded: Page(page: 1, perPage: 10) {
                media(type: ANIME, isAdult: false, status_in: [RELEASING, FINISHED], sort: START_DATE_DESC) { $MEDIA_FIELDS }
              }
              recentUpdated: Page(page: 1, perPage: 10) {
                media(type: ANIME, isAdult: false, status: RELEASING, sort: UPDATED_AT_DESC) { $MEDIA_FIELDS }
              }
              mostWatched: Page(page: 1, perPage: 10) {
                media(type: ANIME, isAdult: false, sort: FAVOURITES_DESC) { $MEDIA_FIELDS }
              }
              newEpisodes: Page(page: 1, perPage: 10) {
                media(type: ANIME, isAdult: false, status: RELEASING, format_in: [TV, ONA], sort: TRENDING_DESC) { $MEDIA_FIELDS }
              }
              recommended: Page(page: 1, perPage: 10) {
                media(type: ANIME, isAdult: false, averageScore_greater: 78, sort: SCORE_DESC) { $MEDIA_FIELDS }
              }
            }
        """.trimIndent()

        private val HOME_GENRES_QUERY = """
            query HomeGenres {
              action: Page(page: 1, perPage: 10) {
                media(type: ANIME, isAdult: false, genre_in: ["Action"], sort: POPULARITY_DESC) { $MEDIA_FIELDS }
              }
              adventure: Page(page: 1, perPage: 10) {
                media(type: ANIME, isAdult: false, genre_in: ["Adventure"], sort: POPULARITY_DESC) { $MEDIA_FIELDS }
              }
              romance: Page(page: 1, perPage: 10) {
                media(type: ANIME, isAdult: false, genre_in: ["Romance"], sort: POPULARITY_DESC) { $MEDIA_FIELDS }
              }
              comedy: Page(page: 1, perPage: 10) {
                media(type: ANIME, isAdult: false, genre_in: ["Comedy"], sort: POPULARITY_DESC) { $MEDIA_FIELDS }
              }
              fantasy: Page(page: 1, perPage: 10) {
                media(type: ANIME, isAdult: false, genre_in: ["Fantasy"], sort: POPULARITY_DESC) { $MEDIA_FIELDS }
              }
              isekai: Page(page: 1, perPage: 10) {
                media(type: ANIME, isAdult: false, tag_in: ["Isekai"], sort: POPULARITY_DESC) { $MEDIA_FIELDS }
              }
              drama: Page(page: 1, perPage: 10) {
                media(type: ANIME, isAdult: false, genre_in: ["Drama"], sort: POPULARITY_DESC) { $MEDIA_FIELDS }
              }
              mystery: Page(page: 1, perPage: 10) {
                media(type: ANIME, isAdult: false, genre_in: ["Mystery"], sort: POPULARITY_DESC) { $MEDIA_FIELDS }
              }
              supernatural: Page(page: 1, perPage: 10) {
                media(type: ANIME, isAdult: false, genre_in: ["Supernatural"], sort: POPULARITY_DESC) { $MEDIA_FIELDS }
              }
            }
        """.trimIndent()

        private val SEARCH_QUERY = """
            query SearchAnime(
              ${'$'}page: Int,
              ${'$'}perPage: Int,
              ${'$'}search: String,
              ${'$'}genreIn: [String],
              ${'$'}tagIn: [String],
              ${'$'}seasonYear: Int,
              ${'$'}season: MediaSeason,
              ${'$'}format: MediaFormat,
              ${'$'}sort: [MediaSort]
            ) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                pageInfo { currentPage hasNextPage }
                media(
                  type: ANIME,
                  isAdult: false,
                  search: ${'$'}search,
                  genre_in: ${'$'}genreIn,
                  tag_in: ${'$'}tagIn,
                  seasonYear: ${'$'}seasonYear,
                  season: ${'$'}season,
                  format: ${'$'}format,
                  sort: ${'$'}sort
                ) {
                  $MEDIA_FIELDS
                }
              }
            }
        """.trimIndent()

        private val SUGGESTIONS_QUERY = """
            query SuggestAnime(${'$'}search: String) {
              Page(page: 1, perPage: 8) {
                media(type: ANIME, isAdult: false, search: ${'$'}search, sort: SEARCH_MATCH) {
                  id
                  title { romaji english native }
                }
              }
            }
        """.trimIndent()

        private val DETAILS_QUERY = """
            query AnimeDetails(${'$'}id: Int) {
              Media(id: ${'$'}id, type: ANIME, isAdult: false) {
                $MEDIA_FIELDS
                relations {
                  edges {
                    relationType
                    node {
                      $MEDIA_FIELDS
                    }
                  }
                }
                recommendations(page: 1, perPage: 8, sort: RATING_DESC) {
                  nodes {
                    mediaRecommendation {
                      $MEDIA_FIELDS
                    }
                  }
                }
              }
            }
        """.trimIndent()
    }
}
