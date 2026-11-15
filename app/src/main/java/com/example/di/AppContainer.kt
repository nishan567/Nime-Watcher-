package com.example.di

import android.content.Context
import com.example.BuildConfig
import com.example.R
import com.example.core.network.NetworkMonitor
import com.example.core.preferences.UserPreferencesRepository
import com.example.data.catalog.CatalogProviderConfig
import com.example.data.local.NimeCatalogDatabase
import com.example.data.repository.AnimeCatalogRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.GenreDiscoveryRepository
import com.example.data.repository.UserProfileRepository
import com.example.data.stream.StreamProviderConfig
import com.example.domain.catalog.AnimeCatalogProvider
import com.example.domain.stream.AuthorizedStreamProvider
import com.example.playback.PlaybackSessionManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val databaseId: String = appContext.getString(R.string.firestore_database_id)
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(databaseId)
    }

    val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(appContext)
    }

    val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(Firebase.auth)
    }

    val userProfileRepository: UserProfileRepository by lazy {
        UserProfileRepository(firestore)
    }

    val genreDiscoveryRepository: GenreDiscoveryRepository by lazy {
        GenreDiscoveryRepository(firestore)
    }

    val catalogDatabase: NimeCatalogDatabase by lazy {
        NimeCatalogDatabase.getInstance(appContext)
    }

    val catalogProvider: AnimeCatalogProvider by lazy {
        CatalogProviderConfig.createActiveProvider(enableDebugLogs = BuildConfig.DEBUG)
    }

    val catalogRepository: AnimeCatalogRepository by lazy {
        AnimeCatalogRepository(
            provider = catalogProvider,
            dao = catalogDatabase.catalogDao()
        )
    }

    val streamProvider: AuthorizedStreamProvider by lazy {
        StreamProviderConfig.createStreamProvider(appContext)
    }

    val playbackManager: PlaybackSessionManager by lazy {
        PlaybackSessionManager(
            context = appContext,
            catalogRepository = catalogRepository,
            streamProvider = streamProvider,
            preferencesRepository = preferencesRepository,
            networkMonitor = networkMonitor
        )
    }
}
