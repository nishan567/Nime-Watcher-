package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class GenreDiscoveryPreference(
    @DocumentId val id: String = "",
    val userId: String = "",
    val preferredTags: List<String> = DEFAULT_PREFERRED_TAGS,
    val sortBy: String = AnimeSortOption.TRENDING.apiSort,
    val formatFilter: String = FORMAT_ALL,
    val onlyShowPreferred: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "preferredTags" to preferredTags.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(25),
        "sortBy" to (AnimeSortOption.entries.firstOrNull { it.apiSort == sortBy }?.apiSort
            ?: AnimeSortOption.TRENDING.apiSort),
        "formatFilter" to (formatFilter.takeIf { it in ALLOWED_FORMATS } ?: FORMAT_ALL),
        "onlyShowPreferred" to onlyShowPreferred,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toSortOption(): AnimeSortOption =
        AnimeSortOption.entries.firstOrNull { it.apiSort == sortBy } ?: AnimeSortOption.TRENDING

    fun toFormatOrNull(): AnimeFormat? = when (formatFilter) {
        "TV" -> AnimeFormat.TV
        "MOVIE" -> AnimeFormat.MOVIE
        "OVA" -> AnimeFormat.OVA
        "ONA" -> AnimeFormat.ONA
        "SPECIAL" -> AnimeFormat.SPECIAL
        else -> null
    }

    companion object {
        const val DEFAULT_DOC_ID = "default"
        const val FORMAT_ALL = "ALL"
        val ALLOWED_FORMATS = setOf("ALL", "TV", "MOVIE", "OVA", "ONA", "SPECIAL")
        val DEFAULT_PREFERRED_TAGS = listOf("Action", "Fantasy", "Isekai", "Cyberpunk")
    }
}

@IgnoreExtraProperties
data class DiscoveryCategory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val queryValue: String = "",
    val filterKind: String = KIND_GENRE,
    val accentKey: String = ACCENT_ORANGE,
    @get:PropertyName("isPinned") @set:PropertyName("isPinned")
    var isPinned: Boolean = false,
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "name" to name.trim().take(50).ifBlank { "Anime" },
        "queryValue" to queryValue.trim().take(40).ifBlank { "Action" },
        "filterKind" to if (filterKind == KIND_TAG) KIND_TAG else KIND_GENRE,
        "accentKey" to (accentKey.takeIf { it in ALLOWED_ACCENTS } ?: ACCENT_ORANGE),
        "isPinned" to isPinned,
        "sortOrder" to sortOrder.coerceIn(0L, 500L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        const val KIND_GENRE = "genre"
        const val KIND_TAG = "tag"
        const val ACCENT_ORANGE = "orange"
        const val ACCENT_AMBER = "amber"
        const val ACCENT_EMERALD = "emerald"
        const val ACCENT_CYAN = "cyan"
        const val ACCENT_ROSE = "rose"
        const val ACCENT_VIOLET = "violet"
        val ALLOWED_ACCENTS = setOf(
            ACCENT_ORANGE,
            ACCENT_AMBER,
            ACCENT_EMERALD,
            ACCENT_CYAN,
            ACCENT_ROSE,
            ACCENT_VIOLET
        )
    }
}

data class CuratedTagOption(
    val queryValue: String,
    val displayName: String,
    val filterKind: String,
    val defaultAccent: String
)

object GenreDiscoveryCatalog {
    val availableTagOptions: List<CuratedTagOption> = listOf(
        CuratedTagOption("Action", "Action", DiscoveryCategory.KIND_GENRE, DiscoveryCategory.ACCENT_ORANGE),
        CuratedTagOption("Fantasy", "Fantasy", DiscoveryCategory.KIND_GENRE, DiscoveryCategory.ACCENT_AMBER),
        CuratedTagOption("Isekai", "Isekai", DiscoveryCategory.KIND_TAG, DiscoveryCategory.ACCENT_VIOLET),
        CuratedTagOption("Cyberpunk", "Cyberpunk", DiscoveryCategory.KIND_TAG, DiscoveryCategory.ACCENT_CYAN),
        CuratedTagOption("Adventure", "Adventure", DiscoveryCategory.KIND_GENRE, DiscoveryCategory.ACCENT_EMERALD),
        CuratedTagOption("Romance", "Romance", DiscoveryCategory.KIND_GENRE, DiscoveryCategory.ACCENT_ROSE),
        CuratedTagOption("Comedy", "Comedy", DiscoveryCategory.KIND_GENRE, DiscoveryCategory.ACCENT_AMBER),
        CuratedTagOption("Psychological", "Psychological", DiscoveryCategory.KIND_GENRE, DiscoveryCategory.ACCENT_VIOLET),
        CuratedTagOption("Sci-Fi", "Sci-Fi", DiscoveryCategory.KIND_GENRE, DiscoveryCategory.ACCENT_CYAN),
        CuratedTagOption("Mystery", "Mystery", DiscoveryCategory.KIND_GENRE, DiscoveryCategory.ACCENT_VIOLET),
        CuratedTagOption("Supernatural", "Supernatural", DiscoveryCategory.KIND_GENRE, DiscoveryCategory.ACCENT_ROSE),
        CuratedTagOption("Horror", "Horror", DiscoveryCategory.KIND_GENRE, DiscoveryCategory.ACCENT_ROSE),
        CuratedTagOption("Slice of Life", "Slice of Life", DiscoveryCategory.KIND_GENRE, DiscoveryCategory.ACCENT_EMERALD),
        CuratedTagOption("Drama", "Drama", DiscoveryCategory.KIND_GENRE, DiscoveryCategory.ACCENT_AMBER),
        CuratedTagOption("Time Travel", "Time Travel", DiscoveryCategory.KIND_TAG, DiscoveryCategory.ACCENT_CYAN),
        CuratedTagOption("Post-Apocalyptic", "Post-Apocalyptic", DiscoveryCategory.KIND_TAG, DiscoveryCategory.ACCENT_ORANGE),
        CuratedTagOption("Martial Arts", "Martial Arts", DiscoveryCategory.KIND_TAG, DiscoveryCategory.ACCENT_ORANGE),
        CuratedTagOption("Sports", "Sports", DiscoveryCategory.KIND_GENRE, DiscoveryCategory.ACCENT_EMERALD)
    )

    fun starterCategories(userId: String): List<Pair<String, DiscoveryCategory>> = listOf(
        "cat_action" to DiscoveryCategory(
            userId = userId,
            name = "High-Octane Action",
            queryValue = "Action",
            filterKind = DiscoveryCategory.KIND_GENRE,
            accentKey = DiscoveryCategory.ACCENT_ORANGE,
            isPinned = true,
            sortOrder = 0L
        ),
        "cat_fantasy" to DiscoveryCategory(
            userId = userId,
            name = "Epic Fantasy Realms",
            queryValue = "Fantasy",
            filterKind = DiscoveryCategory.KIND_GENRE,
            accentKey = DiscoveryCategory.ACCENT_AMBER,
            isPinned = true,
            sortOrder = 1L
        ),
        "cat_isekai" to DiscoveryCategory(
            userId = userId,
            name = "Isekai Worlds",
            queryValue = "Isekai",
            filterKind = DiscoveryCategory.KIND_TAG,
            accentKey = DiscoveryCategory.ACCENT_VIOLET,
            isPinned = true,
            sortOrder = 2L
        ),
        "cat_cyberpunk" to DiscoveryCategory(
            userId = userId,
            name = "Cyberpunk & Neon",
            queryValue = "Cyberpunk",
            filterKind = DiscoveryCategory.KIND_TAG,
            accentKey = DiscoveryCategory.ACCENT_CYAN,
            isPinned = false,
            sortOrder = 3L
        ),
        "cat_romance" to DiscoveryCategory(
            userId = userId,
            name = "Romance & Heart",
            queryValue = "Romance",
            filterKind = DiscoveryCategory.KIND_GENRE,
            accentKey = DiscoveryCategory.ACCENT_ROSE,
            isPinned = false,
            sortOrder = 4L
        ),
        "cat_psychological" to DiscoveryCategory(
            userId = userId,
            name = "Psychological Thrillers",
            queryValue = "Psychological",
            filterKind = DiscoveryCategory.KIND_GENRE,
            accentKey = DiscoveryCategory.ACCENT_VIOLET,
            isPinned = false,
            sortOrder = 5L
        ),
        "cat_slice_of_life" to DiscoveryCategory(
            userId = userId,
            name = "Slice of Life & Cozy",
            queryValue = "Slice of Life",
            filterKind = DiscoveryCategory.KIND_GENRE,
            accentKey = DiscoveryCategory.ACCENT_EMERALD,
            isPinned = false,
            sortOrder = 6L
        ),
        "cat_mystery" to DiscoveryCategory(
            userId = userId,
            name = "Mystery & Supernatural",
            queryValue = "Mystery",
            filterKind = DiscoveryCategory.KIND_GENRE,
            accentKey = DiscoveryCategory.ACCENT_AMBER,
            isPinned = false,
            sortOrder = 7L
        )
    )
}
