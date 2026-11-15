package com.example.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.preferences.UserPreferences
import com.example.core.ui.NimeGradientButton
import com.example.domain.model.AnimeSeasonGroup
import com.example.ui.theme.VibrantOrange
import com.example.ui.theme.WarmAmber

@Composable
fun StreamNotConfiguredCard(
    animeTitle: String,
    episodeNumber: Int,
    reason: String,
    onLaunchShowcase: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp)
            .testTag("stream_not_configured_card")
    ) {
        IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = WarmAmber,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "$animeTitle • Episode $episodeNumber",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NimeGradientButton(
                    text = "Launch Media3 Adaptive HLS Showcase",
                    onClick = onLaunchShowcase,
                    icon = Icons.Rounded.PlayArrow,
                    testTag = "launch_showcase_stream_button"
                )
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Back to Anime Details")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QualityAudioSpeedSheet(
    state: PlayerUiState,
    autoNextEnabled: Boolean,
    onSelectQuality: (VideoQualityOption) -> Unit,
    onSelectAudio: (AudioTrackOption) -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onToggleAutoNext: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Stream Quality, Audio & Speed",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        Text(
            text = "Video Quality (Verified Stream Tracks)",
            style = MaterialTheme.typography.titleSmall
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.qualityOptions.forEach { option ->
                FilterChip(
                    selected = state.selectedQualityId == option.id,
                    onClick = { onSelectQuality(option) },
                    label = { Text(option.label) },
                    leadingIcon = if (state.selectedQualityId == option.id) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = VibrantOrange
                    )
                )
            }
        }

        HorizontalDivider()
        Text("Playback Speed", style = MaterialTheme.typography.titleSmall)
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(speeds) { speed ->
                FilterChip(
                    selected = state.playbackSpeed == speed,
                    onClick = { onSelectSpeed(speed) },
                    leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = { Text("${speed}x") }
                )
            }
        }

        if (state.audioOptions.isNotEmpty()) {
            HorizontalDivider()
            Text("Audio Tracks", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.audioOptions.forEach { audio ->
                    FilterChip(
                        selected = state.selectedAudioId == audio.id,
                        onClick = { onSelectAudio(audio) },
                        label = { Text(audio.label) }
                    )
                }
            }
        }

        HorizontalDivider()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto Play Next Episode", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Automatically start the next episode when finished",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = autoNextEnabled,
                onCheckedChange = onToggleAutoNext,
                colors = SwitchDefaults.colors(checkedTrackColor = VibrantOrange)
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubtitleSettingsSheet(
    state: PlayerUiState,
    preferences: UserPreferences,
    onSelectSubtitle: (SubtitleOption) -> Unit,
    onSelectFontScale: (Float) -> Unit,
    onToggleHighContrast: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.ClosedCaption, contentDescription = null, tint = VibrantOrange)
            Text(
                text = "Subtitles & Caption Style",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Text("Available Subtitle Tracks", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.subtitleOptions.forEach { option ->
                FilterChip(
                    selected = state.selectedSubtitleId == option.id,
                    onClick = { onSelectSubtitle(option) },
                    label = { Text(option.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = VibrantOrange
                    )
                )
            }
        }

        HorizontalDivider()
        Text("Subtitle Text Size", style = MaterialTheme.typography.titleSmall)
        val sizePresets = listOf(
            0.85f to "Small",
            1.0f to "Normal",
            1.25f to "Large",
            1.5f to "Extra Large"
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sizePresets.forEach { (scale, label) ->
                FilterChip(
                    selected = kotlin.math.abs(preferences.subtitleFontScale - scale) < 0.05f,
                    onClick = { onSelectFontScale(scale) },
                    label = { Text(label) }
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("High-Contrast Subtitle Backdrop", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Adds a dark translucent background behind captions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = preferences.subtitleHighContrast,
                onCheckedChange = onToggleHighContrast,
                colors = SwitchDefaults.colors(checkedTrackColor = VibrantOrange)
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun EpisodeSelectorSheet(
    state: PlayerUiState,
    onSelectSeason: (AnimeSeasonGroup) -> Unit,
    onSelectEpisode: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Episodes & Seasons",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        if (state.seasons.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.seasons, key = { it.seasonId }) { season ->
                    FilterChip(
                        selected = state.selectedSeasonId == season.seasonId,
                        onClick = { onSelectSeason(season) },
                        label = { Text(season.label) }
                    )
                }
            }
        }

        val displayEpisodes = if (state.episodes.isNotEmpty()) {
            state.episodes.map { it.episodeNumber to it.title }
        } else {
            (1..12).map { it to "Episode $it" }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            items(displayEpisodes, key = { it.first }) { (epNum, epTitle) ->
                val isCurrent = epNum == state.currentEpisodeNumber
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectEpisode(epNum) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "#$epNum",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = VibrantOrange
                            )
                            Text(
                                text = epTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (isCurrent) {
                            Text(
                                text = "Playing",
                                style = MaterialTheme.typography.labelSmall,
                                color = VibrantOrange
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaybackPreferencesSheet(
    preferences: UserPreferences,
    isPipSupported: Boolean,
    notificationsGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onToggleBackgroundPlayback: (Boolean) -> Unit,
    onToggleAutoPip: (Boolean) -> Unit,
    onToggleBackgroundAudio: (Boolean) -> Unit,
    onToggleLockScreenControls: (Boolean) -> Unit,
    onToggleAutoNext: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("playback_preferences_sheet")
    ) {
        Text(
            text = "Picture-in-Picture & Background Audio",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Control how Nime Watcher handles PiP windows, background audio when PiP closes, and lock-screen media controls.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!notificationsGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Allow notifications to display lock-screen & background controls.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onRequestNotificationPermission,
                        modifier = Modifier.testTag("grant_notification_permission_button")
                    ) {
                        Text("Allow")
                    }
                }
            }
        }

        PlaybackSwitchRow(
            title = "Background Playback",
            subtitle = "Keep foreground media session active outside the app",
            checked = preferences.backgroundPlaybackEnabled,
            onCheckedChange = onToggleBackgroundPlayback,
            testTag = "switch_background_playback"
        )
        HorizontalDivider()
        PlaybackSwitchRow(
            title = "Automatic Picture-in-Picture",
            subtitle = if (isPipSupported) {
                "Auto-enter floating PiP window when pressing Home"
            } else {
                "Picture-in-Picture is not supported on this device"
            },
            checked = preferences.autoPipEnabled && isPipSupported,
            enabled = isPipSupported,
            onCheckedChange = onToggleAutoPip,
            testTag = "switch_auto_pip"
        )
        HorizontalDivider()
        PlaybackSwitchRow(
            title = "Continue Audio in Background",
            subtitle = "Keep audio playing when PiP is dismissed or screen locks",
            checked = preferences.backgroundAudioEnabled,
            enabled = preferences.backgroundPlaybackEnabled,
            onCheckedChange = onToggleBackgroundAudio,
            testTag = "switch_background_audio"
        )
        HorizontalDivider()
        PlaybackSwitchRow(
            title = "Lock Screen Controls",
            subtitle = "Show anime artwork, Play/Pause, and Next/Prev on lock screen",
            checked = preferences.lockScreenControlsEnabled,
            enabled = preferences.backgroundPlaybackEnabled,
            onCheckedChange = onToggleLockScreenControls,
            testTag = "switch_lock_screen_controls"
        )
        HorizontalDivider()
        PlaybackSwitchRow(
            title = "Auto Next Episode",
            subtitle = "Automatically play the next episode when finished",
            checked = preferences.autoNextEpisodeEnabled,
            onCheckedChange = onToggleAutoNext,
            testTag = "switch_auto_next"
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PlaybackSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = VibrantOrange),
            modifier = Modifier.testTag(testTag)
        )
    }
}
