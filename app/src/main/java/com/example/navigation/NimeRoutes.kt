package com.example.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface NimeRoute {
    @Serializable
    data object Home : NimeRoute

    @Serializable
    data object Search : NimeRoute

    @Serializable
    data object WatchLater : NimeRoute

    @Serializable
    data object Favorites : NimeRoute

    @Serializable
    data object Profile : NimeRoute

    @Serializable
    data object Settings : NimeRoute

    @Serializable
    data object AvatarGallery : NimeRoute

    @Serializable
    data object EditProfile : NimeRoute

    @Serializable
    data object AccountSecurity : NimeRoute

    @Serializable
    data class AnimeDetails(val animeId: String) : NimeRoute

    @Serializable
    data class CatalogSectionList(val sectionKey: String) : NimeRoute

    @Serializable
    data class GenreDiscovery(val initialTag: String? = null) : NimeRoute

    @Serializable
    data class AnimePlayer(
        val animeId: String,
        val seasonId: String,
        val episodeNumber: Int,
        val useShowcaseDemo: Boolean = false
    ) : NimeRoute
}
