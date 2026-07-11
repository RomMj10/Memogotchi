package com.example.memogotchi.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    val service: GoalBuddyApiService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Set to NONE for release builds if you gate this by BuildConfig.DEBUG
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        // Retrofit requires the base URL to end with a trailing slash, and
        // every @POST path in GoalBuddyApiService is relative (no leading "/").
        val baseUrl = ApiConfig.BASE_URL.let {
            if (it.endsWith("/")) it else "$it/"
        }

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoalBuddyApiService::class.java)
    }
}