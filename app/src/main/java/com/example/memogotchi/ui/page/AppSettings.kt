package com.example.memogotchi.ui.page

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class TextSizeOption(val label: String, val scale: Float) {
    SMALL("Small", 0.75f),
    NORMAL("Normal", 1.0f),
    LARGE("Large", 1.2f),
}

object AppSettings {
    private const val PREFS = "memogotchi_settings"
    private const val KEY_DAILY_LIMIT = "daily_limit_minutes"
    private const val KEY_HEALTH_ALERTS = "health_alerts"
    private const val KEY_TEXT_SIZE = "text_size"

    var dailyLimitMinutes by mutableStateOf(150) // default 2h 30m
    var healthAlertsEnabled by mutableStateOf(true)
    var textSize by mutableStateOf(TextSizeOption.NORMAL)
    var bgIndex by mutableStateOf(0)
    var surfaceIndex by mutableStateOf(0)
    var accentIndex by mutableStateOf(0)
    var textPrimaryIndex by mutableStateOf(0)
    var textSecondaryIndex by mutableStateOf(0)

    private var initialized = false


    fun init(context: Context) {
        if (initialized) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        dailyLimitMinutes = prefs.getInt(KEY_DAILY_LIMIT, 150)
        healthAlertsEnabled = prefs.getBoolean(KEY_HEALTH_ALERTS, true)
        textSize = runCatching {
            TextSizeOption.valueOf(prefs.getString(KEY_TEXT_SIZE, TextSizeOption.NORMAL.name)!!)
        }.getOrDefault(TextSizeOption.NORMAL)
        initialized = true

        bgIndex = prefs.getInt("bg_index", 0)
        surfaceIndex = prefs.getInt("surface_index", 0)
        accentIndex = prefs.getInt("accent_index", 0)
        textPrimaryIndex = prefs.getInt("text_primary_index", 0)
        textSecondaryIndex = prefs.getInt("text_secondary_index", 0)
    }

    fun setColorIndex(context: Context, key:String, index:Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(key, index).apply()
    }

    fun setDailyLimitMinutes(context: Context, minutes: Int) {
        dailyLimitMinutes = minutes
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_DAILY_LIMIT, minutes).apply()
    }

    fun setHealthAlertsEnabled(context: Context, enabled: Boolean) {
        healthAlertsEnabled = enabled
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_HEALTH_ALERTS, enabled).apply()
    }

    fun setTextSize(context: Context, size: TextSizeOption) {
        textSize = size
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TEXT_SIZE, size.name).apply()
    }

    fun currentAppColors(): com.example.memogotchi.ui.theme.AppColors {
        val p = com.example.memogotchi.ui.theme.ColorPresets
        val accentColor = p.accent[accentIndex]
        return com.example.memogotchi.ui.theme.AppColors(
            bg = p.bg[bgIndex],
            surface = p.surface[surfaceIndex],
            accent = accentColor,
            accentLight = accentColor,
            accentDark = accentColor,
            textPrimary = p.textPrimary[textPrimaryIndex],
            textSecondary = p.textSecondary[textSecondaryIndex],
        )
    }
}
