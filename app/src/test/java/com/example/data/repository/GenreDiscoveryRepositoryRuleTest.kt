package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.domain.model.AnimeFormat
import com.example.domain.model.AnimeSortOption
import com.example.domain.model.DiscoveryCategory
import com.example.domain.model.GenreDiscoveryPreference
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenreDiscoveryRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun uniqueEmail(prefix: String): String =
        "${prefix}_${System.nanoTime()}@test.com"

    @Test
    fun ensureDefaultsInitialized_seedsPreferencesAndStarterCategories() = runBlocking {
        val uid = signInTestUser(uniqueEmail("alice_init"))
        val repository = GenreDiscoveryRepository(firestore)

        val initRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureDefaultsInitialized(uid)
        }
        assertTrue(initRes.isSuccess)

        val prefs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePreferences(uid).first { it != null }
        }
        assertEquals(uid, prefs?.userId)
        assertEquals(GenreDiscoveryPreference.DEFAULT_PREFERRED_TAGS, prefs?.preferredTags)

        val categories = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCategories(uid).first { it.isNotEmpty() }
        }
        assertTrue(categories.size >= 8)
        assertTrue(categories.any { it.queryValue == "Cyberpunk" && it.filterKind == DiscoveryCategory.KIND_TAG })
    }

    @Test
    fun savePreferences_andTogglePreferredTag_updatesFirestoreState() = runBlocking {
        val uid = signInTestUser(uniqueEmail("alice_prefs"))
        val repository = GenreDiscoveryRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureDefaultsInitialized(uid).getOrThrow()
            repository.savePreferences(
                preferredTags = listOf("Psychological", "Cyberpunk"),
                sortBy = AnimeSortOption.SCORE,
                formatFilter = AnimeFormat.TV,
                onlyShowPreferred = true
            ).getOrThrow()
        }

        val updated = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePreferences(uid).first {
                it != null && it.sortBy == AnimeSortOption.SCORE.apiSort
            }
        }
        assertEquals(listOf("Psychological", "Cyberpunk"), updated?.preferredTags)
        assertEquals("TV", updated?.formatFilter)
        assertEquals(true, updated?.onlyShowPreferred)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.togglePreferredTag("Mecha", updated!!).getOrThrow()
        }
        val afterToggle = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePreferences(uid).first {
                it?.preferredTags?.contains("Mecha") == true
            }
        }
        assertTrue(afterToggle?.preferredTags?.contains("Mecha") == true)
    }

    @Test
    fun createPinAndDeleteCategory_worksForAuthenticatedOwner() = runBlocking {
        val uid = signInTestUser(uniqueEmail("alice_cats"))
        val repository = GenreDiscoveryRepository(firestore)

        val createdId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCategory(
                name = "Time Travel Gems",
                queryValue = "Time Travel",
                filterKind = DiscoveryCategory.KIND_TAG,
                accentKey = DiscoveryCategory.ACCENT_CYAN,
                isPinned = false,
                sortOrder = 10L
            ).getOrThrow()
        }
        assertTrue(createdId.startsWith("cat_"))

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.toggleCategoryPinned(createdId, isPinned = true).getOrThrow()
        }
        val categoriesWithPinned = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCategories(uid).first { list ->
                list.any { it.id == createdId && it.isPinned }
            }
        }
        assertTrue(categoriesWithPinned.any { it.id == createdId && it.isPinned })

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteCategory(createdId).getOrThrow()
        }
        val categoriesAfterDelete = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCategories(uid).first { list ->
                list.none { it.id == createdId }
            }
        }
        assertFalse(categoriesAfterDelete.any { it.id == createdId })
    }

    @Test
    fun observePreferences_crossUserAndUnauthenticated_failWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(uniqueEmail("alice_sec"))
        val repository = GenreDiscoveryRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureDefaultsInitialized(aliceUid).getOrThrow()
        }

        // Cross-user read attempt by Bob
        signInTestUser(uniqueEmail("bob_sec"))
        var crossUserError: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCategories(aliceUid).first()
            }
        } catch (t: Throwable) {
            crossUserError = t
        }
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            crossUserError?.unwrapFirestoreException()?.code
        )

        // Unauthenticated read attempt
        auth.signOut()
        var unauthError: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observePreferences(aliceUid).first()
            }
        } catch (t: Throwable) {
            unauthError = t
        }
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            unauthError?.unwrapFirestoreException()?.code
        )
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
