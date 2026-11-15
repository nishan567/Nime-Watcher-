package com.example.ui.player

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.core.preferences.UserPreferences
import com.example.core.preferences.UserPreferencesRepository
import com.example.domain.model.AnimeSeasonGroup
import com.example.playback.PlaybackSessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class AnimePlayerViewModel(
    initialAnimeId: String,
    initialSeasonId: String,
    initialEpisodeNumber: Int,
    forceShowcaseDemo: Boolean,
    private val playbackManager: PlaybackSessionManager,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val player: ExoPlayer
        get() = playbackManager.player

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val uiState: StateFlow<PlayerUiState> = playbackManager.uiState

    init {
        playbackManager.openOrAttachEpisode(
            animeId = initialAnimeId,
            seasonId = initialSeasonId,
            episodeNumber = initialEpisodeNumber,
            forceShowcaseDemo = forceShowcaseDemo
        )
    }

    fun launchShowcaseDemo() = playbackManager.launchShowcaseDemo()

    fun retryPlayback() = playbackManager.retryPlayback()

    fun togglePlayPause() = playbackManager.togglePlayPause()

    fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)

    fun seekBy(deltaMs: Long) = playbackManager.seekBy(deltaMs)

    fun skipIntro() = playbackManager.skipIntro()

    fun skipOutro() = playbackManager.skipOutro()

    fun setPlaybackSpeed(speed: Float) = playbackManager.setPlaybackSpeed(speed)

    fun selectQuality(option: VideoQualityOption) = playbackManager.selectQuality(option)

    fun selectAudioTrack(option: AudioTrackOption) = playbackManager.selectAudioTrack(option)

    fun selectSubtitleOption(option: SubtitleOption) = playbackManager.selectSubtitleOption(option)

    fun setSubtitleFontScale(scale: Float) {
        viewModelScope.launch { preferencesRepository.setSubtitleFontScale(scale) }
    }

    fun setSubtitleHighContrast(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSubtitleHighContrast(enabled) }
    }

    fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setBackgroundPlaybackEnabled(enabled) }
    }

    fun setAutoPipEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoPipEnabled(enabled) }
    }

    fun setBackgroundAudioEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setBackgroundAudioEnabled(enabled) }
    }

    fun setLockScreenControlsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setLockScreenControlsEnabled(enabled) }
    }

    fun setAutoNextEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoNextEpisodeEnabled(enabled) }
        if (!enabled) playbackManager.cancelAutoNextCountdown()
    }

    fun switchEpisode(episodeNumber: Int) = playbackManager.switchEpisode(episodeNumber)

    fun selectSeason(season: AnimeSeasonGroup) = playbackManager.selectSeason(season)

    fun nextEpisode() = playbackManager.nextEpisode()

    fun previousEpisode() = playbackManager.previousEpisode()

    fun cancelAutoNextCountdown() = playbackManager.cancelAutoNextCountdown()

    fun stopPlaybackAndClose() = playbackManager.stopPlaybackAndDismissSession()

    override fun onCleared() {
        playbackManager.persistProgressNow()
        super.onCleared()
    }

    class Factory(
        private val animeId: String,
        private val seasonId: String,
        private val episodeNumber: Int,
        private val forceShowcaseDemo: Boolean,
        private val playbackManager: PlaybackSessionManager,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnimePlayerViewModel(
                initialAnimeId = animeId,
                initialSeasonId = seasonId,
                initialEpisodeNumber = episodeNumber,
                forceShowcaseDemo = forceShowcaseDemo,
                playbackManager = playbackManager,
                preferencesRepository = preferencesRepository
            ) as T
        }
    }
}
