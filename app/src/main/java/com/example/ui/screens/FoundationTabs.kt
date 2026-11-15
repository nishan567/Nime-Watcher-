package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.playback.PipUtils
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.preferences.AppLanguage
import com.example.core.preferences.UserPreferences
import com.example.core.ui.NimeBrandMark
import com.example.core.ui.NimeEmptyStateCard
import com.example.core.ui.NimeErrorStateCard
import com.example.core.ui.NimeGradientButton
import com.example.core.ui.ShimmerSkeletonBox
import com.example.domain.model.AnimeAvatarCatalog
import com.example.domain.model.NimeRoadmap
import com.example.domain.model.StageMilestone
import com.example.domain.model.UiState
import com.example.domain.model.UserProfile
import com.example.ui.components.UserAvatarView
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NimeGradients
import com.example.ui.theme.VibrantOrange
import com.example.ui.theme.WarmAmber
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeFoundationTab(
    profileState: UiState<UserProfile>,
    onRetryProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onCompleteRegistration: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_foundation_tab")
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
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
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("home_settings_button")
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
                        val profile = (profileState as? UiState.Success)?.data
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

        item {
            when (profileState) {
                is UiState.Loading -> {
                    ShimmerSkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    )
                }
                is UiState.Error -> {
                    NimeErrorStateCard(
                        message = profileState.message,
                        onRetry = onRetryProfile
                    )
                }
                is UiState.Empty, is UiState.Success -> {
                    val profile = (profileState as? UiState.Success)?.data
                    val displayName = profile?.displayName ?: "Anime Explorer"
                    HeroFoundationCard(
                        userName = displayName,
                        profileCompleted = profile?.profileCompleted ?: false,
                        onConfigureClick = if (profile?.profileCompleted == false) onCompleteRegistration else onOpenSettings
                    )
                }
            }
        }

        item {
            Text(
                text = "Planned Anime Genres (Stage 3)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(10.dp))
            val previewGenres = listOf("Action", "Adventure", "Isekai", "Fantasy", "Sci-Fi", "Mystery", "Romance")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(previewGenres) { genre ->
                    AssistChip(
                        onClick = {},
                        label = { Text(genre) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }
        }

        item {
            Text(
                text = "Build Progress Checklist",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(NimeRoadmap.stages, key = { it.stageNumber }) { milestone ->
            StageMilestoneCard(milestone = milestone)
        }
    }
}

@Composable
private fun HeroFoundationCard(
    userName: String,
    profileCompleted: Boolean,
    onConfigureClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NimeGradients.cardGlowBorder, MaterialTheme.shapes.large)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_welcome_banner),
                contentDescription = "Nime Watcher Featured Artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp)
                    .background(NimeGradients.darkScrimBottom)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = VibrantOrange.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = stringResource(R.string.stage_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmAmber,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "Welcome back, $userName",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (profileCompleted) {
                        "Your anime avatar, profile, and cloud session are active. Catalog & Media3 arrive in Stages 3–4."
                    } else {
                        "Complete your Nime Watcher registration profile, date of birth & avatar selection."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f)
                )
                NimeGradientButton(
                    text = if (profileCompleted) stringResource(R.string.settings_title) else "Complete Profile Setup",
                    onClick = onConfigureClick,
                    icon = if (profileCompleted) Icons.Rounded.Settings else Icons.Rounded.Badge,
                    testTag = "hero_settings_cta"
                )
            }
        }
    }
}

@Composable
private fun StageMilestoneCard(milestone: StageMilestone) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Icon(
                imageVector = if (milestone.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (milestone.completed) "Completed" else "Pending",
                tint = if (milestone.completed) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stage ${milestone.stageNumber}: ${milestone.title}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = milestone.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SearchFoundationTab() {
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Action", "Isekai", "Fantasy", "Sub", "Dub")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("search_foundation_tab"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.nav_search),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search anime title, genre, or year…") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_query_input")
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }
        }
        NimeEmptyStateCard(
            icon = Icons.Rounded.Search,
            title = "Catalog Provider Ready for Stage 3",
            subtitle = "Search input and genre filter UI are wired. Authorized anime catalog providers and search history connect in Stage 3."
        )
    }
}

@Composable
fun WatchLaterFoundationTab(
    items: List<com.example.domain.model.AnimeSeries> = emptyList(),
    favoriteIds: Set<String> = emptySet(),
    onAnimeClick: (com.example.domain.model.AnimeSeries) -> Unit = {},
    onExploreHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("watch_later_foundation_tab"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.nav_watch_later),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (items.isEmpty()) {
            NimeEmptyStateCard(
                icon = Icons.Outlined.BookmarkBorder,
                title = "Watch Later Queue Empty",
                subtitle = "Tap Watch Later on any anime details page to save it here. Full Firestore sync completes in Stage 6.",
                actionLabel = "Discover Anime",
                onActionClick = onExploreHome
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                gridItems(items, key = { it.id }) { anime ->
                    com.example.ui.components.AnimePosterCard(
                        anime = anime,
                        isFavorite = favoriteIds.contains(anime.id),
                        isWatchLater = true,
                        onClick = { onAnimeClick(anime) }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoritesFoundationTab(
    items: List<com.example.domain.model.AnimeSeries> = emptyList(),
    watchLaterIds: Set<String> = emptySet(),
    onAnimeClick: (com.example.domain.model.AnimeSeries) -> Unit = {},
    onExploreHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("favorites_foundation_tab"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.nav_favorites),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (items.isEmpty()) {
            NimeEmptyStateCard(
                icon = Icons.Outlined.FavoriteBorder,
                title = "No Favorites Yet",
                subtitle = "Tap Add to Favorites on the Home banner or Anime Details to collect your favorites. Cloud sync expands in Stage 6.",
                actionLabel = "Discover Anime",
                onActionClick = onExploreHome
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                gridItems(items, key = { it.id }) { anime ->
                    com.example.ui.components.AnimePosterCard(
                        anime = anime,
                        isFavorite = true,
                        isWatchLater = watchLaterIds.contains(anime.id),
                        onClick = { onAnimeClick(anime) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileFoundationTab(
    profileState: UiState<UserProfile>,
    preferences: UserPreferences,
    statusMessage: String?,
    onRetryProfile: () -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onOpenAvatarGallery: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenAccountSecurity: () -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit,
    onDismissStatus: () -> Unit
) {
    var showEditNameDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_foundation_tab")
    ) {
        item {
            Text(
                text = stringResource(R.string.nav_profile),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (statusMessage != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismissStatus() }
                ) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        item {
            when (profileState) {
                is UiState.Loading -> ShimmerSkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
                is UiState.Error -> NimeErrorStateCard(
                    message = profileState.message,
                    onRetry = onRetryProfile
                )
                is UiState.Empty -> NimeEmptyStateCard(
                    icon = Icons.Rounded.Person,
                    title = "Setting Up Profile",
                    subtitle = "Your Firestore profile record is being initialized.",
                    actionLabel = stringResource(R.string.retry),
                    onActionClick = onRetryProfile
                )
                is UiState.Success -> {
                    val profile = profileState.data
                    val joinDateText = remember(profile.createdAt) {
                        profile.createdAt?.toDate()?.let { date ->
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                        } ?: "Just joined"
                    }
                    val avatarLabel = when (profile.avatarId) {
                        UserProfile.AVATAR_CUSTOM_PHOTO -> "Custom Photo"
                        UserProfile.AVATAR_GOOGLE_PHOTO -> "Google Photo"
                        else -> AnimeAvatarCatalog.findById(profile.avatarId).title
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(modifier = Modifier.clickable { onOpenAvatarGallery() }) {
                                    UserAvatarView(
                                        avatarId = profile.avatarId,
                                        photoUrl = profile.photoUrl,
                                        customAvatarPath = profile.customAvatarPath,
                                        size = 70.dp
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.displayName,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = profile.email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Joined $joinDateText • $avatarLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { showEditNameDialog = true },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("edit_display_name_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Quick edit display name",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            val fullName = listOf(profile.firstName, profile.lastName, profile.surname)
                                .filter { it.isNotBlank() }
                                .joinToString(" ")
                            if (fullName.isNotBlank() || profile.dateOfBirth.isNotBlank()) {
                                HorizontalDivider()
                                if (fullName.isNotBlank()) {
                                    Text(
                                        text = "Full Name: $fullName",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (profile.dateOfBirth.isNotBlank()) {
                                    Text(
                                        text = "DOB: ${profile.dateOfBirth} (Private)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = onOpenAvatarGallery,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("open_avatar_gallery_button")
                                ) {
                                    Icon(Icons.Rounded.Face, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Avatars")
                                }
                                OutlinedButton(
                                    onClick = onOpenEditProfile,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("open_edit_profile_button")
                                ) {
                                    Icon(Icons.Rounded.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (profile.profileCompleted) "Edit Info" else "Complete")
                                }
                            }
                        }
                    }

                    if (showEditNameDialog) {
                        EditNameDialog(
                            initialName = profile.displayName,
                            onDismiss = { showEditNameDialog = false },
                            onSave = {
                                showEditNameDialog = false
                                onUpdateDisplayName(it)
                            }
                        )
                    }
                }
            }
        }

        item {
            AppearanceControlsCard(
                preferences = preferences,
                onToggleDarkMode = onToggleDarkMode,
                onSelectLanguage = onSelectLanguage
            )
        }

        item {
            OutlinedButton(
                onClick = onOpenAccountSecurity,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("open_account_security_button")
            ) {
                Icon(Icons.Rounded.Security, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Account, Terms & Security")
            }
        }

        item {
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("open_settings_button")
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_title))
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.sign_out), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsFoundationScreen(
    preferences: UserPreferences,
    onToggleDarkMode: (Boolean) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onToggleBackgroundPlayback: (Boolean) -> Unit = {},
    onToggleAutoPip: (Boolean) -> Unit = {},
    onToggleBackgroundAudio: (Boolean) -> Unit = {},
    onToggleLockScreenControls: (Boolean) -> Unit = {},
    onToggleAutoNextEpisode: (Boolean) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isPipSupported = remember(context) { PipUtils.isPipSupported(context) }
    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsGranted = granted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("settings_back_button")
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
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .widthIn(max = 600.dp)
                .testTag("settings_foundation_screen")
        ) {
            AppearanceControlsCard(
                preferences = preferences,
                onToggleDarkMode = onToggleDarkMode,
                onSelectLanguage = onSelectLanguage
            )

            PlaybackSettingsCard(
                preferences = preferences,
                isPipSupported = isPipSupported,
                notificationsGranted = notificationsGranted,
                onRequestNotifications = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onToggleBackgroundPlayback = onToggleBackgroundPlayback,
                onToggleAutoPip = onToggleAutoPip,
                onToggleBackgroundAudio = onToggleBackgroundAudio,
                onToggleLockScreenControls = onToggleLockScreenControls,
                onToggleAutoNextEpisode = onToggleAutoNextEpisode
            )
        }
    }
}

@Composable
private fun PlaybackSettingsCard(
    preferences: UserPreferences,
    isPipSupported: Boolean,
    notificationsGranted: Boolean,
    onRequestNotifications: () -> Unit,
    onToggleBackgroundPlayback: (Boolean) -> Unit,
    onToggleAutoPip: (Boolean) -> Unit,
    onToggleBackgroundAudio: (Boolean) -> Unit,
    onToggleLockScreenControls: (Boolean) -> Unit,
    onToggleAutoNextEpisode: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("playback_settings_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.playback_settings_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Media3 Foreground Service, PiP & Lock-Screen Preferences",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!notificationsGranted) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onRequestNotifications,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_enable_notifications_button")
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Enable Media Playback Notifications")
                }
            }

            Spacer(Modifier.height(12.dp))
            SettingsToggleItem(
                icon = Icons.Rounded.PlayCircleOutline,
                title = "Background Playback",
                subtitle = "Keep foreground MediaSession running when leaving the app",
                checked = preferences.backgroundPlaybackEnabled,
                onCheckedChange = onToggleBackgroundPlayback,
                testTag = "settings_background_playback_switch"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            SettingsToggleItem(
                icon = Icons.Rounded.PictureInPictureAlt,
                title = "Automatic PiP",
                subtitle = if (isPipSupported) "Enter Picture-in-Picture when pressing Home" else "Not supported on this device",
                checked = preferences.autoPipEnabled && isPipSupported,
                enabled = isPipSupported,
                onCheckedChange = onToggleAutoPip,
                testTag = "settings_auto_pip_switch"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            SettingsToggleItem(
                icon = Icons.Rounded.Headphones,
                title = "Continue Audio in Background",
                subtitle = "Keep playing audio if PiP is dismissed or screen is locked",
                checked = preferences.backgroundAudioEnabled,
                enabled = preferences.backgroundPlaybackEnabled,
                onCheckedChange = onToggleBackgroundAudio,
                testTag = "settings_background_audio_switch"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            SettingsToggleItem(
                icon = Icons.Rounded.Lock,
                title = "Lock Screen Controls",
                subtitle = "Expose artwork, Play/Pause, and Next/Prev on lock screen",
                checked = preferences.lockScreenControlsEnabled,
                enabled = preferences.backgroundPlaybackEnabled,
                onCheckedChange = onToggleLockScreenControls,
                testTag = "settings_lock_screen_switch"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            SettingsToggleItem(
                icon = Icons.Rounded.AutoMode,
                title = "Auto Next Episode",
                subtitle = "Automatically advance to the next episode upon completion",
                checked = preferences.autoNextEpisodeEnabled,
                onCheckedChange = onToggleAutoNextEpisode,
                testTag = "settings_auto_next_switch"
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = VibrantOrange),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun AppearanceControlsCard(
    preferences: UserPreferences,
    onToggleDarkMode: (Boolean) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.DarkMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Dark Obsidian Theme", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (preferences.darkModeEnabled) "Obsidian & Orange active" else "Crisp Light mode active",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = preferences.darkModeEnabled,
                    onCheckedChange = onToggleDarkMode,
                    colors = SwitchDefaults.colors(checkedTrackColor = VibrantOrange),
                    modifier = Modifier.testTag("dark_mode_switch")
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Interface Language", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = preferences.language.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = preferences.language == lang,
                            onClick = { onSelectLanguage(lang) },
                            label = { Text(lang.label) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("language_chip_${lang.code}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Display Name") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Display Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_name_input")
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                modifier = Modifier.testTag("save_display_name_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
