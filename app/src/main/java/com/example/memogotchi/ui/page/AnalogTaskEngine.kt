package com.example.memogotchi.ui.page

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.BatteryManager
import android.os.Build
import androidx.annotation.RequiresApi

// ════════════════════════════════════════════════════════════════════════════
//  MODELS
// ════════════════════════════════════════════════════════════════════════════
enum class AppCategory(val label: String, val emoji: String) {
    SOCIAL("Social",         "💬"),
    GAMES("Games",           "🎮"),
    ENTERTAINMENT("Video",   "📺"),
    PRODUCTIVITY("Work",     "💼"),
    BROWSER("Browsing",      "🌐"),
    OTHER("Other",           "📱"),
}

data class CategorizedApp(
    val info: AppUsageInfo,
    val category: AppCategory,
    val hours: Double,
)

data class AnalogTask(
    val id: String,
    val title: String,
    val description: String,
    val category: AppCategory,
    val durationMinutes: Int,
    val triggerReason: String,
    var isDone: Boolean = false,
)

// ════════════════════════════════════════════════════════════════════════════
//  APP CATEGORY HELPER
// ════════════════════════════════════════════════════════════════════════════

@RequiresApi(Build.VERSION_CODES.O)
fun getAppCategory(context: Context, packageName: String): AppCategory {
    return try {
        val pm   = context.packageManager
        val info = pm.getApplicationInfo(packageName, 0)

        when (info.category) {
            ApplicationInfo.CATEGORY_SOCIAL      -> AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_GAME        -> AppCategory.GAMES
            ApplicationInfo.CATEGORY_VIDEO       -> AppCategory.ENTERTAINMENT
            ApplicationInfo.CATEGORY_PRODUCTIVITY,
            ApplicationInfo.CATEGORY_NEWS        -> AppCategory.PRODUCTIVITY
            else                                 -> fallbackCategory(packageName)
        }
    } catch (e: Exception) {
        fallbackCategory(packageName)
    }
}

private fun fallbackCategory(pkg: String): AppCategory {
    val social        = listOf("instagram", "facebook", "twitter", "tiktok", "snapchat", "telegram", "whatsapp", "discord", "reddit", "linkedin", "pinterest", "tumblr","quora","messenger")
    val games         = listOf("supercell", "king.com", "roblox", "mojang", "gameloft", "game", "clash", "candy", "puzzle", "chess")
    val entertainment = listOf("youtube", "netflix", "spotify", "twitch", "hulu", "disneyplus", "hbo", "prime", "music", "video", "stream")
    val browser       = listOf("chrome", "firefox", "opera", "brave", "edge", "browser", "samsung.internet", "vivaldi")
    val productivity  = listOf("docs", "sheets", "gmail", "outlook", "notion", "slack", "zoom", "meet", "calendar", "drive", "office","gemini","chatgpt", "claude")

    return when {
        social.any        { pkg.contains(it, ignoreCase = true) } -> AppCategory.SOCIAL
        games.any         { pkg.contains(it, ignoreCase = true) } -> AppCategory.GAMES
        entertainment.any { pkg.contains(it, ignoreCase = true) } -> AppCategory.ENTERTAINMENT
        browser.any       { pkg.contains(it, ignoreCase = true) } -> AppCategory.BROWSER
        productivity.any  { pkg.contains(it, ignoreCase = true) } -> AppCategory.PRODUCTIVITY
        else                                                      -> AppCategory.OTHER
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  BATTERY HELPER
// ════════════════════════════════════════════════════════════════════════════

fun getBatteryLevel(context: Context): Int {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level  = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale  = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    return if (level >= 0 && scale > 0) (level * 100 / scale) else 50
}

// ════════════════════════════════════════════════════════════════════════════
//  TASK ENGINE
// ════════════════════════════════════════════════════════════════════════════
@RequiresApi(Build.VERSION_CODES.O)
fun generateAnalogTasks(
    context: Context,
    today: DayData?,
    batteryLevel: Int,
): List<AnalogTask> {
    if (today == null) return emptyList()

    val tasks = mutableListOf<AnalogTask>()
    val totalHours = today.totalMs / 3_600_000.0

    // Categorize top apps
    val categorized = today.apps.take(10).map { app ->
        CategorizedApp(
            info     = app,
            category = getAppCategory(context, app.packageName),
            hours    = app.totalTimeMs / 3_600_000.0,
        )
    }

    // Hours per category
    val byCategory = categorized.groupBy { it.category }
    val socialHours        = byCategory[AppCategory.SOCIAL]?.sumOf { it.hours } ?: 0.0
    val gameHours          = byCategory[AppCategory.GAMES]?.sumOf { it.hours } ?: 0.0
    val entertainmentHours = byCategory[AppCategory.ENTERTAINMENT]?.sumOf { it.hours } ?: 0.0
    val browseHours        = byCategory[AppCategory.BROWSER]?.sumOf { it.hours } ?: 0.0

    // Top social app name for personalised message
    val topSocialApp = byCategory[AppCategory.SOCIAL]?.maxByOrNull { it.hours }?.info?.appName

    // ── Social triggers ───────────────────────────────────────────────────
    if (socialHours >= 1.5) {
        tasks += AnalogTask(
            id              = "social_letter",
            title           = "Write to someone you miss",
            description     = "Put down the phone and write a short handwritten note or letter to a friend or family member.",
            category        = AppCategory.SOCIAL,
            durationMinutes = 20,
            triggerReason   = "${topSocialApp ?: "Social apps"}: ${formatHours(socialHours)} today",
        )
    }
    if (socialHours >= 3.0) {
        tasks += AnalogTask(
            id              = "social_meetup",
            title           = "Plan a real meetup",
            description     = "Instead of scrolling, reach out to someone and plan to meet in person this week.",
            category        = AppCategory.SOCIAL,
            durationMinutes = 10,
            triggerReason   = "${formatHours(socialHours)} on social media today",
        )
    }

    // ── Gaming triggers ───────────────────────────────────────────────────
    if (gameHours >= 1.0) {
        tasks += AnalogTask(
            id              = "game_walk",
            title           = "Go for a 15-min walk",
            description     = "Step outside for a short walk. No phone, just fresh air and movement.",
            category        = AppCategory.GAMES,
            durationMinutes = 15,
            triggerReason   = "Gaming: ${formatHours(gameHours)} today",
        )
    }
    if (gameHours >= 2.5) {
        tasks += AnalogTask(
            id              = "game_board",
            title           = "Play a board game or card game",
            description     = "Challenge someone at home to a physical game e.g, chess, cards, or anything analog.",
            category        = AppCategory.GAMES,
            durationMinutes = 30,
            triggerReason   = "${formatHours(gameHours)} of gaming today",
        )
    }

    // ── Entertainment triggers ────────────────────────────────────────────
    if (entertainmentHours >= 2.0) {
        tasks += AnalogTask(
            id              = "entertain_read",
            title           = "Read a physical book",
            description     = "Pick up a book you've been meaning to read. Even 20 minutes makes a difference.",
            category        = AppCategory.ENTERTAINMENT,
            durationMinutes = 20,
            triggerReason   = "Streaming/video: ${formatHours(entertainmentHours)} today",
        )
    }

    // ── Browsing triggers ─────────────────────────────────────────────────
    if (browseHours >= 1.5) {
        tasks += AnalogTask(
            id              = "browse_journal",
            title           = "Write in a journal",
            description     = "Instead of scrolling for information, reflect on your own thoughts for a few minutes.",
            category        = AppCategory.BROWSER,
            durationMinutes = 15,
            triggerReason   = "Browsing: ${formatHours(browseHours)} today",
        )
    }

    // ── Total screen time triggers ────────────────────────────────────────
    if (totalHours >= 2.0) {
        tasks += AnalogTask(
            id              = "total_stretch",
            title           = "5-minute stretch",
            description     = "Your body needs a break. Stand up, stretch your neck, shoulders, and back.",
            category        = AppCategory.OTHER,
            durationMinutes = 5,
            triggerReason   = "Total screen time: ${formatHours(totalHours)} today",
        )
    }
    if (totalHours >= 4.0) {
        tasks += AnalogTask(
            id              = "total_cook",
            title           = "Cook or prepare something",
            description     = "Make a snack or a full meal without looking at your phone. Be present in the kitchen.",
            category        = AppCategory.OTHER,
            durationMinutes = 30,
            triggerReason   = "${formatHours(totalHours)} total screen time today",
        )
    }

    // ── Battery triggers ──────────────────────────────────────────────────
    if (batteryLevel <= 20) {
        tasks += AnalogTask(
            id              = "battery_low",
            title           = "Put the phone down to charge",
            description     = "Your battery is at $batteryLevel%. Use this as a natural break — do something offline while it charges.",
            category        = AppCategory.OTHER,
            durationMinutes = 30,
            triggerReason   = "Battery at $batteryLevel%",
        )
    }

    return tasks.distinctBy { it.id }
}

// ── Helpers ───────────────────────────────────────────────────────────────

private fun formatHours(h: Double): String {
    val totalMin = (h * 60).toInt()
    val hrs = totalMin / 60
    val min = totalMin % 60
    return when {
        hrs > 0 && min > 0 -> "${hrs}h ${min}m"
        hrs > 0            -> "${hrs}h"
        else               -> "${min}m"
    }
}