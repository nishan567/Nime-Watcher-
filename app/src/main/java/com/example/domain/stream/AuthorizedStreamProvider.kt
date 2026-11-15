package com.example.domain.stream

/**
 * Subtitle track descriptor using WebVTT or other Media3-supported subtitle formats.
 */
data class SubtitleTrackInfo(
    val uri: String,
    val languageCode: String,
    val label: String,
    val mimeType: String = "text/vtt"
)

/**
 * Authorized playable stream source for Media3 ExoPlayer.
 * Intro/outro timestamps are optional and ONLY populated when verified timestamps exist.
 */
data class AuthorizedStreamSource(
    val streamUrl: String,
    val mimeType: String? = null,
    val providerName: String,
    val licenseNotice: String,
    val isAdaptiveStream: Boolean,
    val subtitles: List<SubtitleTrackInfo> = emptyList(),
    val introStartSec: Long? = null,
    val introEndSec: Long? = null,
    val outroStartSec: Long? = null,
    val outroEndSec: Long? = null
)

sealed interface StreamResolutionResult {
    data class Authorized(
        val source: AuthorizedStreamSource,
        val episodeTitle: String
    ) : StreamResolutionResult

    data class NotConfigured(
        val reason: String,
        val showcaseDemoSource: AuthorizedStreamSource
    ) : StreamResolutionResult
}

/**
 * Interface-based video stream provider abstraction.
 *
 * Strictly separate from [com.example.domain.catalog.AnimeCatalogProvider].
 * Implementations connect ONLY to legally licensed streams, authorized CDNs,
 * or public-domain / Creative Commons content.
 */
interface AuthorizedStreamProvider {
    val providerId: String
    val displayName: String

    suspend fun resolveEpisodeStream(
        animeId: String,
        animeTitle: String,
        seasonId: String,
        episodeNumber: Int
    ): StreamResolutionResult
}
