package com.example.data.stream

import android.content.Context
import android.net.Uri
import androidx.media3.common.MimeTypes
import com.example.domain.stream.AuthorizedStreamProvider
import com.example.domain.stream.AuthorizedStreamSource
import com.example.domain.stream.StreamResolutionResult
import com.example.domain.stream.SubtitleTrackInfo
import java.io.File

/**
 * Central Video Stream Provider Configuration for NIME WATCHER.
 *
 * HOW TO CONNECT YOUR AUTHORIZED VIDEO STREAMING PROVIDER:
 * 1. Register licensed anime episode HLS (.m3u8) or DASH (.mpd) endpoints inside
 *    a custom implementation of [AuthorizedStreamProvider] (e.g., querying your own
 *    authenticated CDN/CMS backend with signed URLs).
 * 2. Swap or add your provider in [createStreamProvider].
 * 3. NEVER scrape unauthorized websites or bypass DRM.
 */
object StreamProviderConfig {
    const val SHOWCASE_ANIME_PREFIX = "showcase:"

    fun createStreamProvider(context: Context): AuthorizedStreamProvider {
        return DefaultAuthorizedStreamProvider(context.applicationContext)
    }
}

/**
 * Resolves registered authorized streams or returns a transparent configuration notice
 * alongside a legally authorized Creative Commons adaptive HLS showcase stream (Blender Foundation)
 * with real WebVTT subtitles (English, বাংলা, 日本語) so every Media3 ExoPlayer control
 * (adaptive quality, WebVTT subtitles, audio tracks, playback speed, intro/outro skip)
 * can be verified without using unauthorized anime streams.
 */
class DefaultAuthorizedStreamProvider(
    private val appContext: Context
) : AuthorizedStreamProvider {

    override val providerId: String = "authorized_registry"
    override val displayName: String = "Nime Watcher Authorized Stream Registry"

    /**
     * Developer extension hook: map `"animeId:episodeNumber"` (or connect an authenticated HTTPS API)
     * to legally licensed HLS/DASH/MP4 stream descriptors.
     */
    private val licensedEpisodeRegistry: Map<String, AuthorizedStreamSource> = emptyMap()

    override suspend fun resolveEpisodeStream(
        animeId: String,
        animeTitle: String,
        seasonId: String,
        episodeNumber: Int
    ): StreamResolutionResult {
        val key = "$animeId:$seasonId:$episodeNumber"
        val registered = licensedEpisodeRegistry[key]
        if (registered != null) {
            return StreamResolutionResult.Authorized(
                source = registered,
                episodeTitle = "$animeTitle • Episode $episodeNumber"
            )
        }

        val showcaseSource = buildOpenLicenseShowcaseStream(episodeNumber)
        if (animeId.startsWith(StreamProviderConfig.SHOWCASE_ANIME_PREFIX)) {
            return StreamResolutionResult.Authorized(
                source = showcaseSource,
                episodeTitle = "$animeTitle • Episode $episodeNumber"
            )
        }

        return StreamResolutionResult.NotConfigured(
            reason = "No licensed video stream provider is registered for \"$animeTitle\" (Episode $episodeNumber). " +
                "AniList provides public catalog metadata only. Connect an authorized HLS/DASH endpoint in StreamProviderConfig, " +
                "or launch the Creative Commons adaptive HLS showcase stream below to test Media3 ExoPlayer.",
            showcaseDemoSource = showcaseSource
        )
    }

    private fun buildOpenLicenseShowcaseStream(episodeNumber: Int): AuthorizedStreamSource {
        val subtitles = ensureLocalShowcaseVttTracks()
        // Alternate open-license Creative Commons adaptive HLS streams per episode number
        // so Next/Previous Episode switching loads distinct real adaptive media items.
        val isEven = episodeNumber % 2 == 0
        val streamUrl = if (isEven) {
            "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
        } else {
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        }
        val titleTag = if (isEven) {
            "Sintel • Blender Foundation (CC-BY 3.0 Adaptive HLS)"
        } else {
            "Big Buck Bunny • Blender Foundation (CC-BY 3.0 Adaptive HLS)"
        }
        return AuthorizedStreamSource(
            streamUrl = streamUrl,
            mimeType = MimeTypes.APPLICATION_M3U8,
            providerName = titleTag,
            licenseNotice = "Creative Commons Open-License HLS Showcase (Multi-Bitrate Adaptive)",
            isAdaptiveStream = true,
            subtitles = subtitles,
            introStartSec = 4L,
            introEndSec = 22L,
            outroStartSec = 520L,
            outroEndSec = 590L
        )
    }

    private fun ensureLocalShowcaseVttTracks(): List<SubtitleTrackInfo> {
        val subsDir = File(appContext.filesDir, "showcase_subs").apply { mkdirs() }
        val trackSpecs = listOf(
            Triple(
                "en",
                "English (WebVTT)",
                """
                WEBVTT

                00:00:01.000 --> 00:00:05.500
                [NIME WATCHER • Media3 Adaptive HLS Showcase]

                00:00:06.000 --> 00:00:12.000
                Multi-bitrate quality, speed, and WebVTT subtitles active.

                00:00:12.500 --> 00:00:22.000
                Opening sequence window — tap "Skip Intro" to jump ahead.

                00:00:23.000 --> 00:00:35.000
                Connect licensed anime streams anytime in StreamProviderConfig.
                """.trimIndent()
            ),
            Triple(
                "bn",
                "বাংলা (WebVTT)",
                """
                WEBVTT

                00:00:01.000 --> 00:00:05.500
                [নাইম ওয়াচার • Media3 অ্যাডাপ্টিভ HLS প্রদর্শনী]

                00:00:06.000 --> 00:00:12.000
                ভিডিও কোয়ালিটি, প্লেব্যাক স্পিড এবং বাংলা সাবটাইটেল সক্রিয়।

                00:00:12.500 --> 00:00:22.000
                ইন্ট্রো অংশ — সামনে যেতে "Skip Intro" চাপুন।

                00:00:23.000 --> 00:00:35.000
                StreamProviderConfig-এ অনুমোদিত অ্যানিমে স্ট্রিম যুক্ত করুন।
                """.trimIndent()
            ),
            Triple(
                "ja",
                "日本語 (WebVTT)",
                """
                WEBVTT

                00:00:01.000 --> 00:00:05.500
                [NIME WATCHER・Media3 HLS テストストリーム]

                00:00:06.000 --> 00:00:12.000
                画質切替・再生速度・WebVTT字幕が有効です。

                00:00:12.500 --> 00:00:22.000
                オープニング区間 —「Skip Intro」でスキップできます。

                00:00:23.000 --> 00:00:35.000
                正規配信プロバイダーは StreamProviderConfig に接続できます。
                """.trimIndent()
            )
        )

        return trackSpecs.map { (lang, label, content) ->
            val vttFile = File(subsDir, "showcase_$lang.vtt")
            if (!vttFile.exists() || vttFile.readText() != content) {
                vttFile.writeText(content)
            }
            SubtitleTrackInfo(
                uri = Uri.fromFile(vttFile).toString(),
                languageCode = lang,
                label = label,
                mimeType = MimeTypes.TEXT_VTT
            )
        }
    }
}
