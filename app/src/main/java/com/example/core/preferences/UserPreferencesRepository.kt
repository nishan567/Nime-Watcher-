package com.example.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.nimeDataStore: DataStore<Preferences> by preferencesDataStore(name = "nime_watcher_prefs")

enum class AppLanguage(val code: String, val label: String) {
    ENGLISH("en", "English"),
    BANGLA("bn", "বাংলা");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

data class UserPreferences(
    val darkModeEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val rememberMeEnabled: Boolean = true,
    val backgroundPlaybackEnabled: Boolean = true,
    val autoPipEnabled: Boolean = true,
    val backgroundAudioEnabled: Boolean = true,
    val lockScreenControlsEnabled: Boolean = true,
    val autoNextEpisodeEnabled: Boolean = true,
    val subtitleFontScale: Float = 1.0f,
    val subtitleHighContrast: Boolean = true,
    val preferredSubtitleLang: String = "en",
)

class UserPreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.nimeDataStore

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
        val LANGUAGE = stringPreferencesKey("app_language")
        val REMEMBER_ME = booleanPreferencesKey("remember_me_enabled")
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback_enabled")
        val AUTO_PIP = booleanPreferencesKey("auto_pip_enabled")
        val BACKGROUND_AUDIO = booleanPreferencesKey("background_audio_enabled")
        val LOCK_SCREEN_CONTROLS = booleanPreferencesKey("lock_screen_controls_enabled")
        val AUTO_NEXT = booleanPreferencesKey("auto_next_episode_enabled")
        val SUBTITLE_FONT_SCALE = floatPreferencesKey("subtitle_font_scale")
        val SUBTITLE_HIGH_CONTRAST = booleanPreferencesKey("subtitle_high_contrast")
        val PREFERRED_SUBTITLE_LANG = stringPreferencesKey("preferred_subtitle_lang")
    }

    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            darkModeEnabled = prefs[Keys.DARK_MODE] ?: true,
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            rememberMeEnabled = prefs[Keys.REMEMBER_ME] ?: true,
            backgroundPlaybackEnabled = prefs[Keys.BACKGROUND_PLAYBACK] ?: true,
            autoPipEnabled = prefs[Keys.AUTO_PIP] ?: true,
            backgroundAudioEnabled = prefs[Keys.BACKGROUND_AUDIO] ?: true,
            lockScreenControlsEnabled = prefs[Keys.LOCK_SCREEN_CONTROLS] ?: true,
            autoNextEpisodeEnabled = prefs[Keys.AUTO_NEXT] ?: true,
            subtitleFontScale = prefs[Keys.SUBTITLE_FONT_SCALE] ?: 1.0f,
            subtitleHighContrast = prefs[Keys.SUBTITLE_HIGH_CONTRAST] ?: true,
            preferredSubtitleLang = prefs[Keys.PREFERRED_SUBTITLE_LANG] ?: "en",
        )
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setRememberMeEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.REMEMBER_ME] = enabled }
    }

    suspend fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BACKGROUND_PLAYBACK] = enabled }
    }

    suspend fun setAutoPipEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_PIP] = enabled }
    }

    suspend fun setBackgroundAudioEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BACKGROUND_AUDIO] = enabled }
    }

    suspend fun setLockScreenControlsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.LOCK_SCREEN_CONTROLS] = enabled }
    }

    suspend fun setAutoNextEpisodeEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_NEXT] = enabled }
    }

    suspend fun setSubtitleFontScale(scale: Float) {
        dataStore.edit { it[Keys.SUBTITLE_FONT_SCALE] = scale.coerceIn(0.75f, 1.75f) }
    }

    suspend fun setSubtitleHighContrast(enabled: Boolean) {
        dataStore.edit { it[Keys.SUBTITLE_HIGH_CONTRAST] = enabled }
    }

    suspend fun setPreferredSubtitleLang(langCode: String) {
        dataStore.edit { it[Keys.PREFERRED_SUBTITLE_LANG] = langCode }
    }
}
