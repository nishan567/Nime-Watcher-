package com.example.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.ui.NimeBrandMark
import com.example.core.ui.NimeErrorStateCard
import com.example.core.ui.ShimmerSkeletonBox
import com.example.data.local.PlaybackProgressEntity
import com.example.data.repository.HomeCatalogPayload
import com.example.domain.model.AnimeGenreCatalog
import com.example.domain.model.AnimeSeries
import com.example.domain.model.CatalogSectionType
import com.example.domain.model.UiState
import com.example.domain.model.UserProfile
import com.example.ui.components.HeroDiscoveryBanner
import com.example.ui.components.HomeSectionCarousel
import com.example.ui.components.UserAvatarView
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VibrantOrange
import com.example.ui.theme.WarmAmber

private val HomeOrderedSections = listOf(
    CatalogSectionType.TRENDING,
    CatalogSectionType.POPULAR,
    CatalogSectionType.RECENTLY_ADDED,
    CatalogSectionType.RECENTLY_UPDATED,
    CatalogSectionType.MOST_WATCHED,
    CatalogSectionType.NEW_EPISODES,
    CatalogSectionType.RECOMMENDED,
    CatalogSectionType.ACTION,
    CatalogSectionType.ADVENTURE,
    CatalogSectionType.ISEKAI,
    CatalogSectionType.FANTASY,
    CatalogSectionType.ROMANCE,
    CatalogSectionType.COMEDY,
    CatalogSectionType.DRAMA,
    CatalogSectionType.MYSTERY,
    CatalogSectionType.SUPERNATURAL
)

@Composable
fun HomeDiscoveryScreen(
    profileState: UiState<UserProfile>,
    homeState: UiState<HomeCatalogPayload>,
    favoriteIds: Set<String>,
    favoritesList: List<AnimeSeries>,
    continueWatchingList: List<PlaybackProgressEntity>,
    onRetryCatalog: () -> Unit,
    onAnimeClick: (AnimeSeries) -> Unit,
    onResumePlayback: (PlaybackProgressEntity) -> Unit,
    onToggleFavorite: (AnimeSeries) -> Unit,
    onSeeAllSection: (CatalogSectionType) -> Unit,
    onSelectGenreSearch: (String) -> Unit,
    onOpenGenreDiscovery: (String?) -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onCompleteRegistration: () -> Unit,
    onOpenFavoritesTab: () -> Unit
) {
    val profile = (profileState as? UiState.Success)?.data
    var selectedHeroIdx by remember { mutableIntStateOf(0) }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_discovery_screen")
    ) {
        // Brand Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NimeBrandMark(size = 38.dp)
                    Column {
                        Text(
                            text = stringResource(R.string.brand_wordmark),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onOpenGenreDiscovery(null) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("home_genre_hub_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Category,
                            contentDescription = "Genre & Tag Discovery",
                            tint = VibrantOrange
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("home_notifications_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications & Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = onOpenProfile,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("home_profile_avatar_button")
                    ) {
                        UserAvatarView(
                            avatarId = profile?.avatarId ?: UserProfile.DEFAULT_AVATAR_ID,
                            photoUrl = profile?.photoUrl,
                            customAvatarPath = profile?.customAvatarPath,
                            size = 36.dp,
                            borderWidth = 1.5.dp
                        )
                    }
                }
            }
        }

        // Registration completion prompt from Stage 2 if incomplete
        if (profile != null && !profile.profileCompleted) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { onCompleteRegistration() }
                        .testTag("home_complete_registration_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Badge,
                            contentDescription = null,
                            tint = VibrantOrange
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Complete Your Nime Watcher Profile",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Add your full name, date of birth & pick an anime avatar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        when (homeState) {
            is UiState.Loading -> {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        ShimmerSkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(270.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            repeat(3) {
                                ShimmerSkeletonBox(
                                    modifier = Modifier
                                        .width(136.dp)
                                        .height(200.dp)
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            repeat(3) {
                                ShimmerSkeletonBox(
                                    modifier = Modifier
                                        .width(136.dp)
                                        .height(200.dp)
                                )
                            }
                        }
                    }
                }
            }

            is UiState.Error -> {
                item {
                    BoxErrorWrapper(
                        message = homeState.message,
                        onRetry = onRetryCatalog
                    )
                }
            }

            is UiState.Empty -> {
                item {
                    BoxErrorWrapper(
                        message = "No anime titles available right now.",
                        onRetry = onRetryCatalog
                    )
                }
            }

            is UiState.Success -> {
                val payload = homeState.data
                val heroCandidates = (payload.sections[CatalogSectionType.TRENDING]
                    ?: payload.sections[CatalogSectionType.POPULAR]
                    ?: emptyList()).take(5)

                if (heroCandidates.isNotEmpty()) {
                    val safeIdx = selectedHeroIdx.coerceIn(0, heroCandidates.lastIndex)
                    val featured = heroCandidates[safeIdx]
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            HeroDiscoveryBanner(
                                featured = featured,
                                isFavorite = favoriteIds.contains(featured.id),
                                onWatchNow = { onAnimeClick(featured) },
                                onToggleFavorite = { onToggleFavorite(featured) }
                            )
                            if (heroCandidates.size > 1) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(heroCandidates.size) { index ->
                                        FilterChip(
                                            selected = safeIdx == index,
                                            onClick = { selectedHeroIdx = index },
                                            label = { Text("#${index + 1}") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = VibrantOrange
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Provider status badge
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudDone,
                                    contentDescription = null,
                                    tint = if (payload.fromCache) WarmAmber else EmeraldSuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = payload.providerName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(
                            onClick = onRetryCatalog,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Refresh")
                        }
                    }
                }

                // Genre Quick Filter Row + Genre Discovery Hub Trigger
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "Explore Genres & Tags",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(
                                onClick = { onOpenGenreDiscovery(null) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_open_genre_discovery_button")
                            ) {
                                Text("Genre Hub", color = VibrantOrange)
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = VibrantOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(AnimeGenreCatalog.searchFilterGenres) { genre ->
                                AssistChip(
                                    onClick = { onOpenGenreDiscovery(genre) },
                                    label = { Text(genre) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("home_genre_chip_${genre.lowercase()}")
                                )
                            }
                        }
                    }
                }

                // Continue Watching (Real Room-backed playback resume cards)
                if (continueWatchingList.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Continue Watching",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(continueWatchingList, key = { "cw_${it.animeId}" }) { progress ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = MaterialTheme.shapes.medium,
                                        modifier = Modifier
                                            .width(230.dp)
                                            .clickable { onResumePlayback(progress) }
                                            .testTag("continue_watching_${progress.animeId}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.padding(10.dp)
                                        ) {
                                            AsyncImage(
                                                model = progress.posterUrl ?: R.drawable.img_poster_fallback,
                                                contentDescription = progress.animeTitle,
                                                placeholder = painterResource(id = R.drawable.img_poster_fallback),
                                                error = painterResource(id = R.drawable.img_poster_fallback),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .width(56.dp)
                                                    .height(74.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = progress.animeTitle,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = progress.episodeTitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = VibrantOrange
                                                )
                                                val ratio = if (progress.durationMs > 0L) {
                                                    (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
                                                } else 0f
                                                LinearProgressIndicator(
                                                    progress = { ratio },
                                                    color = VibrantOrange,
                                                    trackColor = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayCircleOutline,
                                    contentDescription = null,
                                    tint = VibrantOrange,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Continue Watching",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Start watching any episode in Media3 ExoPlayer to automatically save & resume your progress here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // User's Saved Favorites Shelf (real data when user toggles Favorites)
                if (favoritesList.isNotEmpty()) {
                    item {
                        HomeSectionCarousel(
                            title = "Your Favorites",
                            items = favoritesList,
                            favoriteIds = favoriteIds,
                            onAnimeClick = onAnimeClick,
                            onSeeAllClick = onOpenFavoritesTab
                        )
                    }
                }

                // All Catalog Discovery Shelves
                HomeOrderedSections.forEach { sectionType ->
                    val items = payload.sections[sectionType].orEmpty()
                    if (items.isNotEmpty()) {
                        item(key = "section_${sectionType.key}") {
                            HomeSectionCarousel(
                                title = sectionType.title,
                                items = items,
                                favoriteIds = favoriteIds,
                                onAnimeClick = onAnimeClick,
                                onSeeAllClick = { onSeeAllSection(sectionType) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxErrorWrapper(
    message: String,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        NimeErrorStateCard(
            message = message,
            onRetry = onRetry
        )
    }
}
