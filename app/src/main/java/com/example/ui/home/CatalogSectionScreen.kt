package com.example.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.NimeErrorStateCard
import com.example.domain.model.AnimeSeries
import com.example.domain.model.CatalogSectionType
import com.example.ui.components.AnimePosterCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogSectionScreen(
    section: CatalogSectionType,
    viewModel: SectionPaginationViewModel,
    favoriteIds: Set<String>,
    watchLaterIds: Set<String>,
    onAnimeClick: (AnimeSeries) -> Unit,
    onBack: () -> Unit
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val hasNextPage by viewModel.hasNextPage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(section.title) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("section_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when {
            isLoading && items.isEmpty() -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null && items.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    NimeErrorStateCard(
                        message = errorMessage ?: "Unable to load section.",
                        onRetry = { viewModel.refresh() }
                    )
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
                        .padding(padding)
                        .testTag("catalog_section_grid")
                ) {
                    items(items, key = { it.id }) { anime ->
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
                                    .padding(vertical = 12.dp)
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator()
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.loadNextPage() },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("section_load_more_button")
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
