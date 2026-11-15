package com.example.ui.screens

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.network.NetworkMonitor
import com.example.core.preferences.AppLanguage
import com.example.core.preferences.UserPreferences
import com.example.core.preferences.UserPreferencesRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.DeleteAccountResult
import com.example.data.repository.UserProfileRepository
import com.example.domain.model.UiState
import com.example.domain.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val currentUserId: String,
    private val authRepository: AuthRepository,
    private val profileRepository: UserProfileRepository,
    private val preferencesRepository: UserPreferencesRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = true
    )

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UserPreferences()
    )

    val profileState: StateFlow<UiState<UserProfile>> = profileRepository.observeProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile>> { profile ->
            if (profile != null) UiState.Success(profile) else UiState.Empty
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to load user profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        ensureProfileInitialized()
    }

    fun ensureProfileInitialized() {
        val firebaseUser = Firebase.auth.currentUser ?: return
        if (firebaseUser.uid != currentUserId) return
        viewModelScope.launch {
            val prefs = preferences.value
            profileRepository.ensureProfileExists(
                user = firebaseUser,
                defaultDarkMode = prefs.darkModeEnabled,
                defaultLanguage = prefs.language.code
            )
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkModeEnabled(enabled)
            profileRepository.updatePreferences(
                darkModeEnabled = enabled,
                preferredLanguage = preferences.value.language.code
            )
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
            profileRepository.updatePreferences(
                darkModeEnabled = preferences.value.darkModeEnabled,
                preferredLanguage = language.code
            )
        }
    }

    fun toggleRememberMe(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRememberMeEnabled(enabled)
        }
    }

    fun toggleBackgroundPlayback(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBackgroundPlaybackEnabled(enabled)
        }
    }

    fun toggleAutoPip(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoPipEnabled(enabled)
        }
    }

    fun toggleBackgroundAudio(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBackgroundAudioEnabled(enabled)
        }
    }

    fun toggleLockScreenControls(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setLockScreenControlsEnabled(enabled)
        }
    }

    fun toggleAutoNextEpisode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoNextEpisodeEnabled(enabled)
        }
    }

    fun updateDisplayName(newName: String) {
        val cleaned = newName.trim()
        if (cleaned.isEmpty()) {
            _statusMessage.value = "Display name cannot be empty."
            return
        }
        viewModelScope.launch {
            profileRepository.updateDisplayName(cleaned)
                .onSuccess { _statusMessage.value = "Display name updated." }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Update failed." }
        }
    }

    fun saveProfileDetails(
        firstName: String,
        lastName: String,
        surname: String,
        displayName: String,
        dateOfBirth: String,
        termsAccepted: Boolean
    ) {
        val currentAvatar = (profileState.value as? UiState.Success)?.data?.avatarId
            ?: UserProfile.DEFAULT_AVATAR_ID
        viewModelScope.launch {
            profileRepository.saveProfileDetails(
                firstName = firstName,
                lastName = lastName,
                surname = surname,
                displayName = displayName,
                dateOfBirth = dateOfBirth,
                avatarId = currentAvatar,
                termsAccepted = termsAccepted,
                profileCompleted = true
            )
                .onSuccess { _statusMessage.value = "Profile saved to cloud." }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save profile." }
        }
    }

    fun updateAvatarSelection(avatarId: String, customAvatarPath: String?) {
        viewModelScope.launch {
            profileRepository.updateAvatarSelection(avatarId, customAvatarPath)
                .onSuccess { _statusMessage.value = "Anime avatar updated." }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update avatar." }
        }
    }

    fun deleteAccount(context: Context) {
        viewModelScope.launch {
            profileRepository.deleteUserProfileDocument()
                .onFailure { err ->
                    _statusMessage.value = err.localizedMessage ?: "Failed to delete cloud profile."
                    return@launch
                }
            when (val res = authRepository.deleteFirebaseUser(context)) {
                DeleteAccountResult.Deleted -> {
                    _statusMessage.value = null
                }
                DeleteAccountResult.RequiresRecentLogin -> {
                    _statusMessage.value =
                        "Profile data removed. For security, please sign out and sign in again to delete your Google Auth link."
                }
                is DeleteAccountResult.Error -> {
                    _statusMessage.value = res.message
                }
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    class Factory(
        private val currentUserId: String,
        private val authRepository: AuthRepository,
        private val profileRepository: UserProfileRepository,
        private val preferencesRepository: UserPreferencesRepository,
        private val networkMonitor: NetworkMonitor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                currentUserId = currentUserId,
                authRepository = authRepository,
                profileRepository = profileRepository,
                preferencesRepository = preferencesRepository,
                networkMonitor = networkMonitor
            ) as T
        }
    }

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
