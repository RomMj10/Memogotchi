package com.example.memogotchi.fcm

import androidx.compose.runtime.mutableStateOf

/**
 * Holds deep-link data extracted from a tapped push notification's Intent
 * extras. MainActivity writes to this on cold start (onCreate) and warm
 * start (onNewIntent); Step 6's navigation logic reads it to jump straight
 * to the Standby screen with the right matchId, then should call
 * consume() once it's been handled so re-composition doesn't re-trigger
 * the navigation repeatedly.
 */
object NearbyDeepLinkState {
    var pendingMatchId = mutableStateOf<String?>(null)
        private set

    fun set(matchId: String?) {
        pendingMatchId.value = matchId
    }

    fun consume(): String? {
        val value = pendingMatchId.value
        pendingMatchId.value = null
        return value
    }
}