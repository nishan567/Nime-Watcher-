package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.core.network.NetworkMonitor
import com.example.core.preferences.UserPreferencesRepository
import com.example.data.repository.AnimeCatalogRepository
import com.example.domain.model.AnimeEpisode
import com.example.domain.model.AnimeSeasonGroup
import com.example.domain.stream.AuthorizedStreamProvider
import com.example.domain.stream.AuthorizedStreamSource
import com.example.domain.stream.StreamResolutionResult
import com.example.navigation.NimeRoute
import com.example.ui.player.AudioTrackOption
import com.example.ui.player.PlayerUiState
import com.example.ui.player.SubtitleOption
import com.example.ui.player.VideoQualityOption
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Application-scoped Media3 playback coordinator that keeps the ExoPlayer instance
 * and episode queue independent of MainActivity's lifecycle, enabling seamless PiP,
 * background audio, lock-screen media controls, and MediaSessionService notifications.
 */
@OptIn(UnstableApi::class)
class PlaybackSessionManager(
    context: Context,
    private val catalogRepository: AnimeCatalogRepository,
    private val streamProvider: AuthorizedStreamProvider,
    private val preferencesRepository: UserPreferencesRepository,
    private val networkMonitor: NetworkMonitor
) {
    private val appContext = context.applicationContext
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val trackSelector: DefaultTrackSelector by lazy { DefaultTrackSelector(appContext) }

    @Volatile
    var isPlayerInitialized: Boolean = false
        private set

    val player: ExoPlayer by lazy {
        isPlayerInitialized = true
        ExoPlayer.Builder(appContext)
            .setTrackSelector(trackSelector)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .also { exo ->
                exo.addListener(playerListener)
                startProgressTicker()
            }
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _isInPipMode = MutableStateFlow(false)
    val isInPipMode: StateFlow<Boolean> = _isInPipMode.asStateFlow()

    private val _pendingPlayerRoute = MutableStateFlow<NimeRoute.AnimePlayer?>(null)
    val pendingPlayerRoute: StateFlow<NimeRoute.AnimePlayer?> = _pendingPlayerRoute.asStateFlow()

    private var cachedEpisodesBySeason: MutableMap<String, List<AnimeEpisode>> = mutableMapOf()
    private var currentLoadedAnimeId: String? = null
    private var currentLoadedSeasonId: String? = null
    private var progressTickerJob: Job? = null
    private var autoNextJob: Job? = null
    private var lastSavedAtMs: Long = 0L
    private var useShowcaseStream: Boolean = false
    private var recoverOnNetworkRestore: Boolean = false
    private var wasPreviouslyOnline: Boolean = true
    private var controllerFuture: ListenableFuture<MediaController>? = null

    var onStopServiceRequested: (() -> Unit)? = null
    var onSessionStateChanged: (() -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(
                isPlaying = isPlaying,
                isPlaybackSessionActive = _uiState.value.activeStream != null &&
                    (isPlaying || player.playbackState != Player.STATE_IDLE)
            )
            if (!isPlaying) {
                persistProgressNow()
            }
            onSessionStateChanged?.invoke()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val buffering = playbackState == Player.STATE_BUFFERING
            _uiState.value = _uiState.value.copy(
                isBuffering = buffering,
                durationMs = player.duration.coerceAtLeast(0L),
                isPlaybackSessionActive = _uiState.value.activeStream != null &&
                    playbackState != Player.STATE_IDLE
            )
            if (playbackState == Player.STATE_ENDED) {
                persistProgressNow()
                handlePlaybackEnded()
            }
            onSessionStateChanged?.invoke()
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateAvailableTrackOptions(tracks)
        }

        override fun onPlayerError(error: PlaybackException) {
            val isNetworkError = error.errorCode in NETWORK_ERROR_CODES || !wasPreviouslyOnline
            recoverOnNetworkRestore = isNetworkError
            val friendlyMessage = if (isNetworkError) {
                "Network connection interrupted. Playback will resume automatically when back online, or tap Retry."
            } else {
                error.localizedMessage ?: "Stream playback error (${error.errorCodeName})."
            }
            _uiState.value = _uiState.value.copy(
                isBuffering = false,
                isPlaying = false,
                playbackError = friendlyMessage
            )
        }
    }

    init {
        managerScope.launch {
            networkMonitor.isOnline.collect { online ->
                if (!wasPreviouslyOnline && online && recoverOnNetworkRestore &&
                    _uiState.value.activeStream != null && isPlayerInitialized
                ) {
                    recoverOnNetworkRestore = false
                    _uiState.value = _uiState.value.copy(playbackError = null)
                    player.prepare()
                    player.play()
                }
                wasPreviouslyOnline = online
            }
        }
    }

    fun openOrAttachEpisode(
        animeId: String,
        seasonId: String,
        episodeNumber: Int,
        forceShowcaseDemo: Boolean
    ) {
        val current = _uiState.value
        if (forceShowcaseDemo) {
            useShowcaseStream = true
        }
        val sameEpisodeLoaded = current.animeId == animeId &&
            (current.selectedSeasonId == seasonId || seasonId.isBlank()) &&
            current.currentEpisodeNumber == episodeNumber &&
            (current.activeStream != null || current.isLoading)

        if (sameEpisodeLoaded) {
            if (forceShowcaseDemo && current.activeStream == null && !current.isLoading) {
                launchShowcaseDemo()
            }
            return
        }

        useShowcaseStream = forceShowcaseDemo
        loadCatalogAndStartEpisode(
            animeId = animeId,
            seasonId = seasonId,
            episodeNumber = episodeNumber
        )
    }

    private fun loadCatalogAndStartEpisode(
        animeId: String,
        seasonId: String,
        episodeNumber: Int
    ) {
        managerScope.launch {
            if (currentLoadedAnimeId != animeId) {
                cachedEpisodesBySeason.clear()
                currentLoadedAnimeId = animeId
            }
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                animeId = animeId,
                selectedSeasonId = seasonId,
                currentEpisodeNumber = episodeNumber,
                playbackError = null,
                useShowcaseStream = useShowcaseStream
            )

            val details = catalogRepository.getAnimeDetails(animeId).getOrNull()
            if (details != null) {
                cachedEpisodesBySeason.putAll(details.episodesBySeason)
                val resolvedSeason = details.seasons.firstOrNull { it.seasonId == seasonId }
                    ?: details.seasons.firstOrNull()
                val activeSeasonId = resolvedSeason?.seasonId ?: seasonId
                val seasonEpisodes = cachedEpisodesBySeason[activeSeasonId]
                    ?: resolvedSeason?.let { catalogRepository.getEpisodesForSeason(animeId, it) }
                    ?: emptyList()
                cachedEpisodesBySeason[activeSeasonId] = seasonEpisodes
                currentLoadedSeasonId = activeSeasonId

                _uiState.value = _uiState.value.copy(
                    animeTitle = details.series.title,
                    posterUrl = details.series.posterUrl,
                    seasons = details.seasons,
                    selectedSeasonId = activeSeasonId,
                    episodes = seasonEpisodes
                )
            } else {
                currentLoadedSeasonId = seasonId
                _uiState.value = _uiState.value.copy(
                    animeTitle = "Nime Watcher Stream",
                    selectedSeasonId = seasonId
                )
            }
            resolveAndPrepareEpisode(episodeNumber)
        }
    }

    private suspend fun resolveAndPrepareEpisode(episodeNumber: Int) {
        autoNextJob?.cancel()
        val state = _uiState.value
        val episodes = state.episodes
        val hasNext = episodes.any { it.episodeNumber == episodeNumber + 1 } ||
            (useShowcaseStream && episodeNumber < 12)
        val hasPrev = episodeNumber > 1

        _uiState.value = state.copy(
            isLoading = true,
            currentEpisodeNumber = episodeNumber,
            autoNextCountdownSec = null,
            playbackError = null,
            hasNextEpisode = hasNext,
            hasPreviousEpisode = hasPrev,
            useShowcaseStream = useShowcaseStream
        )

        val res = streamProvider.resolveEpisodeStream(
            animeId = state.animeId,
            animeTitle = state.animeTitle.ifBlank { "Anime" },
            seasonId = state.selectedSeasonId,
            episodeNumber = episodeNumber
        )

        val streamToPlay: AuthorizedStreamSource? = when (res) {
            is StreamResolutionResult.Authorized -> res.source
            is StreamResolutionResult.NotConfigured -> {
                if (useShowcaseStream) res.showcaseDemoSource else null
            }
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            resolutionResult = res,
            activeStream = streamToPlay,
            isPlaybackSessionActive = streamToPlay != null
        )

        if (streamToPlay != null) {
            val savedPosition = catalogRepository.getSavedPositionMs(
                animeId = state.animeId,
                seasonId = state.selectedSeasonId,
                episodeNumber = episodeNumber
            )
            prepareExoPlayerMedia(streamToPlay, episodeNumber, savedPosition)
            ensureMediaSessionServiceConnected()
        } else if (isPlayerInitialized) {
            player.stop()
            player.clearMediaItems()
        }
        onSessionStateChanged?.invoke()
    }

    private suspend fun prepareExoPlayerMedia(
        source: AuthorizedStreamSource,
        episodeNumber: Int,
        resumePositionMs: Long
    ) {
        val prefs = preferencesRepository.preferences.first()
        val state = _uiState.value
        val episodeTitle = state.episodes.firstOrNull { it.episodeNumber == episodeNumber }?.title
            ?.takeIf { it.isNotBlank() }
            ?: "Episode $episodeNumber"

        val subtitleConfigs = source.subtitles.map { sub ->
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.uri))
                .setMimeType(sub.mimeType)
                .setLanguage(sub.languageCode)
                .setLabel(sub.label)
                .setSelectionFlags(
                    if (sub.languageCode == prefs.preferredSubtitleLang) C.SELECTION_FLAG_DEFAULT else 0
                )
                .build()
        }

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle("${state.animeTitle} • Ep $episodeNumber")
            .setArtist(state.animeTitle.ifBlank { "Nime Watcher" })
            .setSubtitle(episodeTitle)
            .setDisplayTitle("${state.animeTitle} — $episodeTitle")
        state.posterUrl?.let { url ->
            runCatching { Uri.parse(url) }.getOrNull()?.let { uri ->
                metadataBuilder.setArtworkUri(uri)
            }
        }

        val mediaBuilder = MediaItem.Builder()
            .setUri(source.streamUrl)
            .setMediaId("${state.animeId}_ep_$episodeNumber")
            .setSubtitleConfigurations(subtitleConfigs)
            .setMediaMetadata(metadataBuilder.build())
        source.mimeType?.let { mediaBuilder.setMimeType(it) }

        val disableSubtitles = prefs.preferredSubtitleLang == "off"
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, disableSubtitles)
            .apply {
                if (!disableSubtitles) {
                    setPreferredTextLanguage(prefs.preferredSubtitleLang)
                }
            }
            .build()

        player.setMediaItem(mediaBuilder.build())
        if (resumePositionMs > 0L) {
            player.seekTo(resumePositionMs)
        }
        player.prepare()
        player.playWhenReady = true
    }

    private fun ensureMediaSessionServiceConnected() {
        if (controllerFuture != null) return
        try {
            val sessionToken = SessionToken(
                appContext,
                ComponentName(appContext, NimePlaybackService::class.java)
            )
            val future = MediaController.Builder(appContext, sessionToken).buildAsync()
            controllerFuture = future
            future.addListener(
                {
                    runCatching { future.get() }
                        .onFailure { err ->
                            Log.w(TAG, "MediaController connection closed or unavailable", err)
                            controllerFuture = null
                        }
                },
                MoreExecutors.directExecutor()
            )
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to bind NimePlaybackService", t)
            controllerFuture = null
        }
    }

    fun launchShowcaseDemo() {
        useShowcaseStream = true
        managerScope.launch {
            resolveAndPrepareEpisode(_uiState.value.currentEpisodeNumber)
        }
    }

    fun retryPlayback() {
        recoverOnNetworkRestore = false
        managerScope.launch {
            resolveAndPrepareEpisode(_uiState.value.currentEpisodeNumber)
        }
    }

    fun togglePlayPause() {
        if (!isPlayerInitialized) return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE && _uiState.value.activeStream != null) {
                player.prepare()
            }
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        if (!isPlayerInitialized) return
        val clamped = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(clamped)
        _uiState.value = _uiState.value.copy(currentPositionMs = clamped)
    }

    fun seekBy(deltaMs: Long) {
        if (!isPlayerInitialized) return
        seekTo(player.currentPosition + deltaMs)
    }

    fun skipIntro() {
        val endSec = _uiState.value.activeStream?.introEndSec ?: return
        seekTo(endSec * 1000L)
    }

    fun skipOutro() {
        val stream = _uiState.value.activeStream ?: return
        if (_uiState.value.hasNextEpisode) {
            nextEpisode()
        } else {
            val endSec = stream.outroEndSec ?: return
            seekTo(endSec * 1000L)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        if (!isPlayerInitialized) return
        player.setPlaybackSpeed(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun selectQuality(option: VideoQualityOption) {
        if (!isPlayerInitialized) return
        val builder = player.trackSelectionParameters.buildUpon()
        if (option.id == "auto" || option.groupIndex == null || option.trackIndex == null) {
            builder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                .clearVideoSizeConstraints()
        } else {
            val videoGroups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
            val targetGroup = videoGroups.getOrNull(option.groupIndex)
            if (targetGroup != null) {
                builder.setOverrideForType(
                    TrackSelectionOverride(targetGroup.mediaTrackGroup, listOf(option.trackIndex))
                )
            }
        }
        player.trackSelectionParameters = builder.build()
        _uiState.value = _uiState.value.copy(selectedQualityId = option.id)
    }

    fun selectAudioTrack(option: AudioTrackOption) {
        if (!isPlayerInitialized) return
        val audioGroups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        val targetGroup = audioGroups.getOrNull(option.groupIndex) ?: return
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(
                TrackSelectionOverride(targetGroup.mediaTrackGroup, listOf(option.trackIndex))
            )
            .build()
        _uiState.value = _uiState.value.copy(selectedAudioId = option.id)
    }

    fun selectSubtitleOption(option: SubtitleOption) {
        if (!isPlayerInitialized) return
        val builder = player.trackSelectionParameters.buildUpon()
        if (option.isOff) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            managerScope.launch { preferencesRepository.setPreferredSubtitleLang("off") }
        } else {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            val textGroups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
            val targetGroup = option.groupIndex?.let { textGroups.getOrNull(it) }
            if (targetGroup != null && option.trackIndex != null) {
                builder.setOverrideForType(
                    TrackSelectionOverride(targetGroup.mediaTrackGroup, listOf(option.trackIndex))
                )
            }
            option.language?.let { lang ->
                managerScope.launch { preferencesRepository.setPreferredSubtitleLang(lang) }
            }
        }
        player.trackSelectionParameters = builder.build()
        _uiState.value = _uiState.value.copy(selectedSubtitleId = option.id)
    }

    fun switchEpisode(episodeNumber: Int) {
        persistProgressNow()
        managerScope.launch {
            resolveAndPrepareEpisode(episodeNumber)
        }
    }

    fun selectSeason(season: AnimeSeasonGroup) {
        persistProgressNow()
        val animeId = _uiState.value.animeId
        if (animeId.isBlank()) return
        managerScope.launch {
            val eps = cachedEpisodesBySeason[season.seasonId]
                ?: catalogRepository.getEpisodesForSeason(animeId, season).also {
                    cachedEpisodesBySeason[season.seasonId] = it
                }
            currentLoadedSeasonId = season.seasonId
            _uiState.value = _uiState.value.copy(
                selectedSeasonId = season.seasonId,
                episodes = eps
            )
            resolveAndPrepareEpisode(1)
        }
    }

    fun nextEpisode() {
        if (_uiState.value.hasNextEpisode) {
            switchEpisode(_uiState.value.currentEpisodeNumber + 1)
        }
    }

    fun previousEpisode() {
        if (_uiState.value.hasPreviousEpisode) {
            switchEpisode(_uiState.value.currentEpisodeNumber - 1)
        }
    }

    fun cancelAutoNextCountdown() {
        autoNextJob?.cancel()
        _uiState.value = _uiState.value.copy(autoNextCountdownSec = null)
    }

    fun setInPipMode(inPip: Boolean) {
        _isInPipMode.value = inPip
    }

    fun handleAppMovedToBackground() {
        if (_isInPipMode.value) return
        val state = _uiState.value
        if (!state.isPlaybackSessionActive || state.activeStream == null) return

        managerScope.launch {
            val prefs = preferencesRepository.preferences.first()
            val allowBackgroundAudio = prefs.backgroundPlaybackEnabled && prefs.backgroundAudioEnabled
            if (allowBackgroundAudio) {
                if (isPlayerInitialized && player.isPlaying) {
                    _uiState.value = _uiState.value.copy(inBackgroundAudioMode = true)
                    ensureMediaSessionServiceConnected()
                }
            } else {
                if (isPlayerInitialized && player.isPlaying) {
                    player.pause()
                    persistProgressNow()
                }
                _uiState.value = _uiState.value.copy(inBackgroundAudioMode = false)
            }
        }
    }

    fun handleAppReturnedToForeground() {
        if (_uiState.value.inBackgroundAudioMode) {
            _uiState.value = _uiState.value.copy(inBackgroundAudioMode = false)
        }
    }

    fun requestOpenActivePlayerRoute() {
        val state = _uiState.value
        if (state.animeId.isBlank()) return
        _pendingPlayerRoute.value = NimeRoute.AnimePlayer(
            animeId = state.animeId,
            seasonId = state.selectedSeasonId,
            episodeNumber = state.currentEpisodeNumber,
            useShowcaseDemo = state.useShowcaseStream
        )
    }

    fun requestOpenPlayerRoute(route: NimeRoute.AnimePlayer) {
        _pendingPlayerRoute.value = route
    }

    fun consumePendingPlayerRoute() {
        _pendingPlayerRoute.value = null
    }

    fun stopPlaybackAndDismissSession() {
        autoNextJob?.cancel()
        persistProgressNow()
        if (isPlayerInitialized) {
            player.playWhenReady = false
            player.stop()
            player.clearMediaItems()
        }
        _uiState.value = _uiState.value.copy(
            isPlaying = false,
            isBuffering = false,
            activeStream = null,
            isPlaybackSessionActive = false,
            inBackgroundAudioMode = false,
            autoNextCountdownSec = null
        )
        onStopServiceRequested?.invoke()
        onSessionStateChanged?.invoke()
    }

    private fun handlePlaybackEnded() {
        managerScope.launch {
            val prefs = preferencesRepository.preferences.first()
            if (!prefs.autoNextEpisodeEnabled || !_uiState.value.hasNextEpisode) return@launch
            autoNextJob?.cancel()
            autoNextJob = managerScope.launch {
                for (sec in 5 downTo 1) {
                    _uiState.value = _uiState.value.copy(autoNextCountdownSec = sec)
                    delay(1000L)
                }
                _uiState.value = _uiState.value.copy(autoNextCountdownSec = null)
                nextEpisode()
            }
        }
    }

    private fun updateAvailableTrackOptions(tracks: Tracks) {
        if (!isPlayerInitialized) return
        val qualities = mutableListOf(VideoQualityOption("auto", "Auto"))
        val seenHeights = mutableSetOf<Int>()
        val videoGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
        var selectedQId = "auto"

        videoGroups.forEachIndexed { gIdx, group ->
            for (tIdx in 0 until group.length) {
                if (!group.isTrackSupported(tIdx)) continue
                val format = group.getTrackFormat(tIdx)
                val height = format.height
                if (height > 0 && seenHeights.add(height)) {
                    val optId = "q_${height}"
                    qualities.add(VideoQualityOption(optId, "${height}p", height, gIdx, tIdx))
                    if (group.isTrackSelected(tIdx) && !player.trackSelectionParameters.overrides.isEmpty()) {
                        selectedQId = optId
                    }
                }
            }
        }
        val sortedQualities = listOf(qualities.first()) + qualities.drop(1).sortedBy { it.height ?: 0 }

        val audios = mutableListOf<AudioTrackOption>()
        var selectedAudio: String? = null
        val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        audioGroups.forEachIndexed { gIdx, group ->
            for (tIdx in 0 until group.length) {
                if (!group.isTrackSupported(tIdx)) continue
                val fmt = group.getTrackFormat(tIdx)
                val langCode = fmt.language
                val langLabel = fmt.label ?: langCode?.let { Locale.forLanguageTag(it).displayLanguage }
                    ?.takeIf { it.isNotBlank() } ?: "Audio Track ${audios.size + 1}"
                val optId = "aud_${gIdx}_$tIdx"
                audios.add(AudioTrackOption(optId, langLabel, langCode, gIdx, tIdx))
                if (group.isTrackSelected(tIdx)) selectedAudio = optId
            }
        }

        val subtitles = mutableListOf(SubtitleOption("off", "Off", null, isOff = true))
        var selectedSubId = "off"
        val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
        val textDisabled = C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes
        textGroups.forEachIndexed { gIdx, group ->
            for (tIdx in 0 until group.length) {
                if (!group.isTrackSupported(tIdx)) continue
                val fmt = group.getTrackFormat(tIdx)
                val langCode = fmt.language
                val label = fmt.label ?: langCode?.let { Locale.forLanguageTag(it).displayLanguage }
                    ?.takeIf { it.isNotBlank() } ?: "Subtitles ${subtitles.size}"
                val optId = "sub_${gIdx}_$tIdx"
                subtitles.add(SubtitleOption(optId, label, langCode, isOff = false, gIdx, tIdx))
                if (!textDisabled && group.isTrackSelected(tIdx)) {
                    selectedSubId = optId
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            qualityOptions = sortedQualities,
            selectedQualityId = selectedQId,
            audioOptions = audios,
            selectedAudioId = selectedAudio,
            subtitleOptions = subtitles,
            selectedSubtitleId = selectedSubId
        )
    }

    private fun startProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = managerScope.launch {
            while (isActive) {
                if (isPlayerInitialized) {
                    val posMs = player.currentPosition.coerceAtLeast(0L)
                    val durMs = player.duration.coerceAtLeast(0L)
                    val stream = _uiState.value.activeStream
                    val posSec = posMs / 1000L

                    val showIntro = stream?.introStartSec != null && stream.introEndSec != null &&
                        posSec in stream.introStartSec until stream.introEndSec
                    val showOutro = stream?.outroStartSec != null && stream.outroEndSec != null &&
                        posSec in stream.outroStartSec until stream.outroEndSec

                    _uiState.value = _uiState.value.copy(
                        currentPositionMs = posMs,
                        durationMs = durMs,
                        showSkipIntro = showIntro,
                        showSkipOutro = showOutro
                    )

                    val now = System.currentTimeMillis()
                    if (player.isPlaying && now - lastSavedAtMs >= 5000L) {
                        lastSavedAtMs = now
                        persistProgressNow()
                    }
                }
                delay(500L)
            }
        }
    }

    fun persistProgressNow() {
        if (!isPlayerInitialized) return
        val state = _uiState.value
        if (state.activeStream == null) return
        val posMs = player.currentPosition.coerceAtLeast(0L)
        val durMs = player.duration.coerceAtLeast(0L)
        if (durMs <= 0L || posMs < 1500L) return
        managerScope.launch {
            catalogRepository.savePlaybackProgress(
                animeId = state.animeId,
                animeTitle = state.animeTitle.ifBlank { "Anime" },
                posterUrl = state.posterUrl,
                seasonId = state.selectedSeasonId,
                episodeNumber = state.currentEpisodeNumber,
                episodeTitle = "Episode ${state.currentEpisodeNumber}",
                positionMs = posMs,
                durationMs = durMs
            )
        }
    }

    private companion object {
        const val TAG = "PlaybackSessionManager"
        val NETWORK_ERROR_CODES = setOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED
        )
    }
}
