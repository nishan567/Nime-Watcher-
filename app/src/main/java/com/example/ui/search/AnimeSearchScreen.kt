package com.example.ui.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.ui.NimeErrorStateCard
import com.example.core.ui.ShimmerSkeletonBox
import com.example.domain.model.AnimeFormat
import com.example.domain.model.AnimeGenreCatalog
import com.example.domain.model.AnimeReleaseSeason
import com.example.domain.model.AnimeSearchFilter
import com.example.domain.model.AnimeSeries
import com.example.domain.model.AnimeSortOption
import com.example.domain.model.AudioFilter
import com.example.ui.components.AnimePosterCard
import com.example.ui.theme.VibrantOrange

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AnimeSearchScreen(
    viewModel: AnimeSearchViewModel,
    favoriteIds: Set<String>,
    watchLaterIds: Set<String>,
    onAnimeClick: (AnimeSeries) -> Unit,
    onOpenGenreDiscovery: () -> Unit = {}
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val hasNextPage by viewModel.hasNextPage.collectAsStateWithLifecycle()
    val fromCache by viewModel.fromCache.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current
    var showFilterSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("anime_search_screen")
    ) {
        // Top Search Bar + Filter Button
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.nav_search),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (filter.hasActiveFilters || filter.query.isNotBlank()) {
                        TextButton(
                            onClick = { viewModel.resetFilters() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("search_reset_filters_button")
                        ) {
                            Text("Reset", color = VibrantOrange)
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenGenreDiscovery,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("search_open_genre_discovery_button")
                    ) {
                        Icon(Icons.Rounded.Category, contentDescription = null, tint = VibrantOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Genres")
                    }
                    OutlinedButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("search_open_filters_button")
                    ) {
                        Icon(Icons.Rounded.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Filters")
                    }
                }
            }

            OutlinedTextField(
                value = filter.query,
                onValueChange = { viewModel.onQueryChanged(it) },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (filter.query.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onQueryChanged("") },
                            modifier = Modifier.testTag("search_clear_input_button")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        viewModel.submitQuery()
                    }
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_query_input")
            )

            // Live search suggestions dropdown chips
            if (suggestions.isNotEmpty() && filter.query.isNotBlank()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { suggestion ->
                        AssistChip(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.submitQuery(suggestion)
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(suggestion) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }

            // Genre filter chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filter.selectedGenre == null,
                        onClick = { viewModel.selectGenre(null) },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = VibrantOrange
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("search_genre_all")
                    )
                }
                items(AnimeGenreCatalog.searchFilterGenres) { genre ->
                    FilterChip(
                        selected = filter.selectedGenre == genre,
                        onClick = { viewModel.selectGenre(genre) },
                        label = { Text(genre) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = VibrantOrange
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("search_genre_${genre.lowercase()}")
                    )
                }
            }

            // Active sort/filter summary pill row
            val activeSummary = buildList {
                filter.releaseYear?.let { add("Year $it") }
                filter.season?.let { add(it.displayLabel) }
                filter.format?.let { add(it.displayLabel) }
                if (filter.audioFilter != AudioFilter.ALL) add(filter.audioFilter.displayLabel)
                add("Sort: ${filter.sortBy.displayLabel}")
                if (fromCache) add("Offline Cache")
            }
            Text(
                text = activeSummary.joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Scrollable grid with history/trending header + results
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("search_results_grid")
        ) {
            if (filter.query.isBlank() && searchHistory.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Recent Searches", style = MaterialTheme.typography.titleSmall)
                            }
                            TextButton(
                                onClick = { viewModel.clearHistory() },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("search_clear_history_button")
                            ) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.clear_history))
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            searchHistory.forEach { term ->
                                AssistChip(
                                    onClick = { viewModel.submitQuery(term) },
                                    label = { Text(term) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove $term",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { viewModel.deleteHistoryItem(term) }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (filter.query.isBlank() && !filter.hasActiveFilters) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = VibrantOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Trending Searches", style = MaterialTheme.typography.titleSmall)
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AnimeGenreCatalog.trendingSearchKeywords) { keyword ->
                                AssistChip(
                                    onClick = { viewModel.submitQuery(keyword) },
                                    label = { Text(keyword) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                            }
                        }
                    }
                }
            }

            when {
                isLoading && results.isEmpty() -> {
                    items(6) {
                        ShimmerSkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        )
                    }
                }

                errorMessage != null && results.isEmpty() -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        NimeErrorStateCard(
                            message = errorMessage ?: "Search error.",
                            onRetry = { viewModel.retry() }
                        )
                    }
                }

                results.isEmpty() -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        NoSearchResultsCard(onReset = { viewModel.resetFilters() })
                    }
                }

                else -> {
                    items(results, key = { it.id }) { anime ->
                        AnimePosterCard(
                            anime = anime,
                            isFavorite = favoriteIds.contains(anime.id),
                            isWatchLater = watchLaterIds.contains(anime.id),
                            onClick = { onAnimeClick(anime) }
                        )
                    }
                    if (hasNextPage) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator()
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.loadNextPage() },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("search_load_more_button")
                                    ) {
                                        Text("Load More Results")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            SearchFiltersSheetContent(
                filter = filter,
                onSelectSort = viewModel::selectSort,
                onSelectYear = viewModel::selectYear,
                onSelectSeason = viewModel::selectSeason,
                onSelectFormat = viewModel::selectFormat,
                onSelectAudio = viewModel::selectAudioFilter,
                onReset = viewModel::resetFilters,
                onClose = { showFilterSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchFiltersSheetContent(
    filter: AnimeSearchFilter,
    onSelectSort: (AnimeSortOption) -> Unit,
    onSelectYear: (Int?) -> Unit,
    onSelectSeason: (AnimeReleaseSeason?) -> Unit,
    onSelectFormat: (AnimeFormat?) -> Unit,
    onSelectAudio: (AudioFilter) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
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
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Sort & Filter Catalog",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            TextButton(onClick = onReset) {
                Text("Reset All", color = VibrantOrange)
            }
        }

        Text("Sort By", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimeSortOption.entries.forEach { option ->
                FilterChip(
                    selected = filter.sortBy == option,
                    onClick = { onSelectSort(option) },
                    label = { Text(option.displayLabel) }
                )
            }
        }

        HorizontalDivider()
        Text("Release Season", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter.season == null,
                onClick = { onSelectSeason(null) },
                label = { Text("Any Season") }
            )
            AnimeReleaseSeason.entries.forEach { season ->
                FilterChip(
                    selected = filter.season == season,
                    onClick = { onSelectSeason(season) },
                    label = { Text(season.displayLabel) }
                )
            }
        }

        HorizontalDivider()
        Text("Format", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter.format == null,
                onClick = { onSelectFormat(null) },
                label = { Text("All Formats") }
            )
            listOf(
                AnimeFormat.TV,
                AnimeFormat.MOVIE,
                AnimeFormat.OVA,
                AnimeFormat.ONA,
                AnimeFormat.SPECIAL
            ).forEach { fmt ->
                FilterChip(
                    selected = filter.format == fmt,
                    onClick = { onSelectFormat(fmt) },
                    label = { Text(fmt.displayLabel) }
                )
            }
        }

        HorizontalDivider()
        Text("Audio / Subtitles", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioFilter.entries.forEach { audio ->
                FilterChip(
                    selected = filter.audioFilter == audio,
                    onClick = { onSelectAudio(audio) },
                    label = { Text(audio.displayLabel) }
                )
            }
        }

        HorizontalDivider()
        Text("Release Year", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = filter.releaseYear == null,
                    onClick = { onSelectYear(null) },
                    label = { Text("Any Year") }
                )
            }
            items(AnimeGenreCatalog.selectableYears) { year ->
                FilterChip(
                    selected = filter.releaseYear == year,
                    onClick = { onSelectYear(year) },
                    label = { Text(year.toString()) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("apply_search_filters_button")
        ) {
            Text("Show Results", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun NoSearchResultsCard(onReset: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_no_results_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_search_anime),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            Text(
                text = stringResource(R.string.no_results_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.no_results_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(stringResource(R.string.reset_filters))
            }
        }
    }
}
