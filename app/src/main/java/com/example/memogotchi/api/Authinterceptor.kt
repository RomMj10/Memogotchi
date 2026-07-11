package com.example.memogotchi.api

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Attaches "Authorization: Bearer <firebase-id-token>" to every request.
 * OkHttp interceptors run synchronously (not suspend functions), so we use
 * Tasks.await(...) with a timeout rather than a coroutine here.
 *
 * On a 401 response, retries exactly once with a force-refreshed token —
 * covers the case where the cached token expired mid-session.
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val token = getIdTokenBlocking(forceRefresh = false)
        val requestWithAuth = if (token != null) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }

        val response = chain.proceed(requestWithAuth)

        if (response.code == 401) {
            response.close()
            val refreshedToken = getIdTokenBlocking(forceRefresh = true)
            if (refreshedToken != null) {
                val retryRequest = original.newBuilder()
                    .header("Authorization", "Bearer $refreshedToken")
                    .build()
                return chain.proceed(retryRequest)
            }
        }

        return response
    }

    private fun getIdTokenBlocking(forceRefresh: Boolean): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            val result = Tasks.await(user.getIdToken(forceRefresh), 10, TimeUnit.SECONDS)
            result?.token
        } catch (e: Exception) {
            Log.e("AuthInterceptor", "Failed to get ID token", e)
            null
        }
    }
}