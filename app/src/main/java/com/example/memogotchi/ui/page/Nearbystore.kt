package com.example.memogotchi.ui.page

import android.content.Context

enum class NearbyMode {
    OFF, PASSIVE, ACTIVE
}

object NearbyStore {
    private const val PREFS = "memogotchi_nearby"
    private const val KEY_MODE = "nearby_mode"

    fun loadMode(context: Context): NearbyMode {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, NearbyMode.OFF.name)
        return try {
            NearbyMode.valueOf(stored ?: NearbyMode.OFF.name)
        } catch (e: IllegalArgumentException) {
            NearbyMode.OFF
        }
    }

    fun saveMode(context: Context, mode: NearbyMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_MODE, mode.name).apply()
    }
}