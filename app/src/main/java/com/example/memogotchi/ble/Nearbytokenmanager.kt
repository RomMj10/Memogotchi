package com.example.memogotchi.ble

import android.util.Log
import com.example.memogotchi.api.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the current rotating nearby-token in memory and refreshes it via
 * the backend (POST /nearby/token) before it expires. The backend issues
 * tokens with a ~20 minute TTL; this refreshes a bit early so the
 * advertiser never briefly broadcasts a token the server has already
 * invalidated.
 */
object NearbyTokenManager {
    private const val TAG = "NearbyTokenManager"
    private const val REFRESH_SAFETY_MARGIN_MS = 60_000L

    private val _currentTokenHex = MutableStateFlow<String?>(null)
    val currentTokenHex: StateFlow<String?> = _currentTokenHex

    private var expiresAtMillis: Long = 0L

    suspend fun ensureFreshToken(): String? {
        val now = System.currentTimeMillis()
        val cached = _currentTokenHex.value
        if (cached != null && now < expiresAtMillis - REFRESH_SAFETY_MARGIN_MS) {
            return cached
        }
        return try {
            val response = ApiClient.service.requestNearbyToken()
            _currentTokenHex.value = response.token
            expiresAtMillis = response.expiresAtMillis
            response.token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh nearby token", e)
            null
        }
    }

    fun clear() {
        _currentTokenHex.value = null
        expiresAtMillis = 0L
    }
}