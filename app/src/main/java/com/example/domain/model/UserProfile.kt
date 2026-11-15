package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val surname: String = "",
    val displayName: String = "",
    val email: String = "",
    val dateOfBirth: String = "",
    val avatarId: String = DEFAULT_AVATAR_ID,
    val photoUrl: String? = null,
    val customAvatarPath: String? = null,
    val profileCompleted: Boolean = false,
    val termsAccepted: Boolean = false,
    val preferredLanguage: String = "en",
    val darkModeEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> {
        val base = mutableMapOf<String, Any>(
            "userId" to userId,
            "firstName" to firstName.trim().take(40),
            "lastName" to lastName.trim().take(40),
            "surname" to surname.trim().take(40),
            "displayName" to displayName.trim().take(60).ifBlank { "Anime Fan" },
            "email" to email.trim().take(120).ifBlank { "member@nimewatcher.app" },
            "dateOfBirth" to dateOfBirth.trim().take(16),
            "avatarId" to avatarId.trim().take(64).ifBlank { DEFAULT_AVATAR_ID },
            "profileCompleted" to profileCompleted,
            "termsAccepted" to termsAccepted,
            "preferredLanguage" to if (preferredLanguage == "bn") "bn" else "en",
            "darkModeEnabled" to darkModeEnabled,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        photoUrl?.takeIf { it.isNotBlank() }?.let { base["photoUrl"] = it.take(500) }
        customAvatarPath?.takeIf { it.isNotBlank() }?.let { base["customAvatarPath"] = it.take(300) }
        return base
    }

    companion object {
        const val DEFAULT_AVATAR_ID = "samurai_ember"
        const val AVATAR_GOOGLE_PHOTO = "google_photo"
        const val AVATAR_CUSTOM_PHOTO = "custom_photo"
    }
}
