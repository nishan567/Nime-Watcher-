package com.example.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.core.preferences.UserPreferences
import com.example.core.ui.OfflineStatusBanner
import com.example.di.AppContainer
import com.example.domain.model.CatalogSectionType
import com.example.domain.model.UiState
import com.example.domain.model.UserProfile
import com.example.ui.anime.AnimeDetailsScreen
import com.example.ui.anime.AnimeDetailsViewModel
import com.example.ui.auth.WelcomeAuthScreen
import com.example.ui.discover.GenreDiscoveryScreen
import com.example.ui.discover.GenreDiscoveryViewModel
import com.example.ui.home.CatalogSectionScreen
import com.example.ui.home.HomeCatalogViewModel
import com.example.ui.home.HomeDiscoveryScreen
import com.example.ui.home.SectionPaginationViewModel
import com.example.ui.player.AnimePlayerScreen
import com.example.ui.player.AnimePlayerViewModel
import com.example.ui.profile.AccountSecurityScreen
import com.example.ui.profile.AvatarSelectionScreen
import com.example.ui.profile.EditProfileScreen
import com.example.ui.screens.FavoritesFoundationTab
import com.example.ui.screens.MainViewModel
import com.example.ui.screens.ProfileFoundationTab
import com.example.ui.screens.SettingsFoundationScreen
import com.example.ui.screens.WatchLaterFoundationTab
import com.example.ui.search.AnimeSearchScreen
import com.example.ui.search.AnimeSearchViewModel
import com.example.ui.splash.SplashScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

private data class BottomNavItem(
    val route: NimeRoute,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopLevelDestinations = listOf(
    BottomNavItem(NimeRoute.Home, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    BottomNavItem(NimeRoute.Search, R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
    BottomNavItem(NimeRoute.WatchLater, R.string.nav_watch_later, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "nav_watch_later"),
    BottomNavItem(NimeRoute.Favorites, R.string.nav_favorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "nav_favorites"),
    BottomNavItem(NimeRoute.Profile, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "nav_profile"),
)

@Composable
fun NimeWatcherRoot(
    container: AppContainer,
    preferences: UserPreferences,
    isInPipMode: Boolean = false
) {
    val scope = rememberCoroutineScope()
    var showSplash by remember { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    if (showSplash) {
        SplashScreen(
            authRepository = container.authRepository,
            rememberMeEnabled = preferences.rememberMeEnabled,
            onSplashFinished = {
                currentUser = Firebase.auth.currentUser
                showSplash = false
            }
        )
    } else {
        val user = currentUser
        if (user == null) {
            WelcomeAuthScreen(
                authRepository = container.authRepository,
                profileRepository = container.userProfileRepository,
                preferences = preferences,
                onToggleRememberMe = { enabled ->
                    scope.launch { container.preferencesRepository.setRememberMeEnabled(enabled) }
                },
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                }
            )
        } else {
            key(user.uid) {
                AuthenticatedMainShell(
                    currentUserId = user.uid,
                    isEmailVerified = user.isEmailVerified,
                    container = container,
                    isInPipMode = isInPipMode
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedMainShell(
    currentUserId: String,
    isEmailVerified: Boolean,
    container: AppContainer,
    isInPipMode: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = MainViewModel.Factory(
            currentUserId = currentUserId,
            authRepository = container.authRepository,
            profileRepository = container.userProfileRepository,
            preferencesRepository = container.preferencesRepository,
            networkMonitor = container.networkMonitor
        )
    )

    val homeViewModel: HomeCatalogViewModel = viewModel(
        key = "home_catalog_$currentUserId",
        factory = HomeCatalogViewModel.Factory(container.catalogRepository)
    )

    val searchViewModel: AnimeSearchViewModel = viewModel(
        key = "anime_search_$currentUserId",
        factory = AnimeSearchViewModel.Factory(container.catalogRepository)
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val homeState by homeViewModel.homeState.collectAsStateWithLifecycle()
    val favoritesList by homeViewModel.favoritesList.collectAsStateWithLifecycle()
    val watchLaterList by homeViewModel.watchLaterList.collectAsStateWithLifecycle()
    val continueWatchingList by homeViewModel.continueWatchingList.collectAsStateWithLifecycle()
    val favoriteIds by homeViewModel.favoriteIds.collectAsStateWithLifecycle()
    val watchLaterIds by homeViewModel.watchLaterIds.collectAsStateWithLifecycle()

    val playerState by container.playbackManager.uiState.collectAsStateWithLifecycle()
    val pendingPlayerRoute by container.playbackManager.pendingPlayerRoute.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isTopLevelRoute = TopLevelDestinations.any { item ->
        currentDestination?.hasRoute(item.route::class) == true
    }
    val isPlayerRoute = currentDestination?.hasRoute(NimeRoute.AnimePlayer::class) == true

    LaunchedEffect(pendingPlayerRoute) {
        val target = pendingPlayerRoute ?: return@LaunchedEffect
        if (!isPlayerRoute) {
            navController.navigate(target) {
                launchSingleTop = true
            }
        }
        container.playbackManager.consumePendingPlayerRoute()
    }

    fun navigateTopLevel(route: NimeRoute) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openGenreInSearch(genre: String) {
        searchViewModel.applyGenrePreset(genre)
        navigateTopLevel(NimeRoute.Search)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = if (isInPipMode) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing,
            bottomBar = {
                if (!isInPipMode) {
                    Column {
                        if (playerState.isPlaybackSessionActive && !isPlayerRoute && !useNavigationRail) {
                            ActivePlaybackMiniPlayerBar(
                                state = playerState,
                                onRestorePlayer = {
                                    container.playbackManager.requestOpenActivePlayerRoute()
                                },
                                onTogglePlayPause = { container.playbackManager.togglePlayPause() },
                                onNextEpisode = { container.playbackManager.nextEpisode() },
                                onStop = { container.playbackManager.stopPlaybackAndDismissSession() }
                            )
                        }
                        if (!useNavigationRail && isTopLevelRoute) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.testTag("bottom_navigation_bar")
                            ) {
                                TopLevelDestinations.forEach { item ->
                                    val selected = currentDestination?.hasRoute(item.route::class) == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTopLevel(item.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = stringResource(item.labelRes)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(item.labelRes),
                                                maxLines = 1
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag(item.testTag)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!isInPipMode && useNavigationRail && isTopLevelRoute) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("side_navigation_rail")
                    ) {
                        TopLevelDestinations.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (!isInPipMode) {
                        OfflineStatusBanner(isOnline = isOnline)
                    }
                    NavHost(
                        navController = navController,
                        startDestination = NimeRoute.Home,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable<NimeRoute.Home> {
                            HomeDiscoveryScreen(
                                profileState = profileState,
                                homeState = homeState,
                                favoriteIds = favoriteIds,
                                favoritesList = favoritesList,
                                continueWatchingList = continueWatchingList,
                                onRetryCatalog = { homeViewModel.loadHome(forceLoadingState = true) },
                                onAnimeClick = { anime ->
                                    navController.navigate(NimeRoute.AnimeDetails(anime.id))
                                },
                                onResumePlayback = { progress ->
                                    navController.navigate(
                                        NimeRoute.AnimePlayer(
                                            animeId = progress.animeId,
                                            seasonId = progress.seasonId,
                                            episodeNumber = progress.episodeNumber,
                                            useShowcaseDemo = true
                                        )
                                    )
                                },
                                onToggleFavorite = homeViewModel::toggleFavorite,
                                onSeeAllSection = { section ->
                                    navController.navigate(NimeRoute.CatalogSectionList(section.key))
                                },
                                onSelectGenreSearch = ::openGenreInSearch,
                                onOpenGenreDiscovery = { tag ->
                                    navController.navigate(NimeRoute.GenreDiscovery(initialTag = tag))
                                },
                                onOpenSettings = { navController.navigate(NimeRoute.Settings) },
                                onOpenProfile = { navigateTopLevel(NimeRoute.Profile) },
                                onCompleteRegistration = { navController.navigate(NimeRoute.EditProfile) },
                                onOpenFavoritesTab = { navigateTopLevel(NimeRoute.Favorites) }
                            )
                        }
                        composable<NimeRoute.Search> {
                            AnimeSearchScreen(
                                viewModel = searchViewModel,
                                favoriteIds = favoriteIds,
                                watchLaterIds = watchLaterIds,
                                onAnimeClick = { anime ->
                                    navController.navigate(NimeRoute.AnimeDetails(anime.id))
                                },
                                onOpenGenreDiscovery = {
                                    navController.navigate(NimeRoute.GenreDiscovery())
                                }
                            )
                        }
                        composable<NimeRoute.WatchLater> {
                            WatchLaterFoundationTab(
                                items = watchLaterList,
                                favoriteIds = favoriteIds,
                                onAnimeClick = { anime ->
                                    navController.navigate(NimeRoute.AnimeDetails(anime.id))
                                },
                                onExploreHome = { navigateTopLevel(NimeRoute.Home) }
                            )
                        }
                        composable<NimeRoute.Favorites> {
                            FavoritesFoundationTab(
                                items = favoritesList,
                                watchLaterIds = watchLaterIds,
                                onAnimeClick = { anime ->
                                    navController.navigate(NimeRoute.AnimeDetails(anime.id))
                                },
                                onExploreHome = { navigateTopLevel(NimeRoute.Home) }
                            )
                        }
                        composable<NimeRoute.Profile> {
                            ProfileFoundationTab(
                                profileState = profileState,
                                preferences = preferences,
                                statusMessage = statusMessage,
                                onRetryProfile = { viewModel.ensureProfileInitialized() },
                                onUpdateDisplayName = viewModel::updateDisplayName,
                                onToggleDarkMode = viewModel::toggleDarkMode,
                                onSelectLanguage = viewModel::setLanguage,
                                onOpenAvatarGallery = { navController.navigate(NimeRoute.AvatarGallery) },
                                onOpenEditProfile = { navController.navigate(NimeRoute.EditProfile) },
                                onOpenAccountSecurity = { navController.navigate(NimeRoute.AccountSecurity) },
                                onOpenSettings = { navController.navigate(NimeRoute.Settings) },
                                onSignOut = {
                                    scope.launch { container.authRepository.signOut(context) }
                                },
                                onDismissStatus = viewModel::clearStatusMessage
                            )
                        }
                        composable<NimeRoute.Settings> {
                            SettingsFoundationScreen(
                                preferences = preferences,
                                onToggleDarkMode = viewModel::toggleDarkMode,
                                onSelectLanguage = viewModel::setLanguage,
                                onToggleBackgroundPlayback = viewModel::toggleBackgroundPlayback,
                                onToggleAutoPip = viewModel::toggleAutoPip,
                                onToggleBackgroundAudio = viewModel::toggleBackgroundAudio,
                                onToggleLockScreenControls = viewModel::toggleLockScreenControls,
                                onToggleAutoNextEpisode = viewModel::toggleAutoNextEpisode,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable<NimeRoute.AvatarGallery> {
                            val activeProfile = (profileState as? UiState.Success)?.data
                                ?: UserProfile(userId = currentUserId)
                            AvatarSelectionScreen(
                                profile = activeProfile,
                                onSaveAvatar = viewModel::updateAvatarSelection,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable<NimeRoute.EditProfile> {
                            val activeProfile = (profileState as? UiState.Success)?.data
                                ?: UserProfile(userId = currentUserId)
                            EditProfileScreen(
                                profile = activeProfile,
                                onOpenAvatarGallery = { navController.navigate(NimeRoute.AvatarGallery) },
                                onSaveProfile = viewModel::saveProfileDetails,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable<NimeRoute.AccountSecurity> {
                            val activeProfile = (profileState as? UiState.Success)?.data
                                ?: UserProfile(userId = currentUserId)
                            AccountSecurityScreen(
                                profile = activeProfile,
                                preferences = preferences,
                                isEmailVerified = isEmailVerified,
                                statusMessage = statusMessage,
                                onToggleRememberMe = viewModel::toggleRememberMe,
                                onSignOut = {
                                    scope.launch { container.authRepository.signOut(context) }
                                },
                                onDeleteAccount = { viewModel.deleteAccount(context) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable<NimeRoute.AnimeDetails> { entry ->
                            val routeArgs = entry.toRoute<NimeRoute.AnimeDetails>()
                            val detailsViewModel: AnimeDetailsViewModel = viewModel(
                                key = "details_${routeArgs.animeId}",
                                factory = AnimeDetailsViewModel.Factory(
                                    animeId = routeArgs.animeId,
                                    catalogRepository = container.catalogRepository
                                )
                            )
                            AnimeDetailsScreen(
                                viewModel = detailsViewModel,
                                favoriteIds = favoriteIds,
                                watchLaterIds = watchLaterIds,
                                onWatchEpisode = { seasonId, episodeNumber ->
                                    navController.navigate(
                                        NimeRoute.AnimePlayer(
                                            animeId = routeArgs.animeId,
                                            seasonId = seasonId,
                                            episodeNumber = episodeNumber
                                        )
                                    )
                                },
                                onSelectRelatedAnime = { related ->
                                    navController.navigate(NimeRoute.AnimeDetails(related.id))
                                },
                                onSelectGenreSearch = ::openGenreInSearch,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable<NimeRoute.AnimePlayer> { entry ->
                            val routeArgs = entry.toRoute<NimeRoute.AnimePlayer>()
                            val playerViewModel: AnimePlayerViewModel = viewModel(
                                key = "player_${routeArgs.animeId}_${routeArgs.seasonId}_${routeArgs.episodeNumber}",
                                factory = AnimePlayerViewModel.Factory(
                                    animeId = routeArgs.animeId,
                                    seasonId = routeArgs.seasonId,
                                    episodeNumber = routeArgs.episodeNumber,
                                    forceShowcaseDemo = routeArgs.useShowcaseDemo,
                                    playbackManager = container.playbackManager,
                                    preferencesRepository = container.preferencesRepository
                                )
                            )
                            AnimePlayerScreen(
                                viewModel = playerViewModel,
                                isInPipMode = isInPipMode,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable<NimeRoute.CatalogSectionList> { entry ->
                            val routeArgs = entry.toRoute<NimeRoute.CatalogSectionList>()
                            val section = CatalogSectionType.fromKey(routeArgs.sectionKey)
                                ?: CatalogSectionType.TRENDING
                            val sectionViewModel: SectionPaginationViewModel = viewModel(
                                key = "section_${section.key}",
                                factory = SectionPaginationViewModel.Factory(
                                    section = section,
                                    catalogRepository = container.catalogRepository
                                )
                            )
                            CatalogSectionScreen(
                                section = section,
                                viewModel = sectionViewModel,
                                favoriteIds = favoriteIds,
                                watchLaterIds = watchLaterIds,
                                onAnimeClick = { anime ->
                                    navController.navigate(NimeRoute.AnimeDetails(anime.id))
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable<NimeRoute.GenreDiscovery> { entry ->
                            val routeArgs = entry.toRoute<NimeRoute.GenreDiscovery>()
                            val genreViewModel: GenreDiscoveryViewModel = viewModel(
                                key = "genre_discovery_${currentUserId}_${routeArgs.initialTag.orEmpty()}",
                                factory = GenreDiscoveryViewModel.Factory(
                                    currentUserId = currentUserId,
                                    initialTag = routeArgs.initialTag,
                                    discoveryRepository = container.genreDiscoveryRepository,
                                    catalogRepository = container.catalogRepository
                                )
                            )
                            GenreDiscoveryScreen(
                                viewModel = genreViewModel,
                                favoriteIds = favoriteIds,
                                watchLaterIds = watchLaterIds,
                                onAnimeClick = { anime ->
                                    navController.navigate(NimeRoute.AnimeDetails(anime.id))
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    if (!isInPipMode && playerState.isPlaybackSessionActive && !isPlayerRoute && useNavigationRail) {
                        ActivePlaybackMiniPlayerBar(
                            state = playerState,
                            onRestorePlayer = {
                                container.playbackManager.requestOpenActivePlayerRoute()
                            },
                            onTogglePlayPause = { container.playbackManager.togglePlayPause() },
                            onNextEpisode = { container.playbackManager.nextEpisode() },
                            onStop = { container.playbackManager.stopPlaybackAndDismissSession() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivePlaybackMiniPlayerBar(
    state: com.example.ui.player.PlayerUiState,
    onRestorePlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextEpisode: () -> Unit,
    onStop: () -> Unit
) {
    val progress = if (state.durationMs > 0L) {
        (state.currentPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRestorePlayer() }
            .testTag("active_playback_mini_player")
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                AsyncImage(
                    model = state.posterUrl,
                    placeholder = painterResource(id = R.drawable.img_poster_fallback),
                    error = painterResource(id = R.drawable.img_poster_fallback),
                    contentDescription = state.animeTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 40.dp, height = 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.animeTitle.ifBlank { "Now Playing" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ep ${state.currentEpisodeNumber} • ${if (state.isPlaying) "Playing (Tap to restore)" else "Paused"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play"
                    )
                }
                if (state.hasNextEpisode) {
                    IconButton(
                        onClick = onNextEpisode,
                        modifier = Modifier.testTag("mini_player_next")
                    ) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Next Episode")
                    }
                }
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.testTag("mini_player_stop")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Stop Playback")
                }
            }
        }
    }
}
