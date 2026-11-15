package com.example.data.catalog

import com.example.domain.catalog.AnimeCatalogProvider
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Central Anime Catalog & Stream Provider Configuration for NIME WATCHER.
 *
 * HOW TO CONNECT ADDITIONAL AUTHORIZED PROVIDERS LATER:
 * 1. Metadata Providers: Implement [AnimeCatalogProvider] in `data/catalog/`
 *    and register its `providerId` in [CatalogProviderConfig.createActiveProvider].
 * 2. Playable Video Stream Providers (Stage 4): Keep stream resolution separate
 *    from metadata lookup. Authorized HLS (.m3u8), DASH (.mpd), or licensed MP4
 *    URLs must be supplied by a dedicated `AuthorizedStreamProvider` adapter
 *    connected in Stage 4, never scraped from unauthorized sites.
 */
object CatalogProviderConfig {
    const val ACTIVE_PROVIDER_ID = "anilist"
    const val ANILIST_GRAPHQL_ENDPOINT = "https://graphql.anilist.co"
    const val DEFAULT_PAGE_SIZE = 20

    val sharedJson: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    fun createOkHttpClient(enableDebugLogs: Boolean = false): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)

        if (enableDebugLogs) {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(logger)
        }
        return builder.build()
    }

    fun createActiveProvider(enableDebugLogs: Boolean = false): AnimeCatalogProvider {
        return when (ACTIVE_PROVIDER_ID) {
            AniListCatalogProvider.PROVIDER_ID -> AniListCatalogProvider(
                httpClient = createOkHttpClient(enableDebugLogs),
                json = sharedJson
            )
            else -> AniListCatalogProvider(
                httpClient = createOkHttpClient(enableDebugLogs),
                json = sharedJson
            )
        }
    }
}
