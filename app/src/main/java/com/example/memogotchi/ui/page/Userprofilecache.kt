package com.example.memogotchi.ui.page

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

/**
 * Resolves a user's display name from users/{uid}.displayName, with a
 * simple in-memory cache since the same uid gets looked up repeatedly
 * across the buddy list, buddy detail, and incoming-request overlay.
 * Requires MainActivity's sign-in flow to write displayName alongside
 * fcmToken (see step8-setup-notes.txt) — falls back to a generic label
 * if it's missing (e.g. for accounts that signed in before this was added).
 */
object UserProfileCache {
    private val cache = mutableMapOf<String, String>()

    suspend fun getDisplayName(uid: String): String {
        cache[uid]?.let { return it }
        return try {
            val doc = Firebase.firestore.collection("users").document(uid).get().await()
            val name = doc.getString("displayName")?.takeIf { it.isNotBlank() } ?: "A memo"
            cache[uid] = name
            name
        } catch (e: Exception) {
            "A memo"
        }
    }
}