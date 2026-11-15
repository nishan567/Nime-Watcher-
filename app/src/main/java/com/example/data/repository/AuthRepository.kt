package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult
    data object Cancelled : AuthResult
    data class Error(val message: String) : AuthResult
}

sealed interface DeleteAccountResult {
    data object Deleted : DeleteAccountResult
    data object RequiresRecentLogin : DeleteAccountResult
    data class Error(val message: String) : DeleteAccountResult
}

class AuthRepository(
    private val auth: FirebaseAuth = Firebase.auth
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun attemptSilentGoogleSignIn(context: Context): FirebaseUser? {
        auth.currentUser?.let { return it }
        val clientId = runCatching { context.getString(R.string.default_web_client_id) }.getOrNull()
            ?: return null

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(firebaseCredential).await().user
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Silent Google sign-in skipped: ${e.message}")
            null
        }
    }

    suspend fun signInWithGoogleInteractive(activity: Activity): AuthResult {
        val clientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            return AuthResult.Error("Google Sign-In configuration missing: default_web_client_id not found")
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val credentialManager = CredentialManager.create(activity)
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val user = auth.signInWithCredential(firebaseCredential).await().user
                if (user != null) {
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Authentication completed without a Firebase user.")
                }
            } else {
                AuthResult.Error("Unexpected credential type received.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
            AuthResult.Cancelled
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            AuthResult.Error(e.localizedMessage ?: "Google Sign-In failed. Please try again.")
        }
    }

    suspend fun deleteFirebaseUser(context: Context): DeleteAccountResult {
        val user = auth.currentUser ?: return DeleteAccountResult.Error("No signed-in user.")
        return try {
            user.delete().await()
            withContext(NonCancellable) {
                runCatching {
                    CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                }
            }
            DeleteAccountResult.Deleted
        } catch (_: FirebaseAuthRecentLoginRequiredException) {
            DeleteAccountResult.RequiresRecentLogin
        } catch (e: Exception) {
            DeleteAccountResult.Error(e.localizedMessage ?: "Failed to delete account.")
        }
    }

    suspend fun signOut(context: Context) {
        withContext(NonCancellable) {
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                auth.signOut()
            }
        }
    }

    private companion object {
        const val TAG = "Auth"
    }
}
