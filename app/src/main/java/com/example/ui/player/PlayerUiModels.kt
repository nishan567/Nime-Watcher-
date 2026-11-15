package com.example.ui.player

import com.example.domain.model.AnimeEpisode
import com.example.domain.model.AnimeSeasonGroup
import com.example.domain.stream.AuthorizedStreamSource
import com.example.domain.stream.StreamResolutionResult

data class VideoQualityOption(
    val id: String,
    val label: String,
    val height: Int? = null,
    val groupIndex: Int? = null,
    val trackIndex: Int? = null
)

data class AudioTrackOption(
    val id: String,
    val label: String,
    val language: String?,
    val groupIndex: Int,
    val trackIndex: Int
)

data class SubtitleOption(
    val id: String,
    val label: String,
    val language: String?,
    val isOff: Boolean,
    val groupIndex: Int? = null,
    val trackIndex: Int? = null
)

data class PlayerUiState(
    val isLoading: Boolean = true,
    val animeId: String = "",
    val animeTitle: String = "",
    val posterUrl: String? = null,
    val seasons: List<AnimeSeasonGroup> = emptyList(),
    val selectedSeasonId: String = "",
    val episodes: List<AnimeEpisode> = emptyList(),
    val currentEpisodeNumber: Int = 1,
    val resolutionResult: StreamResolutionResult? = null,
    val activeStream: AuthorizedStreamSource? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val playbackError: String? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val qualityOptions: List<VideoQualityOption> = listOf(VideoQualityOption("auto", "Auto")),
    val selectedQualityId: String = "auto",
    val audioOptions: List<AudioTrackOption> = emptyList(),
    val selectedAudioId: String? = null,
    val subtitleOptions: List<SubtitleOption> = listOf(SubtitleOption("off", "Off", null, isOff = true)),
    val selectedSubtitleId: String = "off",
    val showSkipIntro: Boolean = false,
    val showSkipOutro: Boolean = false,
    val autoNextCountdownSec: Int? = null,
    val hasNextEpisode: Boolean = false,
    val hasPreviousEpisode: Boolean = false,
    val isPlaybackSessionActive: Boolean = false,
    val inBackgroundAudioMode: Boolean = false,
    val useShowcaseStream: Boolean = false
)
