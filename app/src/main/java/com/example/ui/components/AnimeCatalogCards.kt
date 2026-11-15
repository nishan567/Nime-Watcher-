package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.ui.NimeGradientButton
import com.example.domain.model.AnimeSeries
import com.example.ui.theme.NimeGradients
import com.example.ui.theme.VibrantOrange
import com.example.ui.theme.WarmAmber
import java.util.Locale

@Composable
fun HeroDiscoveryBanner(
    featured: AnimeSeries,
    isFavorite: Boolean,
    onWatchNow: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NimeGradients.cardGlowBorder, MaterialTheme.shapes.large)
            .clickable { onWatchNow() }
            .testTag("hero_featured_banner")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            AsyncImage(
                model = featured.bannerUrl ?: featured.posterUrl ?: R.drawable.img_hero_cyber_blade,
                contentDescription = featured.title,
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
                    .padding(14.dp)
            ) {
                Surface(
                    color = VibrantOrange.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "FEATURED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                featured.verifiedRating?.let { score ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.68f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = WarmAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f", score),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
                featured.releaseYear?.let { year ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.68f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = featured.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (featured.genres.isNotEmpty()) {
                    Text(
                        text = featured.genres.take(3).joinToString(" • "),
                        style = MaterialTheme.typography.labelMedium,
                        color = WarmAmber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = featured.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NimeGradientButton(
                        text = stringResource(R.string.watch_now),
                        onClick = onWatchNow,
                        icon = Icons.Rounded.PlayArrow,
                        testTag = "hero_watch_now_button",
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = onToggleFavorite,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .height(52.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("hero_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isFavorite) stringResource(R.string.in_favorites) else stringResource(R.string.add_to_favorites),
                            tint = if (isFavorite) VibrantOrange else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeSectionCarousel(
    title: String,
    items: List<AnimeSeries>,
    favoriteIds: Set<String>,
    onAnimeClick: (AnimeSeries) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onSeeAllClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("see_all_${title.lowercase().replace(" ", "_")}")
            ) {
                Text(stringResource(R.string.see_all), color = VibrantOrange)
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = VibrantOrange,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { "${title}_${it.id}" }) { anime ->
                AnimePosterCard(
                    anime = anime,
                    isFavorite = favoriteIds.contains(anime.id),
                    onClick = { onAnimeClick(anime) },
                    cardWidth = 142.dp
                )
            }
        }
    }
}

@Composable
fun AnimePosterCard(
    anime: AnimeSeries,
    isFavorite: Boolean = false,
    isWatchLater: Boolean = false,
    onClick: () -> Unit,
    cardWidth: Dp? = null,
    modifier: Modifier = Modifier
) {
    val sizedModifier = if (cardWidth != null) modifier.width(cardWidth) else modifier.fillMaxWidth()
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = sizedModifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .testTag("anime_card_${anime.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            AsyncImage(
                model = anime.posterUrl ?: R.drawable.img_poster_fallback,
                contentDescription = anime.title,
                placeholder = painterResource(id = R.drawable.img_poster_fallback),
                error = painterResource(id = R.drawable.img_poster_fallback),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top badges: Verified score (left) + Sub/Dub (right)
            anime.verifiedRating?.let { rating ->
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = WarmAmber,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            anime.subOrDubLabel?.let { audioBadge ->
                Surface(
                    color = VibrantOrange.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = audioBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (isFavorite || isWatchLater) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    if (isFavorite) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = stringResource(R.string.in_favorites),
                                tint = VibrantOrange,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(13.dp)
                            )
                        }
                    }
                    if (isWatchLater) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bookmark,
                                contentDescription = stringResource(R.string.in_watch_later),
                                tint = WarmAmber,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = anime.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        val metaParts = buildList {
            anime.releaseYear?.let { add(it.toString()) }
            anime.genres.firstOrNull()?.let { add(it) }
        }
        if (metaParts.isNotEmpty()) {
            Text(
                text = metaParts.joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
