package com.example.domain.model

import androidx.compose.ui.graphics.Color

enum class AvatarCategory(val id: String, val label: String) {
    ALL("all", "All"),
    MALE("male", "Male"),
    FEMALE("female", "Female"),
    FANTASY("fantasy", "Fantasy"),
    SAMURAI("samurai", "Samurai"),
    NINJA("ninja", "Ninja"),
    CUTE("cute", "Cute"),
    DARK("dark", "Dark"),
    MINIMAL("minimal", "Minimal"),
}

data class AnimeAvatar(
    val id: String,
    val title: String,
    val category: AvatarCategory,
    val bgStart: Color,
    val bgEnd: Color,
    val accent: Color,
    val secondary: Color
)

object AnimeAvatarCatalog {
    val avatars: List<AnimeAvatar> = listOf(
        // Male
        AnimeAvatar("male_ren", "Ren Blaze", AvatarCategory.MALE, Color(0xFF1D1B2E), Color(0xFF3E2028), Color(0xFFFF6B1A), Color(0xFFFFD180)),
        AnimeAvatar("male_kaito", "Kaito Azure", AvatarCategory.MALE, Color(0xFF0E1B2B), Color(0xFF1A3A5A), Color(0xFF4FC3F7), Color(0xFFE1F5FE)),
        // Female
        AnimeAvatar("female_akira", "Akira Nova", AvatarCategory.FEMALE, Color(0xFF2B1326), Color(0xFF4A1936), Color(0xFFFF8A65), Color(0xFFFFE0B2)),
        AnimeAvatar("female_yumi", "Yumi Moon", AvatarCategory.FEMALE, Color(0xFF1A1635), Color(0xFF322659), Color(0xFFCE93D8), Color(0xFFFFF59D)),
        // Fantasy
        AnimeAvatar("fantasy_sylph", "Sylph Aurora", AvatarCategory.FANTASY, Color(0xFF0B2A29), Color(0xFF174F46), Color(0xFF64FFDA), Color(0xFFFFD54F)),
        AnimeAvatar("fantasy_drakon", "Drakon Ignis", AvatarCategory.FANTASY, Color(0xFF240F0F), Color(0xFF4D1A12), Color(0xFFFF5722), Color(0xFFFFC107)),
        // Samurai
        AnimeAvatar("samurai_ember", "Ember Ronin", AvatarCategory.SAMURAI, Color(0xFF1B120E), Color(0xFF451F0D), Color(0xFFFF6B1A), Color(0xFFFFCA28)),
        AnimeAvatar("samurai_kuro", "Kuro Blade", AvatarCategory.SAMURAI, Color(0xFF111524), Color(0xFF252D48), Color(0xFF90CAF9), Color(0xFFFF7043)),
        // Ninja
        AnimeAvatar("ninja_kage", "Kage Shinobi", AvatarCategory.NINJA, Color(0xFF121318), Color(0xFF2A1D1B), Color(0xFFFF6B1A), Color(0xFFE0E0E0)),
        AnimeAvatar("ninja_swift", "Swift Mist", AvatarCategory.NINJA, Color(0xFF0C1D24), Color(0xFF173B47), Color(0xFF26C6DA), Color(0xFFFFAB40)),
        // Cute
        AnimeAvatar("cute_neko", "Neko Mochi", AvatarCategory.CUTE, Color(0xFF2E1E28), Color(0xFF563242), Color(0xFFFFAB91), Color(0xFFFFF8E1)),
        AnimeAvatar("cute_kitsune", "Kitsune Bell", AvatarCategory.CUTE, Color(0xFF2E1A12), Color(0xFF5A311D), Color(0xFFFF9800), Color(0xFFFFFDE7)),
        // Dark
        AnimeAvatar("dark_reaper", "Void Eclipse", AvatarCategory.DARK, Color(0xFF090A0F), Color(0xFF290E18), Color(0xFFFF3D00), Color(0xFFB71C1C)),
        AnimeAvatar("dark_phantom", "Phantom Oni", AvatarCategory.DARK, Color(0xFF120F18), Color(0xFF2A1B33), Color(0xFFFF6E40), Color(0xFFEDE7F6)),
        // Minimal
        AnimeAvatar("minimal_torii", "Torii Ember", AvatarCategory.MINIMAL, Color(0xFF171314), Color(0xFF331B16), Color(0xFFFF6B1A), Color(0xFFFFE0B2)),
        AnimeAvatar("minimal_sakura", "Sakura Crest", AvatarCategory.MINIMAL, Color(0xFF191522), Color(0xFF2D223B), Color(0xFFFFB74D), Color(0xFFF8BBD0)),
    )

    fun findById(id: String): AnimeAvatar =
        avatars.firstOrNull { it.id == id } ?: avatars.first { it.id == UserProfile.DEFAULT_AVATAR_ID }
}
