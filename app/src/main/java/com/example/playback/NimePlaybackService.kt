package com.example.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.NimeWatcherApplication
import com.example.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground Media3 MediaSessionService managing background audio, lock-screen media controls,
 * rich episode notifications (Previous, Play/Pause, Next, Stop), and PiP RemoteAction intents.
 */
@OptIn(UnstableApi::class)
class NimePlaybackService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var playbackManager: PlaybackSessionManager
    private var mediaSession: MediaSession? = null
    private var lockScreenControlsEnabled: Boolean = true

    override fun onCreate() {
        super.onCreate()
        val container = (applicationContext as NimeWatcherApplication).container
        playbackManager = container.playbackManager

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(PLAYBACK_CHANNEL_ID)
            .setChannelName(R.string.notification_channel_playback)
            .build()
            .apply {
                setSmallIcon(R.drawable.ic_nime_notification)
            }
        setMediaNotificationProvider(notificationProvider)

        val forwardingPlayer = NimeSessionForwardingPlayer(playbackManager)

        val stopButton = CommandButton.Builder(CommandButton.ICON_STOP)
            .setDisplayName(getString(R.string.pip_stop))
            .setSessionCommand(SessionCommand(ACTION_STOP_PLAYBACK, Bundle.EMPTY))
            .setCustomIconResId(R.drawable.ic_pip_stop)
            .build()

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setId(SESSION_ID)
            .setSessionActivity(buildResumePlayerPendingIntent())
            .setMediaButtonPreferences(listOf(stopButton))
            .setCallback(NimeMediaSessionCallback(stopButton))
            .build()

        playbackManager.onStopServiceRequested = {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        playbackManager.onSessionStateChanged = {
            mediaSession?.let { session ->
                updateMediaNotificationControllerAccess(session)
            }
        }

        serviceScope.launch {
            container.preferencesRepository.preferences.collect { prefs ->
                lockScreenControlsEnabled = prefs.lockScreenControlsEnabled
                mediaSession?.let { session ->
                    updateMediaNotificationControllerAccess(session)
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PIP_PLAY_PAUSE -> playbackManager.togglePlayPause()
            ACTION_PIP_NEXT -> playbackManager.nextEpisode()
            ACTION_PIP_PREV -> playbackManager.previousEpisode()
            ACTION_STOP_PLAYBACK -> playbackManager.stopPlaybackAndDismissSession()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        serviceScope.launch {
            val prefs = (applicationContext as NimeWatcherApplication)
                .container.preferencesRepository.preferences.first()
            val shouldKeepPlaying = prefs.backgroundPlaybackEnabled &&
                prefs.backgroundAudioEnabled &&
                playbackManager.isPlayerInitialized &&
                playbackManager.player.playWhenReady &&
                playbackManager.player.mediaItemCount > 0
            if (!shouldKeepPlaying) {
                playbackManager.stopPlaybackAndDismissSession()
            }
        }
    }

    override fun onDestroy() {
        playbackManager.onStopServiceRequested = null
        playbackManager.onSessionStateChanged = null
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun updateMediaNotificationControllerAccess(session: MediaSession) {
        val notificationController = session.mediaNotificationControllerInfo ?: return
        if (lockScreenControlsEnabled) {
            val commands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_STOP)
                .build()
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(ACTION_STOP_PLAYBACK, Bundle.EMPTY))
                .build()
            session.setAvailableCommands(notificationController, sessionCommands, commands)
        } else {
            val minimalCommands = Player.Commands.Builder()
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_STOP)
                .build()
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(ACTION_STOP_PLAYBACK, Bundle.EMPTY))
                .build()
            session.setAvailableCommands(notificationController, sessionCommands, minimalCommands)
        }
    }

    private fun buildResumePlayerPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN_ACTIVE_PLAYER
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            REQUEST_CODE_OPEN_PLAYER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private inner class NimeMediaSessionCallback(
        private val stopButton: CommandButton
    ) : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(ACTION_STOP_PLAYBACK, Bundle.EMPTY))
                .build()

            val playerCommands = if (session.isMediaNotificationController(controller) && !lockScreenControlsEnabled) {
                Player.Commands.Builder()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_STOP)
                    .build()
            } else {
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_STOP)
                    .build()
            }

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
                .setMediaButtonPreferences(listOf(stopButton))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            return when (customCommand.customAction) {
                ACTION_STOP_PLAYBACK -> {
                    playbackManager.stopPlaybackAndDismissSession()
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                else -> super.onCustomCommand(session, controller, customCommand, args)
            }
        }
    }

    private class NimeSessionForwardingPlayer(
        private val manager: PlaybackSessionManager
    ) : ForwardingPlayer(manager.player) {

        override fun getAvailableCommands(): Player.Commands {
            val state = manager.uiState.value
            return super.getAvailableCommands().buildUpon()
                .apply {
                    if (state.hasNextEpisode) {
                        add(Player.COMMAND_SEEK_TO_NEXT)
                        add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    } else {
                        remove(Player.COMMAND_SEEK_TO_NEXT)
                        remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    }
                    if (state.hasPreviousEpisode) {
                        add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    } else {
                        remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                        remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    }
                    add(Player.COMMAND_STOP)
                }
                .build()
        }

        override fun isCommandAvailable(command: Int): Boolean {
            return when (command) {
                Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ->
                    manager.uiState.value.hasNextEpisode
                Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM ->
                    manager.uiState.value.hasPreviousEpisode
                Player.COMMAND_STOP -> true
                else -> super.isCommandAvailable(command)
            }
        }

        override fun seekToNext() {
            manager.nextEpisode()
        }

        override fun seekToNextMediaItem() {
            manager.nextEpisode()
        }

        override fun seekToPrevious() {
            manager.previousEpisode()
        }

        override fun seekToPreviousMediaItem() {
            manager.previousEpisode()
        }

        override fun stop() {
            manager.stopPlaybackAndDismissSession()
        }
    }

    companion object {
        const val PLAYBACK_CHANNEL_ID = "nime_playback_channel"
        const val SESSION_ID = "nime_watcher_session"
        const val ACTION_OPEN_ACTIVE_PLAYER = "com.example.playback.OPEN_ACTIVE_PLAYER"
        const val ACTION_PIP_PLAY_PAUSE = "com.example.playback.PIP_PLAY_PAUSE"
        const val ACTION_PIP_NEXT = "com.example.playback.PIP_NEXT"
        const val ACTION_PIP_PREV = "com.example.playback.PIP_PREV"
        const val ACTION_STOP_PLAYBACK = "com.example.playback.STOP_PLAYBACK"
        private const val REQUEST_CODE_OPEN_PLAYER = 5001
    }
}
