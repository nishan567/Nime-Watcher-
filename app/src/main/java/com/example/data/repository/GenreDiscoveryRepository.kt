package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.domain.model.AnimeFormat
import com.example.domain.model.AnimeSortOption
import com.example.domain.model.DiscoveryCategory
import com.example.domain.model.GenreDiscoveryCatalog
import com.example.domain.model.GenreDiscoveryPreference
import com.google.firebase.Firebase
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
import java.util.UUID

class GenreDiscoveryRepository(
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

    fun observePreferences(uid: String): Flow<GenreDiscoveryPreference?> = flow {
        val docPath = "${UserProfileRepository.USERS_COLLECTION}/$uid/$PREFERENCES_COLLECTION/${GenreDiscoveryPreference.DEFAULT_DOC_ID}"
        emitAll(
            db.collection(UserProfileRepository.USERS_COLLECTION)
                .document(uid)
                .collection(PREFERENCES_COLLECTION)
                .document(GenreDiscoveryPreference.DEFAULT_DOC_ID)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            GenreDiscoveryPreference::class.java,
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
                        handleFirestoreError(exceptionToLog, OperationType.GET, docPath)
                    }
                    throw error
                }
        )
    }

    fun observeCategories(uid: String): Flow<List<DiscoveryCategory>> = flow {
        val colPath = "${UserProfileRepository.USERS_COLLECTION}/$uid/$CATEGORIES_COLLECTION"
        emitAll(
            db.collection(UserProfileRepository.USERS_COLLECTION)
                .document(uid)
                .collection(CATEGORIES_COLLECTION)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(
                            DiscoveryCategory::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }.sortedWith(
                        compareByDescending<DiscoveryCategory> { it.isPinned }
                            .thenBy { it.sortOrder }
                            .thenBy { it.name.lowercase() }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colPath)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureDefaultsInitialized(userId: String = requireUserId()): Result<Unit> {
        val uid = requireUserId()
        require(uid == userId) { "Authenticated UID mismatch." }
        val userDoc = db.collection(UserProfileRepository.USERS_COLLECTION).document(uid)
        val prefDoc = userDoc.collection(PREFERENCES_COLLECTION)
            .document(GenreDiscoveryPreference.DEFAULT_DOC_ID)
        val categoriesCol = userDoc.collection(CATEGORIES_COLLECTION)

        return try {
            val prefSnap = prefDoc.get().await()
            if (!prefSnap.exists()) {
                val defaultPrefs = GenreDiscoveryPreference(userId = uid)
                prefDoc.set(defaultPrefs.toCreateMap()).await()
            }

            val existingCats = categoriesCol.limit(1).get().await()
            if (existingCats.isEmpty) {
                val batch = db.batch()
                GenreDiscoveryCatalog.starterCategories(uid).forEach { (docId, category) ->
                    batch.set(categoriesCol.document(docId), category.toCreateMap())
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDoc.path)
            Result.failure(e)
        }
    }

    suspend fun savePreferences(
        preferredTags: List<String>,
        sortBy: AnimeSortOption,
        formatFilter: AnimeFormat?,
        onlyShowPreferred: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection(UserProfileRepository.USERS_COLLECTION)
            .document(uid)
            .collection(PREFERENCES_COLLECTION)
            .document(GenreDiscoveryPreference.DEFAULT_DOC_ID)

        val cleanedTags = preferredTags
            .map { it.trim().take(40) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(25)
        val formatKey = formatFilter?.takeIf { it != AnimeFormat.UNKNOWN }?.name
            ?: GenreDiscoveryPreference.FORMAT_ALL

        return try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val createModel = GenreDiscoveryPreference(
                    userId = uid,
                    preferredTags = cleanedTags,
                    sortBy = sortBy.apiSort,
                    formatFilter = formatKey,
                    onlyShowPreferred = onlyShowPreferred
                )
                docRef.set(createModel.toCreateMap()).await()
            } else {
                docRef.update(
                    mapOf(
                        "preferredTags" to cleanedTags,
                        "sortBy" to sortBy.apiSort,
                        "formatFilter" to formatKey,
                        "onlyShowPreferred" to onlyShowPreferred,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun togglePreferredTag(
        tag: String,
        current: GenreDiscoveryPreference
    ): Result<Unit> {
        val normalized = tag.trim()
        val updated = if (current.preferredTags.any { it.equals(normalized, ignoreCase = true) }) {
            current.preferredTags.filterNot { it.equals(normalized, ignoreCase = true) }
        } else {
            (current.preferredTags + normalized).distinct().take(25)
        }
        return savePreferences(
            preferredTags = updated,
            sortBy = current.toSortOption(),
            formatFilter = current.toFormatOrNull(),
            onlyShowPreferred = current.onlyShowPreferred
        )
    }

    suspend fun createCategory(
        name: String,
        queryValue: String,
        filterKind: String,
        accentKey: String,
        isPinned: Boolean,
        sortOrder: Long = 50L
    ): Result<String> {
        val uid = requireUserId()
        val cleanName = name.trim().take(50)
        val cleanQuery = queryValue.trim().take(40)
        require(cleanName.isNotEmpty()) { "Category name cannot be blank" }
        require(cleanQuery.isNotEmpty()) { "Category tag cannot be blank" }

        val slug = cleanQuery.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(24)
            .ifBlank { "tag" }
        val docId = "cat_${slug}_${UUID.randomUUID().toString().replace("-", "").take(6)}"

        val docRef = db.collection(UserProfileRepository.USERS_COLLECTION)
            .document(uid)
            .collection(CATEGORIES_COLLECTION)
            .document(docId)

        val category = DiscoveryCategory(
            userId = uid,
            name = cleanName,
            queryValue = cleanQuery,
            filterKind = if (filterKind == DiscoveryCategory.KIND_TAG) DiscoveryCategory.KIND_TAG else DiscoveryCategory.KIND_GENRE,
            accentKey = accentKey,
            isPinned = isPinned,
            sortOrder = sortOrder.coerceIn(0L, 500L)
        )

        return try {
            docRef.set(category.toCreateMap()).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleCategoryPinned(categoryId: String, isPinned: Boolean): Result<Unit> {
        val uid = requireUserId()
        require(categoryId.isNotBlank()) { "Category ID cannot be blank" }
        val docRef = db.collection(UserProfileRepository.USERS_COLLECTION)
            .document(uid)
            .collection(CATEGORIES_COLLECTION)
            .document(categoryId)

        return try {
            docRef.update(
                mapOf(
                    "isPinned" to isPinned,
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

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        val uid = requireUserId()
        require(categoryId.isNotBlank()) { "Category ID cannot be blank" }
        val docRef = db.collection(UserProfileRepository.USERS_COLLECTION)
            .document(uid)
            .collection(CATEGORIES_COLLECTION)
            .document(categoryId)

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

    suspend fun resetDefaultCategories(): Result<Unit> {
        val uid = requireUserId()
        val categoriesCol = db.collection(UserProfileRepository.USERS_COLLECTION)
            .document(uid)
            .collection(CATEGORIES_COLLECTION)
        return try {
            val batch = db.batch()
            GenreDiscoveryCatalog.starterCategories(uid).forEach { (docId, category) ->
                batch.set(categoriesCol.document(docId), category.toCreateMap())
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, categoriesCol.path)
            Result.failure(e)
        }
    }

    companion object {
        const val PREFERENCES_COLLECTION = "genre_preferences"
        const val CATEGORIES_COLLECTION = "discovery_categories"
    }
}
