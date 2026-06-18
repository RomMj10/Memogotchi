package com.example.memogotchi.ui.page

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object WellnessStore {
    private const val PREFS       = "memogotchi_wellness"
    private const val KEY_SLIDERS = "wellness_sliders"
    private const val KEY_DIARY   = "diary_entries"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ── Sliders ──────────────────────────────────────────────────────────────

    fun saveSliders(ctx: Context, values: List<Float>) {
        val arr = JSONArray().apply { values.forEach { put(it.toDouble()) } }
        prefs(ctx).edit().putString(KEY_SLIDERS, arr.toString()).apply()
    }

    fun loadSliders(ctx: Context): List<Float> {
        val str = prefs(ctx).getString(KEY_SLIDERS, null)
            ?: return listOf(50f, 50f, 50f, 50f)
        return try {
            val arr = JSONArray(str)
            (0 until arr.length()).map { arr.getDouble(it).toFloat() }
        } catch (e: Exception) { listOf(50f, 50f, 50f, 50f) }
    }

    // ── Diary entries ─────────────────────────────────────────────────────────

    fun saveDiaryEntries(ctx: Context, entries: List<DiaryEntry>) {
        val arr = JSONArray()
        entries.takeLast(200).forEach { arr.put(entryToJson(it)) }
        prefs(ctx).edit().putString(KEY_DIARY, arr.toString()).apply()
    }

    fun loadDiaryEntries(ctx: Context): List<DiaryEntry> {
        val str = prefs(ctx).getString(KEY_DIARY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(str)
            (0 until arr.length()).map { entryFromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────

    private fun entryToJson(e: DiaryEntry): JSONObject {
        val catsArr = JSONArray().apply { e.categories.forEach { put(it) } }
        val snapArr = JSONArray().apply { e.sliderSnapshot?.forEach { put(it.toDouble()) } }
        return JSONObject().apply {
            put("id",            e.id)
            put("dateLabel",     e.dateLabel)
            put("dayLabel",      e.dayLabel)
            put("sortKey",       e.sortKey)
            put("text",          e.text)
            put("categories",    catsArr)
            put("sliderSnapshot", if (e.sliderSnapshot != null) snapArr else JSONObject.NULL)
            put("timeLabel", e.timeLabel)
        }
    }

    private fun entryFromJson(o: JSONObject): DiaryEntry {
        val catsArr = o.optJSONArray("categories")
        val cats = if (catsArr != null)
            (0 until catsArr.length()).map { catsArr.getString(it) }
        else emptyList()

        val snapArr = o.optJSONArray("sliderSnapshot")
        val snap = if (snapArr != null)
            (0 until snapArr.length()).map { snapArr.getDouble(it).toFloat() }
        else null

        return DiaryEntry(
            id            = o.optString("id", java.util.UUID.randomUUID().toString()),
            dateLabel     = o.getString("dateLabel"),
            dayLabel      = o.optString("dayLabel", ""),
            sortKey       = o.optLong("sortKey", 0L),
            text          = o.getString("text"),
            categories    = cats,
            sliderSnapshot = snap,
            timeLabel = o.optString("timeLabel", ""),
        )
    }
}