package com.example.memogotchi.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth // Correctly manages auth references
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await // Enables .await() on tasks

class AuthRepository(private val context: Context) {

    suspend fun signInWithGoogle(): FirebaseUser? {
        return try {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId("634492632297-vidh980oh5rmlg1vv261f4n42u10fdhl.apps.googleusercontent.com")
                .setFilterByAuthorizedAccounts(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)

            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

            // Changed from Firebase.auth to FirebaseAuth.getInstance() for absolute clarity and robustness
            val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()

            authResult.user
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
