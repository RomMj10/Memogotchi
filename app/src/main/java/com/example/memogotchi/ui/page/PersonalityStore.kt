package com.example.memogotchi.ui.page

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// ════════════════════════════════════════════════════════════════════════════
//  MODELS
// ════════════════════════════════════════════════════════════════════════════

data class PersonalityProfile(
    val tagline: String,
    val traits: List<String>,        // exactly 3
    val paragraphs: List<String>,    // 2-3
    val generatedAtMs: Long = System.currentTimeMillis(),
)

// ════════════════════════════════════════════════════════════════════════════
//  STORE
// ════════════════════════════════════════════════════════════════════════════

object PersonalityStore {
    private const val PREFS = "memogotchi_personality"

    private const val KEY_TAG_CATEGORY_TALLY   = "tag_category_tally"      // Map<String label, Int>
    private const val KEY_SCREEN_CATEGORY_TALLY = "screen_category_tally" // Map<AppCategory name, Double hours>
    private const val KEY_LAST_ROLLUP_DAY      = "last_rollup_day_label"   // guards double-counting per day
    private const val KEY_PROFILE              = "cached_profile"
    private const val KEY_DIRTY                = "profile_dirty"

    // Thresholds per design doc
    private const val TAG_TALLY_THRESHOLD = 5
    private const val SCREEN_HOURS_THRESHOLD = 5.0
    private const val CATEGORIES_NEEDED = 2

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ── Tag-category tally (diary + goals feed this) ────────────────────────

    fun incrementTagCategoryTally(ctx: Context, tags: List<String>) {
        val labels = categoryLabelsForTags(tags)
        if (labels.isEmpty()) return
        val tally = loadTagCategoryTally(ctx).toMutableMap()
        labels.forEach { label -> tally[label] = (tally[label] ?: 0) + 1 }
        val obj = JSONObject()
        tally.forEach { (k, v) -> obj.put(k, v) }
        prefs(ctx).edit().putString(KEY_TAG_CATEGORY_TALLY, obj.toString()).apply()
        markDirty(ctx)
    }

    fun loadTagCategoryTally(ctx: Context): Map<String, Int> {
        val str = prefs(ctx).getString(KEY_TAG_CATEGORY_TALLY, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(str)
            obj.keys().asSequence().associateWith { obj.getInt(it) }
        } catch (e: Exception) { emptyMap() }
    }

    // ── Screen-time-category tally (cumulative hours, rolled up once/day) ───

    /**
     * Call this once per day when "today's" DayData/categorizedApps are available
     * (e.g. wherever generateAnalogTasks / generateTasksWithGemini already get called).
     * Guarded by dayLabel so re-opening the app the same day won't double-count.
     */
    // ── Replace the rollup function in PersonalityStore.kt with this ──────────

    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.O)
    fun rollupScreenCategoryTallyIfNeeded(ctx: Context, today: DayData?) {
        if (today == null) return
        val lastDay = prefs(ctx).getString(KEY_LAST_ROLLUP_DAY, null)
        if (lastDay == today.fullLabel) return // already rolled up today

        val hoursByCategory = today.apps
            .groupBy { getAppCategory(ctx, it.packageName) }
            .mapValues { (_, apps) -> apps.sumOf { it.totalTimeMs / 3_600_000.0 } }

        val tally = loadScreenCategoryTally(ctx).toMutableMap()
        hoursByCategory.forEach { (cat, hrs) -> tally[cat] = (tally[cat] ?: 0.0) + hrs }

        val obj = JSONObject()
        tally.forEach { (k, v) -> obj.put(k.name, v) }
        prefs(ctx).edit()
            .putString(KEY_SCREEN_CATEGORY_TALLY, obj.toString())
            .putString(KEY_LAST_ROLLUP_DAY, today.fullLabel)
            .apply()
        markDirty(ctx)
    }

    fun loadScreenCategoryTally(ctx: Context): Map<AppCategory, Double> {
        val str = prefs(ctx).getString(KEY_SCREEN_CATEGORY_TALLY, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(str)
            obj.keys().asSequence()
                .mapNotNull { key -> runCatching { AppCategory.valueOf(key) }.getOrNull()?.let { it to obj.getDouble(key) } }
                .toMap()
        } catch (e: Exception) { emptyMap() }
    }

    // ── Unlock check ──────────────────────────────────────────────────────

    fun isUnlocked(ctx: Context): Boolean {
        val tagHits = loadTagCategoryTally(ctx).count { it.value >= TAG_TALLY_THRESHOLD }
        val screenHits = loadScreenCategoryTally(ctx).count { it.value >= SCREEN_HOURS_THRESHOLD }
        return (tagHits + screenHits) >= CATEGORIES_NEEDED
    }

    // ── Dirty flag (profile may be stale) ────────────────────────────────

    fun markDirty(ctx: Context) {
        prefs(ctx).edit().putBoolean(KEY_DIRTY, true).apply()
    }

    fun clearDirty(ctx: Context) {
        prefs(ctx).edit().putBoolean(KEY_DIRTY, false).apply()
    }

    fun isDirty(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_DIRTY, false)

    // ── Cached profile ────────────────────────────────────────────────────

    fun saveProfile(ctx: Context, profile: PersonalityProfile) {
        val traitsArr = JSONArray().apply { profile.traits.forEach { put(it) } }
        val paraArr = JSONArray().apply { profile.paragraphs.forEach { put(it) } }
        val obj = JSONObject().apply {
            put("tagline", profile.tagline)
            put("traits", traitsArr)
            put("paragraphs", paraArr)
            put("generatedAtMs", profile.generatedAtMs)
        }
        prefs(ctx).edit().putString(KEY_PROFILE, obj.toString()).apply()
        clearDirty(ctx)
    }

    fun loadProfile(ctx: Context): PersonalityProfile? {
        val str = prefs(ctx).getString(KEY_PROFILE, null) ?: return null
        return try {
            val obj = JSONObject(str)
            val traitsArr = obj.getJSONArray("traits")
            val paraArr = obj.getJSONArray("paragraphs")
            PersonalityProfile(
                tagline = obj.getString("tagline"),
                traits = (0 until traitsArr.length()).map { traitsArr.getString(it) },
                paragraphs = (0 until paraArr.length()).map { paraArr.getString(it) },
                generatedAtMs = obj.optLong("generatedAtMs", 0L),
            )
        } catch (e: Exception) { null }
    }
}
