package com.example

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.preferences.UserPreferences
import com.example.di.AppContainer
import com.example.navigation.NimeWatcherRoot
import com.example.playback.NimePlaybackService
import com.example.ui.theme.NimeWatcherTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val appContainer: AppContainer by lazy {
        (application as NimeWatcherApplication).container
    }

    private val pipListener = Consumer<PictureInPictureModeChangedInfo> { info ->
        appContainer.playbackManager.setInPipMode(info.isInPictureInPictureMode)
    }

    private val backgroundObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                appContainer.playbackManager.handleAppReturnedToForeground()
            }
            Lifecycle.Event.ON_STOP -> {
                if (!isChangingConfigurations) {
                    appContainer.playbackManager.handleAppMovedToBackground()
                }
            }
            else -> Unit
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        addOnPictureInPictureModeChangedListener(pipListener)
        lifecycle.addObserver(backgroundObserver)
        handleIncomingPlayerIntent(intent)

        setContent {
            val preferences by appContainer.preferencesRepository.preferences
                .collectAsStateWithLifecycle(initialValue = UserPreferences())
            val isInPipMode by appContainer.playbackManager.isInPipMode
                .collectAsStateWithLifecycle()

            val localizedContext = remember(preferences.language.code, this) {
                createLocalizedContext(this, preferences.language.code)
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                NimeWatcherTheme(darkTheme = preferences.darkModeEnabled) {
                    NimeWatcherRoot(
                        container = appContainer,
                        preferences = preferences,
                        isInPipMode = isInPipMode
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingPlayerIntent(intent)
    }

    override fun onDestroy() {
        removeOnPictureInPictureModeChangedListener(pipListener)
        lifecycle.removeObserver(backgroundObserver)
        super.onDestroy()
    }

    private fun handleIncomingPlayerIntent(incoming: Intent?) {
        if (incoming?.action == NimePlaybackService.ACTION_OPEN_ACTIVE_PLAYER) {
            appContainer.playbackManager.requestOpenActivePlayerRoute()
        }
    }

    private fun createLocalizedContext(base: Context, languageCode: String): Context {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
