package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.domain.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserProfileRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProfile(uid: String): Flow<UserProfile?> = flow {
        val path = "$USERS_COLLECTION/$uid"
        emitAll(
            db.collection(USERS_COLLECTION)
                .document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getProfile(uid: String): Result<UserProfile?> {
        val docRef = db.collection(USERS_COLLECTION).document(uid)
        return try {
            val snapshot = docRef.get().await()
            val profile = if (snapshot.exists()) {
                snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun ensureProfileExists(
        user: FirebaseUser,
        defaultDarkMode: Boolean = true,
        defaultLanguage: String = "en"
    ): Result<UserProfile> {
        val uid = requireUserId()
        require(uid == user.uid) { "Authenticated UID mismatch." }
        val docRef = db.collection(USERS_COLLECTION).document(uid)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val parsed = existing.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (parsed != null) return Result.success(parsed)
            }
            val fullNameParts = (user.displayName ?: "").trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            val inferredFirst = fullNameParts.firstOrNull().orEmpty().take(40)
            val inferredSurname = if (fullNameParts.size > 1) fullNameParts.last().take(40) else ""
            val newProfile = UserProfile(
                userId = uid,
                firstName = inferredFirst,
                lastName = "",
                surname = inferredSurname,
                displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Anime Fan",
                email = user.email?.takeIf { it.isNotBlank() } ?: "member@nimewatcher.app",
                dateOfBirth = "",
                avatarId = UserProfile.DEFAULT_AVATAR_ID,
                photoUrl = user.photoUrl?.toString(),
                profileCompleted = false,
                termsAccepted = false,
                preferredLanguage = if (defaultLanguage == "bn") "bn" else "en",
                darkModeEnabled = defaultDarkMode
            )
            docRef.set(newProfile.toCreateMap()).await()
            val createdSnapshot = docRef.get().await()
            val saved = createdSnapshot.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: newProfile
            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveProfileDetails(
        firstName: String,
        lastName: String,
        surname: String,
        displayName: String,
        dateOfBirth: String,
        avatarId: String,
        termsAccepted: Boolean,
        profileCompleted: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection(USERS_COLLECTION).document(uid)
        val cleanDisplay = displayName.trim().take(60)
            .ifBlank { listOf(firstName, surname).filter { it.isNotBlank() }.joinToString(" ").trim() }
            .ifBlank { "Anime Fan" }
            .take(60)

        val payload = mapOf(
            "firstName" to firstName.trim().take(40),
            "lastName" to lastName.trim().take(40),
            "surname" to surname.trim().take(40),
            "displayName" to cleanDisplay,
            "dateOfBirth" to dateOfBirth.trim().take(16),
            "avatarId" to avatarId.trim().take(64).ifBlank { UserProfile.DEFAULT_AVATAR_ID },
            "termsAccepted" to termsAccepted,
            "profileCompleted" to profileCompleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateAvatarSelection(
        avatarId: String,
        customAvatarPath: String? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection(USERS_COLLECTION).document(uid)
        val payload = mutableMapOf<String, Any>(
            "avatarId" to avatarId.trim().take(64).ifBlank { UserProfile.DEFAULT_AVATAR_ID },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!customAvatarPath.isNullOrBlank()) {
            payload["customAvatarPath"] = customAvatarPath.take(300)
        }
        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(
        darkModeEnabled: Boolean,
        preferredLanguage: String
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection(USERS_COLLECTION).document(uid)
        val payload = mapOf(
            "darkModeEnabled" to darkModeEnabled,
            "preferredLanguage" to if (preferredLanguage == "bn") "bn" else "en",
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateDisplayName(displayName: String): Result<Unit> {
        val uid = requireUserId()
        val trimmed = displayName.trim().take(60)
        require(trimmed.isNotEmpty()) { "Display name cannot be blank" }
        val docRef = db.collection(USERS_COLLECTION).document(uid)
        return try {
            docRef.update(
                mapOf(
                    "displayName" to trimmed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteUserProfileDocument(): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection(USERS_COLLECTION).document(uid)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        const val USERS_COLLECTION = "users"
    }
}
