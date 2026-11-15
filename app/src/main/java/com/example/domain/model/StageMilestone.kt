package com.example.domain.model

data class StageMilestone(
    val stageNumber: Int,
    val title: String,
    val summary: String,
    val completed: Boolean
)

object NimeRoadmap {
    val stages: List<StageMilestone> = listOf(
        StageMilestone(1, "Architecture, Theme & Splash", "Material 3 dark obsidian UI, custom fonts, Navigation Compose, Google Sign-In & Firestore foundation.", true),
        StageMilestone(2, "Auth & Anime Avatars", "8-category anime avatar gallery, custom photo cropper, registration profile, and account deletion.", true),
        StageMilestone(3, "Home, Search & Catalog", "Catalog provider abstraction, genres, seasons, and anime details.", true),
        StageMilestone(4, "Media3 Streaming Player", "ExoPlayer controls, adaptive quality, WebVTT subtitles, intro/outro skip.", true),
        StageMilestone(5, "PiP & Background Audio", "MediaSessionService foreground playback, Picture-in-Picture, lock-screen controls.", true),
        StageMilestone(6, "Lists & Continue Watching", "Favorites, Watch Later, Continue Watching, and Watch History sync.", false),
        StageMilestone(7, "Profile, Alerts & Bangla UI", "Notification preferences, stats, help center, and full localization.", false),
        StageMilestone(8, "Security, Testing & Ads Layer", "App Check, Robolectric tests, and remote-toggle ad abstraction.", false),
        StageMilestone(9, "APK Build & Termux Docs", "Release signing, ProGuard rules, and mobile build instructions.", false),
    )
}
