package com.example.ui.player

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.graphics.Rect
import android.graphics.drawable.Icon as AndroidIcon
import android.media.AudioManager
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import com.example.R
import com.example.core.ui.NimeErrorStateCard
import com.example.domain.stream.StreamResolutionResult
import com.example.playback.NimePlaybackService
import com.example.playback.PipUtils
import com.example.playback.PipUtils.findActivity
import com.example.ui.theme.VibrantOrange
import com.example.ui.theme.WarmAmber
import kotlinx.coroutines.delay
import java.util.Locale

private enum class ActivePlayerSheet { NONE, QUALITY_SPEED, SUBTITLES, EPISODES, PLAYBACK_PREFS }

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@OptIn(UnstableApi::class)
@Composable
fun AnimePlayerScreen(
    viewModel: AnimePlayerViewModel,
    isInPipMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val componentActivity = activity as? ComponentActivity
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    val isPipSupported = remember(context) { PipUtils.isPipSupported(context) }
    val shouldAutoEnterPip = isPipSupported &&
        preferences.autoPipEnabled &&
        state.activeStream != null &&
        state.isPlaying
    val currentShouldAutoEnterPip by rememberUpdatedState(shouldAutoEnterPip)

    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var promptedForNotifications by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
    }

    LaunchedEffect(state.activeStream != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            state.activeStream != null &&
            !notificationsGranted &&
            !promptedForNotifications
        ) {
            promptedForNotifications = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var controlsLocked by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf(ActivePlayerSheet.NONE) }
    var gestureFeedbackText by remember { mutableStateOf<String?>(null) }
    var pipSourceRect by remember { mutableStateOf<Rect?>(null) }

    val initialMaxVol = remember(audioManager) {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    var currentVolumeRatio by remember {
        mutableFloatStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / initialMaxVol.toFloat()
        )
    }
    var currentBrightness by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it in 0f..1f } ?: 0.65f
        )
    }

    // Keep Android PiP parameters & remote actions synced with live player state
    LaunchedEffect(
        componentActivity,
        isPipSupported,
        shouldAutoEnterPip,
        state.isPlaying,
        state.hasNextEpisode,
        state.hasPreviousEpisode,
        pipSourceRect
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPipSupported && componentActivity != null) {
            val videoSize = if (state.activeStream != null) viewModel.player.videoSize else VideoSize.UNKNOWN
            val params = buildPipParams(
                context = context,
                videoWidth = videoSize.width,
                videoHeight = videoSize.height,
                sourceRect = pipSourceRect,
                isPlaying = state.isPlaying,
                hasNext = state.hasNextEpisode,
                hasPrev = state.hasPreviousEpisode,
                autoEnter = shouldAutoEnterPip
            )
            runCatching { componentActivity.setPictureInPictureParams(params) }
        }
    }

    // Pre-Android 12 (Android 8..11) PiP trigger via onUserLeaveHint
    if (Build.VERSION.SDK_INT in Build.VERSION_CODES.O until Build.VERSION_CODES.S &&
        isPipSupported &&
        componentActivity != null
    ) {
        DisposableEffect(componentActivity, context) {
            val onUserLeaveHint = Runnable {
                if (currentShouldAutoEnterPip) {
                    val videoSize = viewModel.player.videoSize
                    val params = buildPipParams(
                        context = context,
                        videoWidth = videoSize.width,
                        videoHeight = videoSize.height,
                        sourceRect = pipSourceRect,
                        isPlaying = state.isPlaying,
                        hasNext = state.hasNextEpisode,
                        hasPrev = state.hasPreviousEpisode,
                        autoEnter = false
                    )
                    runCatching { componentActivity.enterPictureInPictureMode(params) }
                }
            }
            componentActivity.addOnUserLeaveHintListener(onUserLeaveHint)
            onDispose {
                componentActivity.removeOnUserLeaveHintListener(onUserLeaveHint)
            }
        }
    }

    // Restore orientation and system bars safely when leaving the player screen
    DisposableEffect(activity) {
        onDispose {
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(isFullscreen, isInPipMode, activity) {
        activity?.let { act ->
            val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
            if (isInPipMode) {
                activeSheet = ActivePlayerSheet.NONE
                controlsVisible = false
            } else if (isFullscreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Auto-hide controls while playing
    LaunchedEffect(controlsVisible, state.isPlaying, controlsLocked, activeSheet, isInPipMode) {
        if (!isInPipMode &&
            controlsVisible &&
            state.isPlaying &&
            !controlsLocked &&
            activeSheet == ActivePlayerSheet.NONE
        ) {
            delay(4000L)
            controlsVisible = false
        }
    }

    LaunchedEffect(gestureFeedbackText) {
        if (gestureFeedbackText != null) {
            delay(1100L)
            gestureFeedbackText = null
        }
    }

    fun triggerManualPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPipSupported && activity != null) {
            val videoSize = viewModel.player.videoSize
            val params = buildPipParams(
                context = context,
                videoWidth = videoSize.width,
                videoHeight = videoSize.height,
                sourceRect = pipSourceRect,
                isPlaying = state.isPlaying,
                hasNext = state.hasNextEpisode,
                hasPrev = state.hasPreviousEpisode,
                autoEnter = shouldAutoEnterPip
            )
            activity.enterPictureInPictureMode(params)
        }
    }

    Scaffold(
        containerColor = if (isFullscreen || isInPipMode) Color.Black else MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("anime_player_screen")
    ) { padding ->
        val res = state.resolutionResult
        if (!isInPipMode &&
            !state.isLoading &&
            state.activeStream == null &&
            res is StreamResolutionResult.NotConfigured
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                StreamNotConfiguredCard(
                    animeTitle = state.animeTitle.ifBlank { "Anime" },
                    episodeNumber = state.currentEpisodeNumber,
                    reason = res.reason,
                    onLaunchShowcase = { viewModel.launchShowcaseDemo() },
                    onBack = onBack
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen || isInPipMode) PaddingValues(0.dp) else padding)
        ) {
            // Video Player Viewport
            BoxWithConstraints(
                modifier = (if (isFullscreen || isInPipMode) {
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                }).onGloballyPositioned { coordinates ->
                    val androidRect = coordinates.boundsInWindow().toAndroidRectF().toRect()
                    if (androidRect.width() > 0 && androidRect.height() > 0) {
                        pipSourceRect = androidRect
                    }
                }
            ) {
                val viewportWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val viewportHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            useController = false
                            keepScreenOn = true
                            player = viewModel.player
                        }
                    },
                    update = { playerView ->
                        if (playerView.player !== viewModel.player) {
                            playerView.player = viewModel.player
                        }
                        playerView.subtitleView?.let { subView ->
                            val bg = if (preferences.subtitleHighContrast) {
                                AndroidColor.argb(185, 10, 11, 16)
                            } else {
                                AndroidColor.TRANSPARENT
                            }
                            subView.setStyle(
                                CaptionStyleCompat(
                                    AndroidColor.WHITE,
                                    bg,
                                    AndroidColor.TRANSPARENT,
                                    CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                    AndroidColor.BLACK,
                                    null
                                )
                            )
                            subView.setFractionalTextSize(
                                SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * preferences.subtitleFontScale
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (!isInPipMode) {
                    // Gesture overlay (Tap, Double-Tap Seek, Brightness/Volume vertical drag)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(controlsLocked, viewportWidthPx) {
                                detectTapGestures(
                                    onTap = {
                                        controlsVisible = !controlsVisible
                                    },
                                    onDoubleTap = { offset ->
                                        if (controlsLocked) return@detectTapGestures
                                        if (offset.x < viewportWidthPx * 0.45f) {
                                            viewModel.seekBy(-10_000L)
                                            gestureFeedbackText = "-10s"
                                        } else {
                                            viewModel.seekBy(10_000L)
                                            gestureFeedbackText = "+10s"
                                        }
                                    }
                                )
                            }
                            .pointerInput(controlsLocked, viewportWidthPx, viewportHeightPx) {
                                detectVerticalDragGestures { change, dragAmount ->
                                    if (controlsLocked) return@detectVerticalDragGestures
                                    change.consume()
                                    val deltaRatio = -dragAmount / viewportHeightPx
                                    if (change.position.x < viewportWidthPx * 0.5f) {
                                        currentBrightness =
                                            (currentBrightness + deltaRatio).coerceIn(0.05f, 1.0f)
                                        activity?.window?.let { w ->
                                            val lp = w.attributes
                                            lp.screenBrightness = currentBrightness
                                            w.attributes = lp
                                        }
                                        gestureFeedbackText =
                                            "Brightness ${(currentBrightness * 100).toInt()}%"
                                    } else {
                                        currentVolumeRatio =
                                            (currentVolumeRatio + deltaRatio).coerceIn(0f, 1f)
                                        val newVol = (currentVolumeRatio * initialMaxVol).toInt()
                                            .coerceIn(0, initialMaxVol)
                                        audioManager.setStreamVolume(
                                            AudioManager.STREAM_MUSIC,
                                            newVol,
                                            0
                                        )
                                        gestureFeedbackText =
                                            "Volume ${(currentVolumeRatio * 100).toInt()}%"
                                    }
                                }
                            }
                    )
                }

                // Buffering spinner
                if (state.isLoading || state.isBuffering) {
                    CircularProgressIndicator(
                        color = VibrantOrange,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (!isInPipMode) {
                    // Gesture feedback pill (Seek / Brightness / Volume)
                    gestureFeedbackText?.let { feedback ->
                        Surface(
                            color = Color.Black.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 56.dp)
                        ) {
                            Text(
                                text = feedback,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = VibrantOrange,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Intro / Outro Skip buttons (shown only inside verified windows)
                    if (!controlsLocked && (state.showSkipIntro || state.showSkipOutro)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 70.dp)
                        ) {
                            if (state.showSkipIntro) {
                                Button(
                                    onClick = { viewModel.skipIntro() },
                                    colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                                    modifier = Modifier.testTag("skip_intro_button")
                                ) {
                                    Text("Skip Intro", fontWeight = FontWeight.Bold)
                                }
                            }
                            if (state.showSkipOutro) {
                                Button(
                                    onClick = { viewModel.skipOutro() },
                                    colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                                    modifier = Modifier.testTag("skip_outro_button")
                                ) {
                                    Text("Skip Outro", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Auto-Next Episode Countdown Overlay
                    state.autoNextCountdownSec?.let { sec ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.86f)),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .testTag("auto_next_overlay")
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Text(
                                    text = "Up Next: Episode ${state.currentEpisodeNumber + 1} in ${sec}s",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { viewModel.cancelAutoNextCountdown() }) {
                                        Text("Cancel", color = Color.White)
                                    }
                                    Button(
                                        onClick = { viewModel.nextEpisode() },
                                        colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange)
                                    ) {
                                        Text("Play Now")
                                    }
                                }
                            }
                        }
                    }

                    // Locked controls minimal unlock button
                    if (controlsLocked && controlsVisible) {
                        OutlinedButton(
                            onClick = { controlsLocked = false },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.72f)
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .testTag("unlock_controls_button")
                        ) {
                            Icon(Icons.Rounded.LockOpen, contentDescription = null, tint = VibrantOrange)
                            Spacer(Modifier.width(6.dp))
                            Text("Unlock Controls", color = Color.White)
                        }
                    }

                    // Full Custom Nime Watcher HUD Overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = controlsVisible && !controlsLocked,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.72f),
                                            Color.Black.copy(alpha = 0.25f),
                                            Color.Black.copy(alpha = 0.84f)
                                        )
                                    )
                                )
                        ) {
                            // Top Bar
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (isFullscreen) isFullscreen = false else onBack()
                                        },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("player_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = "Back",
                                            tint = Color.White
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "${state.animeTitle} • Ep ${state.currentEpisodeNumber}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        state.activeStream?.let { stream ->
                                            Text(
                                                text = stream.providerName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = WarmAmber,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isPipSupported && state.activeStream != null) {
                                        IconButton(
                                            onClick = { triggerManualPip() },
                                            modifier = Modifier.testTag("player_pip_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PictureInPictureAlt,
                                                contentDescription = "Enter Picture-in-Picture",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { activeSheet = ActivePlayerSheet.EPISODES },
                                        modifier = Modifier.testTag("player_episodes_button")
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.PlaylistPlay,
                                            contentDescription = "Episodes",
                                            tint = Color.White
                                        )
                                    }
                                    IconButton(
                                        onClick = { controlsLocked = true },
                                        modifier = Modifier.testTag("player_lock_button")
                                    ) {
                                        Icon(Icons.Rounded.Lock, contentDescription = "Lock controls", tint = Color.White)
                                    }
                                }
                            }

                            // Center Transport Controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                IconButton(
                                    onClick = { viewModel.previousEpisode() },
                                    enabled = state.hasPreviousEpisode,
                                    modifier = Modifier.testTag("player_prev_episode_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Previous episode",
                                        tint = if (state.hasPreviousEpisode) Color.White else Color.Gray,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.seekBy(-10_000L) },
                                    modifier = Modifier.testTag("player_seek_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Replay10,
                                        contentDescription = "Seek backward 10 seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.togglePlayPause() },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(VibrantOrange)
                                        .testTag("player_play_pause_button")
                                ) {
                                    Icon(
                                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.seekBy(10_000L) },
                                    modifier = Modifier.testTag("player_seek_forward_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Forward10,
                                        contentDescription = "Seek forward 10 seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.nextEpisode() },
                                    enabled = state.hasNextEpisode,
                                    modifier = Modifier.testTag("player_next_episode_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Next episode",
                                        tint = if (state.hasNextEpisode) Color.White else Color.Gray,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            // Bottom Timeline + Action Chips
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = formatPlayerTime(state.currentPositionMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                    Slider(
                                        value = if (state.durationMs > 0L) {
                                            (state.currentPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                                        } else 0f,
                                        onValueChange = { frac ->
                                            val target = (frac * state.durationMs).toLong()
                                            viewModel.seekTo(target)
                                        },
                                        colors = SliderDefaults.colors(
                                            thumbColor = VibrantOrange,
                                            activeTrackColor = VibrantOrange,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                            .testTag("player_timeline_slider")
                                    )
                                    Text(
                                        text = formatPlayerTime(state.durationMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                    IconButton(
                                        onClick = { isFullscreen = !isFullscreen },
                                        modifier = Modifier.testTag("player_fullscreen_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                            contentDescription = "Toggle fullscreen",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Below-video Player Control Bar & Episode Controls (Portrait mode, hidden in PiP)
            if (!isFullscreen && !isInPipMode) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    state.playbackError?.let { err ->
                        NimeErrorStateCard(
                            message = err,
                            onRetry = { viewModel.retryPlayback() }
                        )
                    }

                    // Quick Media3 Settings Row (Quality, Subtitles, Episodes, PiP & Background Audio)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            AssistChip(
                                onClick = { activeSheet = ActivePlayerSheet.QUALITY_SPEED },
                                leadingIcon = { Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = {
                                    val qLabel = state.qualityOptions.firstOrNull { it.id == state.selectedQualityId }?.label ?: "Auto"
                                    Text("$qLabel • ${state.playbackSpeed}x")
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("player_quality_speed_chip")
                            )
                        }
                        item {
                            AssistChip(
                                onClick = { activeSheet = ActivePlayerSheet.SUBTITLES },
                                leadingIcon = { Icon(Icons.Rounded.ClosedCaption, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Subtitles") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("player_subtitles_chip")
                            )
                        }
                        item {
                            AssistChip(
                                onClick = { activeSheet = ActivePlayerSheet.EPISODES },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text("Episodes (${state.episodes.size})") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("player_episodes_chip")
                            )
                        }
                        item {
                            AssistChip(
                                onClick = { activeSheet = ActivePlayerSheet.PLAYBACK_PREFS },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Headphones,
                                        contentDescription = null,
                                        tint = if (preferences.backgroundPlaybackEnabled) VibrantOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("PiP & Background") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("player_playback_prefs_chip")
                            )
                        }
                    }

                    // Manual Brightness & Volume Fine Controls (accessible even without touch swipe)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Brightness6, contentDescription = null, tint = WarmAmber, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Brightness", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(74.dp))
                                Slider(
                                    value = currentBrightness,
                                    onValueChange = { value ->
                                        currentBrightness = value
                                        activity?.window?.let { w ->
                                            val lp = w.attributes
                                            lp.screenBrightness = value
                                            w.attributes = lp
                                        }
                                    },
                                    valueRange = 0.05f..1.0f,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, tint = VibrantOrange, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Volume", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(74.dp))
                                Slider(
                                    value = currentVolumeRatio,
                                    onValueChange = { value ->
                                        currentVolumeRatio = value
                                        val target = (value * initialMaxVol).toInt().coerceIn(0, initialMaxVol)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    state.activeStream?.let { stream ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${stream.licenseNotice} • ${stream.providerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (activeSheet != ActivePlayerSheet.NONE && !isInPipMode) {
        ModalBottomSheet(onDismissRequest = { activeSheet = ActivePlayerSheet.NONE }) {
            when (activeSheet) {
                ActivePlayerSheet.QUALITY_SPEED -> QualityAudioSpeedSheet(
                    state = state,
                    autoNextEnabled = preferences.autoNextEpisodeEnabled,
                    onSelectQuality = viewModel::selectQuality,
                    onSelectAudio = viewModel::selectAudioTrack,
                    onSelectSpeed = viewModel::setPlaybackSpeed,
                    onToggleAutoNext = viewModel::setAutoNextEnabled
                )
                ActivePlayerSheet.SUBTITLES -> SubtitleSettingsSheet(
                    state = state,
                    preferences = preferences,
                    onSelectSubtitle = viewModel::selectSubtitleOption,
                    onSelectFontScale = viewModel::setSubtitleFontScale,
                    onToggleHighContrast = viewModel::setSubtitleHighContrast
                )
                ActivePlayerSheet.EPISODES -> EpisodeSelectorSheet(
                    state = state,
                    onSelectSeason = {
                        viewModel.selectSeason(it)
                    },
                    onSelectEpisode = { epNum ->
                        activeSheet = ActivePlayerSheet.NONE
                        viewModel.switchEpisode(epNum)
                    }
                )
                ActivePlayerSheet.PLAYBACK_PREFS -> PlaybackPreferencesSheet(
                    preferences = preferences,
                    isPipSupported = isPipSupported,
                    notificationsGranted = notificationsGranted,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onToggleBackgroundPlayback = viewModel::setBackgroundPlaybackEnabled,
                    onToggleAutoPip = viewModel::setAutoPipEnabled,
                    onToggleBackgroundAudio = viewModel::setBackgroundAudioEnabled,
                    onToggleLockScreenControls = viewModel::setLockScreenControlsEnabled,
                    onToggleAutoNext = viewModel::setAutoNextEnabled
                )
                ActivePlayerSheet.NONE -> Unit
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun buildPipParams(
    context: Context,
    videoWidth: Int,
    videoHeight: Int,
    sourceRect: Rect?,
    isPlaying: Boolean,
    hasNext: Boolean,
    hasPrev: Boolean,
    autoEnter: Boolean
): PictureInPictureParams {
    val actions = mutableListOf<RemoteAction>()

    if (hasPrev) {
        actions.add(
            createServiceRemoteAction(
                context = context,
                action = NimePlaybackService.ACTION_PIP_PREV,
                requestCode = 6101,
                iconRes = R.drawable.ic_pip_prev,
                title = context.getString(R.string.pip_prev)
            )
        )
    }

    actions.add(
        createServiceRemoteAction(
            context = context,
            action = NimePlaybackService.ACTION_PIP_PLAY_PAUSE,
            requestCode = 6102,
            iconRes = if (isPlaying) R.drawable.ic_pip_pause else R.drawable.ic_pip_play,
            title = context.getString(if (isPlaying) R.string.pip_pause else R.string.pip_play)
        )
    )

    if (hasNext) {
        actions.add(
            createServiceRemoteAction(
                context = context,
                action = NimePlaybackService.ACTION_PIP_NEXT,
                requestCode = 6103,
                iconRes = R.drawable.ic_pip_next,
                title = context.getString(R.string.pip_next)
            )
        )
    }

    return PictureInPictureParams.Builder()
        .setAspectRatio(PipUtils.safeAspectRatio(videoWidth, videoHeight))
        .setActions(actions)
        .apply {
            if (sourceRect != null && !sourceRect.isEmpty) {
                setSourceRectHint(sourceRect)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setAutoEnterEnabled(autoEnter)
                setSeamlessResizeEnabled(true)
            }
        }
        .build()
}

@RequiresApi(Build.VERSION_CODES.O)
private fun createServiceRemoteAction(
    context: Context,
    action: String,
    requestCode: Int,
    iconRes: Int,
    title: String
): RemoteAction {
    val intent = Intent(context, NimePlaybackService::class.java).apply {
        this.action = action
    }
    val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        PendingIntent.getForegroundService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    } else {
        PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    return RemoteAction(
        AndroidIcon.createWithResource(context, iconRes),
        title,
        title,
        pendingIntent
    )
}

private fun formatPlayerTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}
