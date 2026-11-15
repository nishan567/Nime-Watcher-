package com.example.ui.anime

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.core.ui.NimeErrorStateCard
import com.example.core.ui.NimeGradientButton
import com.example.core.ui.ShimmerSkeletonBox
import com.example.domain.model.AnimeDetails
import com.example.domain.model.AnimeEpisode
import com.example.domain.model.AnimeSeries
import com.example.domain.model.UiState
import com.example.ui.components.AnimePosterCard
import com.example.ui.theme.NimeGradients
import com.example.ui.theme.VibrantOrange
import com.example.ui.theme.WarmAmber
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailsScreen(
    viewModel: AnimeDetailsViewModel,
    favoriteIds: Set<String>,
    watchLaterIds: Set<String>,
    onWatchEpisode: (seasonId: String, episodeNumber: Int) -> Unit,
    onSelectRelatedAnime: (AnimeSeries) -> Unit,
    onSelectGenreSearch: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val detailsState by viewModel.detailsState.collectAsStateWithLifecycle()
    val selectedSeasonId by viewModel.selectedSeasonId.collectAsStateWithLifecycle()
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val isWatchLater by viewModel.isWatchLater.collectAsStateWithLifecycle()

    var expandDescription by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.testTag("anime_details_screen")
    ) { padding ->
        when (val state = detailsState) {
            is UiState.Loading -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                        Text("Loading Anime…", style = MaterialTheme.typography.titleMedium)
                    }
                    ShimmerSkeletonBox(modifier = Modifier.fillMaxWidth().height(230.dp))
                    ShimmerSkeletonBox(modifier = Modifier.fillMaxWidth().height(120.dp))
                    ShimmerSkeletonBox(modifier = Modifier.fillMaxWidth().height(180.dp))
                }
            }

            is UiState.Error -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    NimeErrorStateCard(
                        message = state.message,
                        onRetry = { viewModel.loadDetails() }
                    )
                }
            }

            is UiState.Empty -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    NimeErrorStateCard(
                        message = "Anime metadata not found.",
                        onRetry = { viewModel.loadDetails() }
                    )
                }
            }

            is UiState.Success -> {
                val details: AnimeDetails = state.data
                val series = details.series

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Cover Art Header with Back + Share
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        ) {
                            AsyncImage(
                                model = series.bannerUrl ?: series.posterUrl ?: R.drawable.img_hero_cyber_blade,
                                contentDescription = series.title,
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .minimumInteractiveComponentSize()
                                        .testTag("details_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = { shareAnime(context, series) },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .minimumInteractiveComponentSize()
                                        .testTag("details_share_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = stringResource(R.string.share_anime),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Poster + Title + Metadata
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            AsyncImage(
                                model = series.posterUrl ?: R.drawable.img_poster_fallback,
                                contentDescription = series.title,
                                placeholder = painterResource(id = R.drawable.img_poster_fallback),
                                error = painterResource(id = R.drawable.img_poster_fallback),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(114.dp)
                                    .aspectRatio(3f / 4f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        MaterialTheme.shapes.medium
                                    )
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = series.title,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (series.alternativeTitles.isNotEmpty()) {
                                    Text(
                                        text = series.alternativeTitles.take(2).joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    series.verifiedRating?.let { rating ->
                                        MetadataPill(
                                            text = "★ ${String.format(Locale.US, "%.1f", rating)}/10",
                                            accent = WarmAmber
                                        )
                                    }
                                    series.releaseYear?.let { year ->
                                        MetadataPill(text = year.toString())
                                    }
                                    MetadataPill(text = series.status.displayLabel)
                                    series.episodeCount?.let { count ->
                                        MetadataPill(text = "$count eps")
                                    }
                                }

                                Text(
                                    text = "Audio: ${series.audioLanguages.joinToString(", ")} • ${series.subOrDubLabel ?: "Metadata Only"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Action Buttons: Watch Now, Favorites, Watch Later, Share
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            NimeGradientButton(
                                text = stringResource(R.string.watch_now),
                                onClick = {
                                    val targetSeason = selectedSeasonId ?: series.id
                                    val firstEpNum = episodes.firstOrNull()?.episodeNumber ?: 1
                                    onWatchEpisode(targetSeason, firstEpNum)
                                },
                                icon = Icons.Rounded.PlayArrow,
                                testTag = "details_watch_now_button"
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.toggleFavorite(series) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("details_favorite_button")
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (isFavorite) VibrantOrange else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isFavorite) stringResource(R.string.in_favorites) else stringResource(R.string.add_to_favorites),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewModel.toggleWatchLater(series) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("details_watch_later_button")
                                ) {
                                    Icon(
                                        imageVector = if (isWatchLater) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (isWatchLater) WarmAmber else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isWatchLater) stringResource(R.string.in_watch_later) else stringResource(R.string.add_to_watch_later),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Genres
                    if (series.genres.isNotEmpty()) {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(series.genres) { genre ->
                                    AssistChip(
                                        onClick = { onSelectGenreSearch(genre) },
                                        label = { Text(genre) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    )
                                }
                            }
                        }
                    }

                    // Synopsis
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Text(
                                    text = "Synopsis",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = series.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (expandDescription) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (series.description.length > 160) {
                                    TextButton(
                                        onClick = { expandDescription = !expandDescription },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = if (expandDescription) "Show less" else "Read full synopsis",
                                            color = VibrantOrange
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Metadata vs Stream Provider Separation Notice
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = VibrantOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${details.providerDisplayName}: ${details.streamConfigurationNotice}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // Season Switcher + Episodes
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (details.seasons.size > 1) "Seasons & Entries" else stringResource(R.string.episodes_title),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(details.seasons, key = { it.seasonId }) { season ->
                                    FilterChip(
                                        selected = selectedSeasonId == season.seasonId,
                                        onClick = { viewModel.selectSeason(season) },
                                        label = {
                                            val countSuffix = season.episodeCount?.let { " ($it)" } ?: ""
                                            Text("${season.label}$countSuffix")
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = VibrantOrange
                                        ),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("season_chip_${season.seasonId}")
                                    )
                                }
                            }
                        }
                    }

                    if (episodes.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "Episode count has not been announced by the catalog provider yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(episodes, key = { it.episodeId }) { episode ->
                            EpisodePreviewRow(
                                episode = episode,
                                onClick = {
                                    onWatchEpisode(episode.seasonId, episode.episodeNumber)
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    // Related & Sequels
                    if (details.relatedAnime.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = stringResource(R.string.related_anime_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(details.relatedAnime, key = { "rel_${it.id}" }) { relAnime ->
                                        AnimePosterCard(
                                            anime = relAnime,
                                            isFavorite = favoriteIds.contains(relAnime.id),
                                            isWatchLater = watchLaterIds.contains(relAnime.id),
                                            onClick = { onSelectRelatedAnime(relAnime) },
                                            cardWidth = 136.dp
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
}

@Composable
private fun MetadataPill(
    text: String,
    accent: Color? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = accent ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EpisodePreviewRow(
    episode: AnimeEpisode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("episode_row_${episode.episodeNumber}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = episode.episodeNumber.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = VibrantOrange
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (episode.authorizedStreamUrl != null) {
                        "Authorized Stream Ready"
                    } else {
                        "Metadata verified • Stream provider attaches in Stage 4"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.PlayCircle,
                contentDescription = "Preview episode ${episode.episodeNumber}",
                tint = VibrantOrange,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun shareAnime(context: Context, series: AnimeSeries) {
    val genresLine = if (series.genres.isNotEmpty()) "\nGenres: ${series.genres.take(4).joinToString(", ")}" else ""
    val yearLine = series.releaseYear?.let { " ($it)" } ?: ""
    val shareText = "Check out ${series.title}$yearLine on Nime Watcher!$genresLine"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, series.title)
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share ${series.title}"))
}
