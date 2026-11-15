# NIME WATCHER — Watch Anime. Anytime. Free.

Native Kotlin + Jetpack Compose anime streaming Android application designed for free, open-source tooling and Firebase Spark tier compatibility.

---

## 1. Selected Architecture (Stage 1 Foundation)

- **UI Layer**: Jetpack Compose + Material 3 (`ObsidianBlack #090A0D`, `CharcoalCard #181B24`, `VibrantOrange #FF6B1A`), bundled Google Fonts (`Outfit` + `Plus Jakarta Sans`), adaptive `NavigationBar` (phones) and `NavigationRail` (tablets/foldables).
- **State & Architecture**: MVVM (`ViewModel` + `StateFlow` + `collectAsStateWithLifecycle`) with Repository pattern and manual `AppContainer` DI.
- **Navigation**: Type-safe Navigation Compose (`@Serializable` `NimeRoute`) with auth-gated root routing (`Splash` -> `WelcomeAuth` -> `AuthenticatedMainShell`).
- **Authentication & Cloud Sync**: Firebase Authentication (Google Sign-In via Jetpack `CredentialManager`) and Cloud Firestore (`users/{userId}` with hardened zero-trust security rules).
- **Local Preferences & Network**: Jetpack DataStore (`darkModeEnabled`, `language` `en`/`bn`, playback flags) and `ConnectivityManager` (`NetworkMonitor`) for real-time offline detection.
- **Planned Streaming & Cache Stack (Stages 3–6)**: Interface-based catalog provider abstraction, Room local cache & playback progress, AndroidX Media3 ExoPlayer, `MediaSessionService`, and Picture-in-Picture.

---

## 2. Free Tools & Open-Source Stack

| Layer | Technology | Cost |
| :--- | :--- | :--- |
| Language & UI | Kotlin 2.2, Jetpack Compose, Material 3 | Free / Open Source |
| Navigation & Images | Navigation Compose, Coil, Kotlinx Serialization | Free / Open Source |
| Auth & Database | Firebase Auth, Cloud Firestore (Spark Plan) | Free Tier |
| Local Persistence | Jetpack DataStore, Room (Stage 3/6) | Free / Open Source |
| Streaming Player | AndroidX Media3 ExoPlayer & MediaSession (Stage 4/5) | Free / Open Source |

---

## 3. Free-Tier Limitations to Know

1. **Firebase Spark Plan Quotas**:
   - Cloud Firestore free tier includes daily read/write/delete limits and 1 GiB storage. Read-heavy catalog browsing should cache locally in Room (Stage 3/6) and avoid unnecessary real-time listeners on static catalog items.
2. **Video Hosting & Bandwidth**:
   - **Firebase does not provide unlimited free video streaming bandwidth.** Never upload full anime episodes to Firebase Storage on a zero budget. Connect **authorized external HLS/DASH stream URLs**, public-domain archives, or licensed embedding/CDN providers via the Stage 3/4 provider interface.
3. **Copyright Compliance**:
   - Only connect legally licensed, public-domain, or explicitly authorized anime video sources. Never scrape unauthorized streaming sites or bypass DRM.

---

## 4. Building an APK Using an Android Phone (8GB+ RAM) & Termux

You can edit code and trigger builds from an Android phone, though there are important native binary constraints on Termux:

1. **Recommended Free Mobile Workflow (GitHub Actions — Zero PC Needed)**:
   - Push the project from Termux (`pkg install git && git push`) to a free GitHub repository and run `./gradlew assembleDebug` (or `assembleRelease`) on GitHub's free Linux runners, then download the APK artifact directly to your phone.
2. **Direct Local Termux Build Requirements & Limitations**:
   - Install JDK 17/21 and Gradle in Termux (`pkg install openjdk-17 gradle`).
   - Standard Android SDK `aapt2` binaries downloaded by AGP target `x86_64` Linux and fail on ARM64 Android devices (`Exec format error`). On Termux ARM64, you must point Gradle to a native ARM64 `aapt2` binary via `gradle.properties`:
     `android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`
   - Keep `org.gradle.jvmargs=-Xmx2048m` on 8GB RAM phones to avoid out-of-memory kills during Kotlin/R8 compilation.

---

## 5. Stage Progress Checklist

- [x] **Stage 1**: Architecture, Gradle setup, adaptive theme, custom app icon, animated splash screen, Google Sign-In + Firestore rules foundation, 5-tab navigation shell, DataStore theme & English/Bangla switcher.
- [x] **Stage 2**: Anime avatar gallery, extended registration/profile editor, account deletion & session controls.
- [x] **Stage 3**: Home discovery sections, anime catalog provider abstraction, search, filters, genres, and details page.
- [x] **Stage 4**: Seasons, episodes, authorized video stream provider, and Media3 ExoPlayer controls.
- [ ] **Stage 5**: Picture-in-Picture, `MediaSessionService` background audio, lock-screen & notification controls.
- [ ] **Stage 6**: Favorites, Watch Later, Continue Watching, and Watch History (Firestore + Room cache).
- [ ] **Stage 7**: Notifications, full Bangla/English localization, help/report flows.
- [ ] **Stage 8**: App Check, expanded Robolectric tests, performance tuning, remote-toggle ad abstraction.
- [ ] **Stage 9**: Release signing, ProGuard/R8, final APK build verification.
