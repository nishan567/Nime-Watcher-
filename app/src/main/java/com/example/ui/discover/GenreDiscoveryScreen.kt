package com.example.ui.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.core.ui.NimeErrorStateCard
import com.example.core.ui.ShimmerSkeletonBox
import com.example.domain.model.AnimeSeries
import com.example.domain.model.AnimeSortOption
import com.example.domain.model.DiscoveryCategory
import com.example.domain.model.GenreDiscoveryCatalog
import com.example.domain.model.GenreDiscoveryPreference
import com.example.ui.components.AnimePosterCard
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NimeGradients
import com.example.ui.theme.VibrantOrange
import com.example.ui.theme.WarmAmber
import java.util.Locale

private enum class ActiveDiscoverySheet {
    NONE, PREFERENCES, ADD_CATEGORY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreDiscoveryScreen(
    viewModel: GenreDiscoveryViewModel,
    favoriteIds: Set<String>,
    watchLaterIds: Set<String>,
    onAnimeClick: (AnimeSeries) -> Unit,
    onBack: () -> Unit
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val shelfMap by viewModel.shelfMap.collectAsStateWithLifecycle()
    val focusedState by viewModel.focusedState.collectAsStateWithLifecycle()
    val isInitializing by viewModel.isInitializing.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val firestoreError by viewModel.firestoreError.collectAsStateWithLifecycle()

    var activeSheet by remember { mutableStateOf(ActiveDiscoverySheet.NONE) }

    val preferredSet = remember(preferences.preferredTags) {
        preferences.preferredTags.map { it.lowercase() }.toSet()
    }
    val orderedCategories = remember(categories, preferredSet, preferences.onlyShowPreferred) {
        val sorted = categories.sortedWith(
            compareByDescending<DiscoveryCategory> { it.isPinned }
                .thenByDescending { preferredSet.contains(it.queryValue.lowercase()) }
                .thenBy { it.sortOrder }
                .thenBy { it.name.lowercase() }
        )
        if (preferences.onlyShowPreferred && preferredSet.isNotEmpty()) {
            sorted.filter { preferredSet.contains(it.queryValue.lowercase()) }.ifEmpty { sorted }
        } else {
            sorted
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Genre & Tag Discovery",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${preferences.toSortOption().displayLabel} • ${
                                preferences.toFormatOrNull()?.displayLabel ?: "All Formats"
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibrantOrange
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("genre_discovery_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshActive() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("genre_discovery_refresh_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh shelves")
                    }
                    IconButton(
                        onClick = { activeSheet = ActiveDiscoverySheet.PREFERENCES },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("genre_discovery_preferences_button")
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = "Manage preferences")
                    }
                    IconButton(
                        onClick = { activeSheet = ActiveDiscoverySheet.ADD_CATEGORY },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("genre_discovery_add_category_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add custom tag category", tint = VibrantOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("genre_discovery_screen")
        ) {
            // Sync & Status Strip
            AnimatedVisibility(visible = statusMessage != null || firestoreError != null) {
                val displayMsg = firestoreError ?: statusMessage.orEmpty()
                Surface(
                    color = if (firestoreError != null) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = displayMsg,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (firestoreError != null) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.dismissStatusMessage() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Atmospheric Preferences Header + Firestore Tag Controls
            DiscoveryControlsHeader(
                preferences = preferences,
                categories = orderedCategories,
                selectedCategoryId = selectedCategoryId,
                onTogglePreferredTag = viewModel::togglePreferredTag,
                onToggleOnlyPreferred = viewModel::toggleOnlyShowPreferred,
                onCycleSort = {
                    val entries = AnimeSortOption.entries
                    val nextIdx = (entries.indexOf(preferences.toSortOption()) + 1) % entries.size
                    viewModel.updateSortOption(entries[nextIdx])
                },
                onSelectCategory = { id -> viewModel.selectCategory(id) },
                onOpenPreferences = { activeSheet = ActiveDiscoverySheet.PREFERENCES },
                onOpenAddCategory = { activeSheet = ActiveDiscoverySheet.ADD_CATEGORY }
            )

            if (isInitializing && orderedCategories.isEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    repeat(3) {
                        ShimmerSkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }
            } else if (selectedCategoryId != null || focusedState.category != null && selectedCategoryId == null && focusedState.items.isNotEmpty() && orderedCategories.none { it.id == focusedState.category?.id }) {
                // Focused Tag Grid Mode
                FocusedCategoryGrid(
                    state = focusedState,
                    favoriteIds = favoriteIds,
                    watchLaterIds = watchLaterIds,
                    onAnimeClick = onAnimeClick,
                    onLoadMore = viewModel::loadNextFocusedPage,
                    onBackToOverview = { viewModel.selectCategory(null) },
                    onRetry = viewModel::refreshActive
                )
            } else {
                // Overview Mode: Spotlight + Categorized Tag Shelves
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("genre_discovery_shelves_list")
                ) {
                    val spotlightCandidate = orderedCategories
                        .firstNotNullOfOrNull { cat -> shelfMap[cat.id]?.items?.firstOrNull()?.let { cat to it } }
                    if (spotlightCandidate != null) {
                        item(key = "spotlight_tag_hero") {
                            TagSpotlightCard(
                                category = spotlightCandidate.first,
                                anime = spotlightCandidate.second,
                                onClick = { onAnimeClick(spotlightCandidate.second) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    if (orderedCategories.isEmpty()) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                Text(
                                    text = "No Discovery Categories Yet",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                OutlinedButton(onClick = { activeSheet = ActiveDiscoverySheet.ADD_CATEGORY }) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Create Tag Category")
                                }
                            }
                        }
                    } else {
                        items(orderedCategories, key = { it.id }) { category ->
                            val content = shelfMap[category.id]
                            GenreCategoryShelf(
                                category = category,
                                content = content,
                                isPreferred = preferredSet.contains(category.queryValue.lowercase()),
                                canDelete = categories.size > 1,
                                favoriteIds = favoriteIds,
                                watchLaterIds = watchLaterIds,
                                onAnimeClick = onAnimeClick,
                                onOpenGrid = { viewModel.selectCategory(category.id) },
                                onTogglePin = { viewModel.toggleCategoryPinned(category) },
                                onDeleteCategory = { viewModel.deleteCategory(category) },
                                onRetry = viewModel::refreshActive
                            )
                        }
                    }
                }
            }
        }
    }

    if (activeSheet != ActiveDiscoverySheet.NONE) {
        ModalBottomSheet(onDismissRequest = { activeSheet = ActiveDiscoverySheet.NONE }) {
            when (activeSheet) {
                ActiveDiscoverySheet.PREFERENCES -> GenrePreferencesBottomSheet(
                    preferences = preferences,
                    onSavePreferences = viewModel::saveFullPreferences,
                    onResetDefaultCategories = viewModel::resetDefaultCategories,
                    onDismiss = { activeSheet = ActiveDiscoverySheet.NONE }
                )
                ActiveDiscoverySheet.ADD_CATEGORY -> AddDiscoveryCategorySheet(
                    onCreateCategory = viewModel::createCategory,
                    onDismiss = { activeSheet = ActiveDiscoverySheet.NONE }
                )
                ActiveDiscoverySheet.NONE -> Unit
            }
        }
    }
}

@Composable
private fun DiscoveryControlsHeader(
    preferences: GenreDiscoveryPreference,
    categories: List<DiscoveryCategory>,
    selectedCategoryId: String?,
    onTogglePreferredTag: (String) -> Unit,
    onToggleOnlyPreferred: () -> Unit,
    onCycleSort: () -> Unit,
    onSelectCategory: (String?) -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenAddCategory: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        VibrantOrange.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(top = 6.dp, bottom = 10.dp)
    ) {
        // Cloud Sync Badge + Sort / Preferred Controls
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDone,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Firestore Synced",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                FilterChip(
                    selected = preferences.onlyShowPreferred,
                    onClick = onToggleOnlyPreferred,
                    label = { Text("Preferred Only (${preferences.preferredTags.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = VibrantOrange
                    ),
                    modifier = Modifier.testTag("toggle_only_preferred_chip")
                )
            }
            item {
                AssistChip(
                    onClick = onCycleSort,
                    label = { Text("Sort: ${preferences.toSortOption().displayLabel}") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("cycle_sort_chip")
                )
            }
            item {
                AssistChip(
                    onClick = onOpenPreferences,
                    leadingIcon = { Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = { Text("Preferences") }
                )
            }
        }

        // Preferred Tags Quick-Toggle Strip
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Your Favorite Tags (Tap to Save in Firestore)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GenreDiscoveryCatalog.availableTagOptions, key = { "pref_${it.queryValue}" }) { opt ->
                    val isPreferred = preferences.preferredTags.any {
                        it.equals(opt.queryValue, ignoreCase = true)
                    }
                    FilterChip(
                        selected = isPreferred,
                        onClick = { onTogglePreferredTag(opt.queryValue) },
                        label = { Text(opt.displayName) },
                        leadingIcon = if (isPreferred) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = VibrantOrange,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = VibrantOrange
                        ),
                        modifier = Modifier.testTag("pref_tag_${opt.queryValue.lowercase()}")
                    )
                }
            }
        }

        // Firestore Categories Switcher Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { onSelectCategory(null) },
                    label = { Text("All Shelves (${categories.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VibrantOrange,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.testTag("category_chip_all")
                )
            }
            items(categories, key = { "tab_${it.id}" }) { cat ->
                val accent = categoryAccentColor(cat.accentKey)
                val selected = selectedCategoryId == cat.id
                FilterChip(
                    selected = selected,
                    onClick = { onSelectCategory(if (selected) null else cat.id) },
                    leadingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) Color.Black else accent)
                            )
                            if (cat.isPinned) {
                                Icon(
                                    imageVector = Icons.Rounded.PushPin,
                                    contentDescription = "Pinned",
                                    tint = if (selected) Color.Black else accent,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    },
                    label = { Text(cat.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accent,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.testTag("category_chip_${cat.queryValue.lowercase()}")
                )
            }
            item {
                AssistChip(
                    onClick = onOpenAddCategory,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = VibrantOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("New Category") },
                    modifier = Modifier.testTag("inline_add_category_chip")
                )
            }
        }
    }
}

@Composable
private fun TagSpotlightCard(
    category: DiscoveryCategory,
    anime: AnimeSeries,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = categoryAccentColor(category.accentKey)
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.55f), MaterialTheme.shapes.large)
            .clickable { onClick() }
            .testTag("tag_spotlight_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            AsyncImage(
                model = anime.bannerUrl ?: anime.posterUrl ?: R.drawable.img_hero_cyber_blade,
                contentDescription = anime.title,
                placeholder = painterResource(id = R.drawable.img_hero_cyber_blade),
                error = painterResource(id = R.drawable.img_hero_cyber_blade),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NimeGradients.darkScrimBottom)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Surface(color = accent, shape = RoundedCornerShape(50)) {
                    Text(
                        text = "${category.name} Pick",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                anime.verifiedRating?.let { rating ->
                    Surface(color = Color.Black.copy(alpha = 0.72f), shape = RoundedCornerShape(50)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Rounded.Star, contentDescription = null, tint = WarmAmber, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f", rating),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(
                    text = anime.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val tagBadges = (anime.genres.take(2) + anime.tags.take(2)).distinct().take(4)
                if (tagBadges.isNotEmpty()) {
                    Text(
                        text = tagBadges.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmAmber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun GenreCategoryShelf(
    category: DiscoveryCategory,
    content: CategoryShelfContent?,
    isPreferred: Boolean,
    canDelete: Boolean,
    favoriteIds: Set<String>,
    watchLaterIds: Set<String>,
    onAnimeClick: (AnimeSeries) -> Unit,
    onOpenGrid: () -> Unit,
    onTogglePin: () -> Unit,
    onDeleteCategory: () -> Unit,
    onRetry: () -> Unit
) {
    val accent = categoryAccentColor(category.accentKey)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("genre_shelf_${category.queryValue.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accent)
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isPreferred) {
                            Surface(
                                color = VibrantOrange.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Preferred",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VibrantOrange,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Label,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${category.filterKind.uppercase()} • ${category.queryValue}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = if (category.isPinned) "Unpin category" else "Pin category",
                        tint = if (category.isPinned) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (canDelete) {
                    IconButton(
                        onClick = onDeleteCategory,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete category",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                TextButton(
                    onClick = onOpenGrid,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Grid", color = VibrantOrange)
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = VibrantOrange,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        when {
            content == null || content.isLoading -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(4) {
                        ShimmerSkeletonBox(
                            modifier = Modifier
                                .width(136.dp)
                                .height(200.dp)
                        )
                    }
                }
            }
            content.errorMessage != null && content.items.isEmpty() -> {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    NimeErrorStateCard(
                        message = content.errorMessage,
                        onRetry = onRetry
                    )
                }
            }
            content.items.isEmpty() -> {
                Text(
                    text = "No titles matched \"${category.queryValue}\" with the current format filter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            else -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(content.items, key = { "${category.id}_${it.id}" }) { anime ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.width(142.dp)
                        ) {
                            AnimePosterCard(
                                anime = anime,
                                isFavorite = favoriteIds.contains(anime.id),
                                isWatchLater = watchLaterIds.contains(anime.id),
                                onClick = { onAnimeClick(anime) },
                                cardWidth = 142.dp
                            )
                            val tagLine = anime.tags.firstOrNull { !it.equals(category.queryValue, true) }
                                ?: anime.genres.firstOrNull()
                            if (!tagLine.isNullOrBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "#$tagLine",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusedCategoryGrid(
    state: FocusedCategoryState,
    favoriteIds: Set<String>,
    watchLaterIds: Set<String>,
    onAnimeClick: (AnimeSeries) -> Unit,
    onLoadMore: () -> Unit,
    onBackToOverview: () -> Unit,
    onRetry: () -> Unit
) {
    val category = state.category
    Column(modifier = Modifier.fillMaxSize()) {
        if (category != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${category.filterKind.uppercase()}: ${category.queryValue}${if (state.fromCache) " • Offline Cache" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryAccentColor(category.accentKey)
                    )
                }
                TextButton(onClick = onBackToOverview) {
                    Text("All Shelves", color = VibrantOrange)
                }
            }
        }

        when {
            state.isLoading && state.items.isEmpty() -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(color = VibrantOrange)
                }
            }
            state.errorMessage != null && state.items.isEmpty() -> {
                Box(modifier = Modifier.padding(16.dp)) {
                    NimeErrorStateCard(
                        message = state.errorMessage,
                        onRetry = onRetry
                    )
                }
            }
            state.items.isEmpty() -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "No anime found for this tag & filter combination.",
                            style = MaterialTheme.typography.titleSmall
                        )
                        OutlinedButton(onClick = onBackToOverview) {
                            Text("View All Shelves")
                        }
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("focused_genre_grid")
                ) {
                    gridItems(state.items, key = { it.id }) { anime ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AnimePosterCard(
                                anime = anime,
                                isFavorite = favoriteIds.contains(anime.id),
                                isWatchLater = watchLaterIds.contains(anime.id),
                                onClick = { onAnimeClick(anime) }
                            )
                            val chips = (anime.genres.take(1) + anime.tags.take(2)).distinct().take(2)
                            if (chips.isNotEmpty()) {
                                Text(
                                    text = chips.joinToString(" • ") { "#$it" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VibrantOrange,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (state.hasNextPage) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                if (state.isLoadingMore) {
                                    CircularProgressIndicator(color = VibrantOrange)
                                } else {
                                    OutlinedButton(
                                        onClick = onLoadMore,
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Text("Load More Anime")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
