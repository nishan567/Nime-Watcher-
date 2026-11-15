package com.example.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ui.NimeGradientButton
import com.example.domain.model.AnimeFormat
import com.example.domain.model.AnimeSortOption
import com.example.domain.model.DiscoveryCategory
import com.example.domain.model.GenreDiscoveryCatalog
import com.example.domain.model.GenreDiscoveryPreference
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VibrantOrange
import com.example.ui.theme.WarmAmber

fun categoryAccentColor(accentKey: String): Color = when (accentKey) {
    DiscoveryCategory.ACCENT_AMBER -> WarmAmber
    DiscoveryCategory.ACCENT_EMERALD -> EmeraldSuccess
    DiscoveryCategory.ACCENT_CYAN -> Color(0xFF38BDF8)
    DiscoveryCategory.ACCENT_ROSE -> CrimsonError
    DiscoveryCategory.ACCENT_VIOLET -> Color(0xFFA78BFA)
    else -> VibrantOrange
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenrePreferencesBottomSheet(
    preferences: GenreDiscoveryPreference,
    onSavePreferences: (
        preferredTags: List<String>,
        sortBy: AnimeSortOption,
        formatFilter: AnimeFormat?,
        onlyShowPreferred: Boolean
    ) -> Unit,
    onResetDefaultCategories: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTags by remember(preferences) { mutableStateOf(preferences.preferredTags.toSet()) }
    var selectedSort by remember(preferences) { mutableStateOf(preferences.toSortOption()) }
    var selectedFormat by remember(preferences) { mutableStateOf(preferences.toFormatOrNull()) }
    var onlyPreferred by remember(preferences) { mutableStateOf(preferences.onlyShowPreferred) }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("genre_preferences_sheet")
    ) {
        Text(
            text = "Discovery Preferences (Cloud Synced)",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Choose your favorite anime tags, default ranking, and release format. Changes persist per user in Firestore.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Preferred Tags & Genres (${selectedTags.size}/25)",
            style = MaterialTheme.typography.titleSmall
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GenreDiscoveryCatalog.availableTagOptions.forEach { option ->
                val isSelected = selectedTags.any { it.equals(option.queryValue, ignoreCase = true) }
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedTags = if (isSelected) {
                            selectedTags.filterNot { it.equals(option.queryValue, ignoreCase = true) }.toSet()
                        } else if (selectedTags.size < 25) {
                            selectedTags + option.queryValue
                        } else {
                            selectedTags
                        }
                    },
                    label = { Text(option.displayName) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = VibrantOrange
                    ),
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }
        }

        HorizontalDivider()

        Text(text = "Default Sort Order", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AnimeSortOption.entries.forEach { option ->
                FilterChip(
                    selected = selectedSort == option,
                    onClick = { selectedSort = option },
                    label = { Text(option.displayLabel) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }
        }

        Text(text = "Format Filter", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFormat == null,
                onClick = { selectedFormat = null },
                label = { Text("All Formats") },
                modifier = Modifier.minimumInteractiveComponentSize()
            )
            listOf(
                AnimeFormat.TV,
                AnimeFormat.MOVIE,
                AnimeFormat.OVA,
                AnimeFormat.ONA,
                AnimeFormat.SPECIAL
            ).forEach { fmt ->
                FilterChip(
                    selected = selectedFormat == fmt,
                    onClick = { selectedFormat = fmt },
                    label = { Text(fmt.displayLabel) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Only Show Preferred Categories",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Hide non-preferred shelves in Overview mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = onlyPreferred,
                onCheckedChange = { onlyPreferred = it }
            )
        }

        OutlinedButton(
            onClick = {
                onResetDefaultCategories()
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Restore Starter Categories")
        }

        NimeGradientButton(
            text = "Save Preferences to Firestore",
            onClick = {
                onSavePreferences(
                    selectedTags.toList(),
                    selectedSort,
                    selectedFormat,
                    onlyPreferred
                )
                onDismiss()
            },
            testTag = "save_genre_preferences_button",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddDiscoveryCategorySheet(
    onCreateCategory: (
        name: String,
        queryValue: String,
        filterKind: String,
        accentKey: String,
        isPinned: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var queryValue by remember { mutableStateOf("") }
    var filterKind by remember { mutableStateOf(DiscoveryCategory.KIND_TAG) }
    var accentKey by remember { mutableStateOf(DiscoveryCategory.ACCENT_ORANGE) }
    var isPinned by remember { mutableStateOf(true) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("add_category_sheet")
    ) {
        Text(
            text = "Add Tag or Genre Category",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Create a custom discovery shelf stored in Firestore and powered by live anime catalog queries.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(text = "Quick Pick Popular Tags", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GenreDiscoveryCatalog.availableTagOptions.forEach { preset ->
                val selected = queryValue.equals(preset.queryValue, ignoreCase = true)
                FilterChip(
                    selected = selected,
                    onClick = {
                        queryValue = preset.queryValue
                        if (categoryName.isBlank() || GenreDiscoveryCatalog.availableTagOptions.any { it.displayName == categoryName }) {
                            categoryName = preset.displayName
                        }
                        filterKind = preset.filterKind
                        accentKey = preset.defaultAccent
                    },
                    label = { Text("${preset.displayName} (${preset.filterKind})") },
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }
        }

        OutlinedTextField(
            value = categoryName,
            onValueChange = { categoryName = it.take(50) },
            label = { Text("Category Display Title") },
            placeholder = { Text("e.g., Cyberpunk & Neon") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("category_name_input")
        )

        OutlinedTextField(
            value = queryValue,
            onValueChange = { queryValue = it.take(40) },
            label = { Text("Catalog Genre / Tag Query") },
            placeholder = { Text("e.g., Cyberpunk, Time Travel, Mecha") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("category_tag_input")
        )

        Text(text = "Query Type", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filterKind == DiscoveryCategory.KIND_GENRE,
                onClick = { filterKind = DiscoveryCategory.KIND_GENRE },
                label = { Text("Genre") },
                modifier = Modifier.minimumInteractiveComponentSize()
            )
            FilterChip(
                selected = filterKind == DiscoveryCategory.KIND_TAG,
                onClick = { filterKind = DiscoveryCategory.KIND_TAG },
                label = { Text("Tag (e.g., Isekai, Cyberpunk)") },
                modifier = Modifier.minimumInteractiveComponentSize()
            )
        }

        Text(text = "Accent Color", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DiscoveryCategory.ALLOWED_ACCENTS.forEach { key ->
                val color = categoryAccentColor(key)
                val selected = accentKey == key
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selected) 3.dp else 0.dp,
                            color = if (selected) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { accentKey = key }
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = key,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PushPin, contentDescription = null, tint = VibrantOrange, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pin Category to Top", style = MaterialTheme.typography.titleSmall)
            }
            Switch(checked = isPinned, onCheckedChange = { isPinned = it })
        }

        val canCreate = categoryName.trim().isNotEmpty() && queryValue.trim().isNotEmpty()
        Button(
            onClick = {
                onCreateCategory(
                    categoryName.trim(),
                    queryValue.trim(),
                    filterKind,
                    accentKey,
                    isPinned
                )
                onDismiss()
            },
            enabled = canCreate,
            colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("confirm_create_category_button")
        ) {
            Text("Save Category to Firestore", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
    }
}
