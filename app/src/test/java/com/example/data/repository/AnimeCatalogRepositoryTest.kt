package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NimeCatalogDatabase
import com.example.domain.catalog.AnimeCatalogProvider
import com.example.domain.model.AnimeDetails
import com.example.domain.model.AnimeEpisode
import com.example.domain.model.AnimeSearchFilter
import com.example.domain.model.AnimeSeasonGroup
import com.example.domain.model.AnimeSeries
import com.example.domain.model.CatalogPageResult
import com.example.domain.model.CatalogSectionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AnimeCatalogRepositoryTest {

    private lateinit var database: NimeCatalogDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NimeCatalogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadHomeCatalog_cachesRemoteSections_andFallsBackWhenOffline() = runBlocking {
        var failNetwork = false
        val sampleAnime = AnimeSeries(
            id = "anilist:101",
            providerId = "anilist",
            externalId = "101",
            title = "Blade of Obsidian",
            genres = listOf("Action", "Fantasy"),
            releaseYear = 2025,
            episodeCount = 12,
            verifiedRating = 8.7
        )

        val fakeProvider = object : AnimeCatalogProvider {
            override val providerId = "test_provider"
            override val displayName = "Test Catalog"
            override val providesPlayableStreams = false

            override suspend fun getHomeSections(): Map<CatalogSectionType, List<AnimeSeries>> {
                if (failNetwork) throw IOException("Offline")
                return mapOf(CatalogSectionType.TRENDING to listOf(sampleAnime))
            }

            override suspend fun getSectionPage(section: CatalogSectionType, page: Int) =
                CatalogPageResult(listOf(sampleAnime), page, false, providerName = displayName)

            override suspend fun searchAnime(filter: AnimeSearchFilter) =
                CatalogPageResult(listOf(sampleAnime), 1, false, providerName = displayName)

            override suspend fun getSuggestions(query: String) = listOf("Blade of Obsidian")

            override suspend fun getAnimeDetails(animeId: String) = AnimeDetails(
                series = sampleAnime,
                seasons = listOf(AnimeSeasonGroup("anilist:101", "anilist:101", 1, "Season 1", sampleAnime.format, 12)),
                episodesBySeason = emptyMap(),
                relatedAnime = emptyList(),
                providerDisplayName = displayName,
                streamConfigurationNotice = "Metadata only"
            )

            override suspend fun getEpisodesForSeason(animeId: String, season: AnimeSeasonGroup) = emptyList<AnimeEpisode>()
        }

        val repo = AnimeCatalogRepository(fakeProvider, database.catalogDao())

        // Online fetch populates Room cache
        val onlineRes = repo.loadHomeCatalog().getOrThrow()
        assertFalse(onlineRes.fromCache)
        assertEquals(1, onlineRes.sections[CatalogSectionType.TRENDING]?.size)

        // Simulated offline fallback serves cached Room data
        failNetwork = true
        val offlineRes = repo.loadHomeCatalog().getOrThrow()
        assertTrue(offlineRes.fromCache)
        assertEquals("Blade of Obsidian", offlineRes.sections[CatalogSectionType.TRENDING]?.first()?.title)
    }

    @Test
    fun searchHistory_andQuickLists_persistInRoom() = runBlocking {
        val dummyProvider = object : AnimeCatalogProvider {
            override val providerId = "test_provider"
            override val displayName = "Test Catalog"
            override val providesPlayableStreams = false
            override suspend fun getHomeSections() = emptyMap<CatalogSectionType, List<AnimeSeries>>()
            override suspend fun getSectionPage(section: CatalogSectionType, page: Int) =
                CatalogPageResult(emptyList(), page, false)
            override suspend fun searchAnime(filter: AnimeSearchFilter) =
                CatalogPageResult(emptyList(), 1, false)
            override suspend fun getSuggestions(query: String) = emptyList<String>()
            override suspend fun getAnimeDetails(animeId: String): AnimeDetails = throw NotImplementedError()
            override suspend fun getEpisodesForSeason(animeId: String, season: AnimeSeasonGroup) = emptyList<AnimeEpisode>()
        }
        val repo = AnimeCatalogRepository(dummyProvider, database.catalogDao())

        repo.recordSearchQuery("Frieren")
        repo.recordSearchQuery("Solo Leveling")
        assertEquals(listOf("Solo Leveling", "Frieren"), repo.observeSearchHistory().first())

        repo.deleteSearchHistoryItem("Frieren")
        assertEquals(listOf("Solo Leveling"), repo.observeSearchHistory().first())

        repo.clearSearchHistory()
        assertTrue(repo.observeSearchHistory().first().isEmpty())

        val anime = AnimeSeries(
            id = "anilist:202",
            providerId = "anilist",
            externalId = "202",
            title = "Sky Samurai"
        )
        assertTrue(repo.toggleFavorite(anime))
        assertEquals(1, repo.observeFavorites().first().size)
        assertFalse(repo.toggleFavorite(anime))
        assertTrue(repo.observeFavorites().first().isEmpty())

        assertTrue(repo.toggleWatchLater(anime))
        assertEquals(1, repo.observeWatchLater().first().size)
    }

    @Test
    fun playbackProgress_persistsAndResetsNearCompletion() = runBlocking {
        val dummyProvider = object : AnimeCatalogProvider {
            override val providerId = "test_provider"
            override val displayName = "Test Catalog"
            override val providesPlayableStreams = false
            override suspend fun getHomeSections() = emptyMap<CatalogSectionType, List<AnimeSeries>>()
            override suspend fun getSectionPage(section: CatalogSectionType, page: Int) =
                CatalogPageResult(emptyList(), page, false)
            override suspend fun searchAnime(filter: AnimeSearchFilter) =
                CatalogPageResult(emptyList(), 1, false)
            override suspend fun getSuggestions(query: String) = emptyList<String>()
            override suspend fun getAnimeDetails(animeId: String): AnimeDetails = throw NotImplementedError()
            override suspend fun getEpisodesForSeason(animeId: String, season: AnimeSeasonGroup) = emptyList<AnimeEpisode>()
        }
        val repo = AnimeCatalogRepository(dummyProvider, database.catalogDao())

        // Below 1500ms threshold should not persist
        repo.savePlaybackProgress(
            animeId = "anilist:101",
            animeTitle = "Blade of Obsidian",
            posterUrl = null,
            seasonId = "anilist:101",
            episodeNumber = 1,
            episodeTitle = "Episode 1",
            positionMs = 800L,
            durationMs = 100_000L
        )
        assertTrue(repo.observeContinueWatching().first().isEmpty())

        // Normal mid-episode progress persists and resumes from exact position
        repo.savePlaybackProgress(
            animeId = "anilist:101",
            animeTitle = "Blade of Obsidian",
            posterUrl = null,
            seasonId = "anilist:101",
            episodeNumber = 1,
            episodeTitle = "Episode 1",
            positionMs = 45_000L,
            durationMs = 100_000L
        )
        val continueList = repo.observeContinueWatching().first()
        assertEquals(1, continueList.size)
        assertEquals(45_000L, repo.getSavedPositionMs("anilist:101", "anilist:101", 1))

        // Near-completion (>=96%) resets resume position to 0L
        repo.savePlaybackProgress(
            animeId = "anilist:101",
            animeTitle = "Blade of Obsidian",
            posterUrl = null,
            seasonId = "anilist:101",
            episodeNumber = 1,
            episodeTitle = "Episode 1",
            positionMs = 97_000L,
            durationMs = 100_000L
        )
        assertEquals(0L, repo.getSavedPositionMs("anilist:101", "anilist:101", 1))
    }

    @Test
    fun authorizedStreamProvider_resolvesShowcaseAndNotConfiguredTransparently() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = com.example.data.stream.StreamProviderConfig.createStreamProvider(context)

        val thirdParty = provider.resolveEpisodeStream(
            animeId = "anilist:999",
            animeTitle = "Sample Anime",
            seasonId = "anilist:999",
            episodeNumber = 1
        )
        assertTrue(thirdParty is com.example.domain.stream.StreamResolutionResult.NotConfigured)
        val notConfigured = thirdParty as com.example.domain.stream.StreamResolutionResult.NotConfigured
        assertTrue(notConfigured.showcaseDemoSource.streamUrl.endsWith(".m3u8"))
        assertEquals(listOf("en", "bn", "ja"), notConfigured.showcaseDemoSource.subtitles.map { it.languageCode })

        val showcase = provider.resolveEpisodeStream(
            animeId = "${com.example.data.stream.StreamProviderConfig.SHOWCASE_ANIME_PREFIX}demo",
            animeTitle = "Showcase Anime",
            seasonId = "${com.example.data.stream.StreamProviderConfig.SHOWCASE_ANIME_PREFIX}demo",
            episodeNumber = 2
        )
        assertTrue(showcase is com.example.domain.stream.StreamResolutionResult.Authorized)
        val authorized = showcase as com.example.domain.stream.StreamResolutionResult.Authorized
        assertTrue(authorized.source.streamUrl.endsWith(".m3u8"))
        assertTrue(authorized.source.isAdaptiveStream)
    }

    @Test
    fun fetchCategoryAnime_handlesTagQueriesAndOfflineCacheFallback() = runBlocking {
        var failNetwork = false
        var lastFilter: AnimeSearchFilter? = null
        val cyberpunkAnime = AnimeSeries(
            id = "anilist:777",
            providerId = "anilist",
            externalId = "777",
            title = "Neon Runners 2099",
            genres = listOf("Action", "Sci-Fi"),
            tags = listOf("Cyberpunk", "Dystopian"),
            releaseYear = 2024,
            verifiedRating = 8.9
        )

        val provider = object : AnimeCatalogProvider {
            override val providerId = "test_provider"
            override val displayName = "Test Catalog"
            override val providesPlayableStreams = false
            override suspend fun getHomeSections() = emptyMap<CatalogSectionType, List<AnimeSeries>>()
            override suspend fun getSectionPage(section: CatalogSectionType, page: Int) =
                CatalogPageResult(emptyList(), page, false)
            override suspend fun searchAnime(filter: AnimeSearchFilter): CatalogPageResult {
                lastFilter = filter
                if (failNetwork) throw IOException("Offline")
                return CatalogPageResult(listOf(cyberpunkAnime), filter.page, false, providerName = displayName)
            }
            override suspend fun getSuggestions(query: String) = emptyList<String>()
            override suspend fun getAnimeDetails(animeId: String): AnimeDetails = throw NotImplementedError()
            override suspend fun getEpisodesForSeason(animeId: String, season: AnimeSeasonGroup) = emptyList<AnimeEpisode>()
        }

        val repo = AnimeCatalogRepository(provider, database.catalogDao())
        val online = repo.fetchCategoryAnime(
            queryValue = "Cyberpunk",
            filterKind = "tag",
            sortBy = com.example.domain.model.AnimeSortOption.TRENDING,
            format = null,
            page = 1
        ).getOrThrow()

        assertEquals("Cyberpunk", lastFilter?.selectedTag)
        assertEquals(null, lastFilter?.selectedGenre)
        assertFalse(online.fromCache)
        assertEquals("Neon Runners 2099", online.items.first().title)

        // Offline fallback serves cached tag shelf from Room
        failNetwork = true
        val offline = repo.fetchCategoryAnime(
            queryValue = "Cyberpunk",
            filterKind = "tag",
            sortBy = com.example.domain.model.AnimeSortOption.TRENDING,
            format = null,
            page = 1
        ).getOrThrow()

        assertTrue(offline.fromCache)
        assertEquals("Neon Runners 2099", offline.items.first().title)
    }
}

