package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureProfileExists_authenticatedUser_createsAndReadsOwnProfile() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val user = checkNotNull(auth.currentUser)
        val repository = UserProfileRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureProfileExists(user, defaultDarkMode = true, defaultLanguage = "en")
        }
        assertTrue(createResult.isSuccess)
        val profile = createResult.getOrThrow()
        assertEquals(uid, profile.userId)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(uid).first { it != null }
        }
        assertEquals(uid, observed?.userId)
    }

    @Test
    fun saveProfileDetails_andUpdateAvatar_updatesFieldsSafely() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val user = checkNotNull(auth.currentUser)
        val repository = UserProfileRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureProfileExists(user).getOrThrow()
            val saveRes = repository.saveProfileDetails(
                firstName = "Akira",
                lastName = "Ren",
                surname = "Tachibana",
                displayName = "AkiraT",
                dateOfBirth = "2001-09-12",
                avatarId = "ninja_kage",
                termsAccepted = true,
                profileCompleted = true
            )
            assertTrue(saveRes.isSuccess)
            val avatarRes = repository.updateAvatarSelection(
                avatarId = "dark_phantom",
                customAvatarPath = null
            )
            assertTrue(avatarRes.isSuccess)
        }

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getProfile(uid).getOrThrow()
        }
        assertEquals("Akira", fetched?.firstName)
        assertEquals("Tachibana", fetched?.surname)
        assertEquals("2001-09-12", fetched?.dateOfBirth)
        assertEquals("dark_phantom", fetched?.avatarId)
        assertEquals(true, fetched?.profileCompleted)
        assertEquals(true, fetched?.termsAccepted)
    }

    @Test
    fun deleteUserProfileDocument_authenticatedOwner_removesRecord() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val user = checkNotNull(auth.currentUser)
        val repository = UserProfileRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureProfileExists(user).getOrThrow()
            val deleteRes = repository.deleteUserProfileDocument()
            assertTrue(deleteRes.isSuccess)
        }

        val afterDelete = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getProfile(uid).getOrThrow()
        }
        assertNull(afterDelete)
    }

    @Test
    fun updatePreferences_authenticatedOwner_updatesThemeAndLanguage() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val user = checkNotNull(auth.currentUser)
        val repository = UserProfileRepository(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureProfileExists(user).getOrThrow()
            val updateResult = repository.updatePreferences(
                darkModeEnabled = false,
                preferredLanguage = "bn"
            )
            assertTrue(updateResult.isSuccess)
        }

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getProfile(uid).getOrThrow()
        }
        assertEquals(false, fetched?.darkModeEnabled)
        assertEquals("bn", fetched?.preferredLanguage)
    }

    @Test
    fun getProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceUser = checkNotNull(auth.currentUser)
        val repository = UserProfileRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureProfileExists(aliceUser).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getProfile(aliceUid)
        }
        assertTrue(result.isFailure)
        val firestoreException = result.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun observeProfile_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = UserProfileRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProfile("some_uid_123").first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        val firestoreException = caught?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_nime@test.com"
        const val BOB_EMAIL = "bob_nime@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
