package com.example.playback

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.network.NetworkMonitor
import com.example.core.preferences.UserPreferencesRepository
import com.example.data.local.NimeCatalogDatabase
import com.example.data.repository.AnimeCatalogRepository
import com.example.data.stream.StreamProviderConfig
import com.example.domain.catalog.AnimeCatalogProvider
import com.example.domain.model.AnimeDetails
import com.example.domain.model.AnimeEpisode
import com.example.domain.model.AnimeSearchFilter
import com.example.domain.model.AnimeSeasonGroup
import com.example.domain.model.CatalogPageResult
import com.example.domain.model.CatalogSectionType
import com.example.navigation.NimeRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PlaybackStage5Test {

    private lateinit var context: Context
    private lateinit var database: NimeCatalogDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, NimeCatalogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun safeAspectRatio_clampsExtremeDimensionsInsideAndroidPipBounds() {
        val standard = PipUtils.safeAspectRatio(1920, 1080)
        assertTrue(standard.toFloat() in 0.42f..2.39f)

        val ultraWide = PipUtils.safeAspectRatio(4000, 500)
        assertTrue(ultraWide.toFloat() <= 2.39f)

        val ultraTall = PipUtils.safeAspectRatio(400, 2000)
        assertTrue(ultraTall.toFloat() >= 0.42f)

        val invalid = PipUtils.safeAspectRatio(0, -10)
        assertEquals(16f / 9f, invalid.toFloat(), 0.01f)
    }

    @Test
    fun userPreferencesRepository_persistsAllStage5PlaybackSettings() = runBlocking {
        val repo = UserPreferencesRepository(context)
        repo.setBackgroundPlaybackEnabled(false)
        repo.setAutoPipEnabled(false)
        repo.setBackgroundAudioEnabled(false)
        repo.setLockScreenControlsEnabled(false)
        repo.setAutoNextEpisodeEnabled(false)

        val updated = repo.preferences.first()
        assertFalse(updated.backgroundPlaybackEnabled)
        assertFalse(updated.autoPipEnabled)
        assertFalse(updated.backgroundAudioEnabled)
        assertFalse(updated.lockScreenControlsEnabled)
        assertFalse(updated.autoNextEpisodeEnabled)

        repo.setBackgroundPlaybackEnabled(true)
        repo.setAutoPipEnabled(true)
        repo.setBackgroundAudioEnabled(true)
        repo.setLockScreenControlsEnabled(true)
        repo.setAutoNextEpisodeEnabled(true)

        val restored = repo.preferences.first()
        assertTrue(restored.backgroundPlaybackEnabled)
        assertTrue(restored.autoPipEnabled)
        assertTrue(restored.backgroundAudioEnabled)
        assertTrue(restored.lockScreenControlsEnabled)
        assertTrue(restored.autoNextEpisodeEnabled)
    }

    @Test
    fun playbackSessionManager_tracksPipPendingRouteAndExplicitStop() {
        val dummyProvider = object : AnimeCatalogProvider {
            override val providerId = "test"
            override val displayName = "Test"
            override val providesPlayableStreams = false
            override suspend fun getHomeSections() = emptyMap<CatalogSectionType, List<com.example.domain.model.AnimeSeries>>()
            override suspend fun getSectionPage(section: CatalogSectionType, page: Int) = CatalogPageResult(emptyList(), page, false)
            override suspend fun searchAnime(filter: AnimeSearchFilter) = CatalogPageResult(emptyList(), 1, false)
            override suspend fun getSuggestions(query: String) = emptyList<String>()
            override suspend fun getAnimeDetails(animeId: String): AnimeDetails = throw NotImplementedError()
            override suspend fun getEpisodesForSeason(animeId: String, season: AnimeSeasonGroup) = emptyList<AnimeEpisode>()
        }

        val manager = PlaybackSessionManager(
            context = context,
            catalogRepository = AnimeCatalogRepository(dummyProvider, database.catalogDao()),
            streamProvider = StreamProviderConfig.createStreamProvider(context),
            preferencesRepository = UserPreferencesRepository(context),
            networkMonitor = NetworkMonitor(context)
        )

        manager.setInPipMode(true)
        assertTrue(manager.isInPipMode.value)
        manager.setInPipMode(false)
        assertFalse(manager.isInPipMode.value)

        val targetRoute = NimeRoute.AnimePlayer(
            animeId = "showcase:demo",
            seasonId = "showcase:demo",
            episodeNumber = 2,
            useShowcaseDemo = true
        )
        manager.requestOpenPlayerRoute(targetRoute)
        assertEquals(targetRoute, manager.pendingPlayerRoute.value)
        manager.consumePendingPlayerRoute()
        assertNull(manager.pendingPlayerRoute.value)

        var stopInvoked = false
        manager.onStopServiceRequested = { stopInvoked = true }
        manager.stopPlaybackAndDismissSession()
        assertTrue(stopInvoked)
        assertFalse(manager.uiState.value.isPlaybackSessionActive)
        assertFalse(manager.uiState.value.inBackgroundAudioMode)
    }
}
