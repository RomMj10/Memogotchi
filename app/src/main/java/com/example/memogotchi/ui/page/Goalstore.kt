package com.example.memogotchi.ui.page

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// ════════════════════════════════════════════════════════════════════════════
//  MODELS
// ════════════════════════════════════════════════════════════════════════════

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false,
)

enum class GoalReminderMode { NONE, PINNED, SCHEDULED }

/**
 * hour/minute: 24h time, default 0:00 (midnight)
 * daysOfWeek: empty set = every day. Otherwise Calendar.DAY_OF_WEEK values (1=Sun..7=Sat)
 */
data class GoalSchedule(
    val hour: Int = 0,
    val minute: Int = 0,
    val daysOfWeek: Set<Int> = emptySet(), // empty = every day
) {
    val isEveryDay: Boolean get() = daysOfWeek.isEmpty()
}

data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: AppCategory? = null,        // non-null = screen-time auto-tracked
    val targetMinutes: Int? = null,            // daily limit, only relevant if category != null
    val checklist: List<ChecklistItem> = emptyList(),  // optional, independent of isMajor
    val isMajor: Boolean = false,              // user-set flag, independent of checklist
    val tags: List<String> = emptyList(),      // same tag set as diary entries
    val reminderMode: GoalReminderMode = GoalReminderMode.NONE,
    val schedule: GoalSchedule = GoalSchedule(),
    val deadlineMs: Long? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
    val isDone: Boolean = false,               // manual completion (minor, non-screen-time goals)
    val completedAtMs: Long? = null,
) {
    val hasChecklist: Boolean get() = checklist.isNotEmpty()
    val isScreenTimeTracked: Boolean get() = category != null && targetMinutes != null

    fun checklistProgress(): Pair<Int, Int> {
        val done = checklist.count { it.isDone }
        return done to checklist.size
    }

    fun isEffectivelyDone(): Boolean {
        return if (hasChecklist) checklist.all { it.isDone } else isDone
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  STORE
// ════════════════════════════════════════════════════════════════════════════

object GoalStore {
    private const val PREFS    = "memogotchi_goals"
    private const val KEY_GOALS = "goals"
    private const val KEY_TAG_TALLY = "tag_tally"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveGoals(ctx: Context, goals: List<Goal>) {
        val arr = JSONArray()
        goals.forEach { arr.put(goalToJson(it)) }
        prefs(ctx).edit().putString(KEY_GOALS, arr.toString()).apply()
    }

    fun loadGoals(ctx: Context): List<Goal> {
        val str = prefs(ctx).getString(KEY_GOALS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(str)
            (0 until arr.length()).map { goalFromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }

    // ── Tag tally (personality profile data, stored only — no behavior yet) ────

    fun incrementTagTally(ctx: Context, tags: List<String>) {
        if (tags.isEmpty()) return
        val tally = loadTagTally(ctx).toMutableMap()
        tags.forEach { tag -> tally[tag] = (tally[tag] ?: 0) + 1 }
        val obj = JSONObject()
        tally.forEach { (k, v) -> obj.put(k, v) }
        prefs(ctx).edit().putString(KEY_TAG_TALLY, obj.toString()).apply()
    }

    fun loadTagTally(ctx: Context): Map<String, Int> {
        val str = prefs(ctx).getString(KEY_TAG_TALLY, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(str)
            obj.keys().asSequence().associateWith { obj.getInt(it) }
        } catch (e: Exception) { emptyMap() }
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────

    private fun checklistItemToJson(c: ChecklistItem) = JSONObject().apply {
        put("id", c.id)
        put("text", c.text)
        put("isDone", c.isDone)
    }

    private fun checklistItemFromJson(o: JSONObject) = ChecklistItem(
        id     = o.optString("id", UUID.randomUUID().toString()),
        text   = o.getString("text"),
        isDone = o.optBoolean("isDone", false),
    )

    private fun scheduleToJson(s: GoalSchedule): JSONObject {
        val daysArr = JSONArray()
        s.daysOfWeek.forEach { daysArr.put(it) }
        return JSONObject().apply {
            put("hour", s.hour)
            put("minute", s.minute)
            put("daysOfWeek", daysArr)
        }
    }

    private fun scheduleFromJson(o: JSONObject?): GoalSchedule {
        if (o == null) return GoalSchedule()
        val daysArr = o.optJSONArray("daysOfWeek")
        val days = if (daysArr != null)
            (0 until daysArr.length()).map { daysArr.getInt(it) }.toSet()
        else emptySet()
        return GoalSchedule(
            hour = o.optInt("hour", 0),
            minute = o.optInt("minute", 0),
            daysOfWeek = days,
        )
    }

    private fun goalToJson(g: Goal): JSONObject {
        val checklistArr = JSONArray()
        g.checklist.forEach { checklistArr.put(checklistItemToJson(it)) }

        val tagsArr = JSONArray()
        g.tags.forEach { tagsArr.put(it) }

        return JSONObject().apply {
            put("id",            g.id)
            put("title",         g.title)
            put("description",   g.description)
            put("category",      g.category?.name ?: JSONObject.NULL)
            put("targetMinutes", g.targetMinutes ?: JSONObject.NULL)
            put("checklist",     checklistArr)
            put("isMajor",       g.isMajor)
            put("tags",          tagsArr)
            put("reminderMode",  g.reminderMode.name)
            put("schedule",      scheduleToJson(g.schedule))
            put("deadlineMs",    g.deadlineMs ?: JSONObject.NULL)
            put("createdAtMs",   g.createdAtMs)
            put("isDone",        g.isDone)
            put("completedAtMs", g.completedAtMs ?: JSONObject.NULL)
        }
    }

    private fun goalFromJson(o: JSONObject): Goal {
        val checklistArr = o.optJSONArray("checklist")
        val checklist = if (checklistArr != null)
            (0 until checklistArr.length()).map { checklistItemFromJson(checklistArr.getJSONObject(it)) }
        else emptyList()

        val tagsArr = o.optJSONArray("tags")
        val tags = if (tagsArr != null)
            (0 until tagsArr.length()).map { tagsArr.getString(it) }
        else emptyList()

        val categoryStr = if (o.isNull("category")) null else o.optString("category", null)
        val category = categoryStr?.let { runCatching { AppCategory.valueOf(it) }.getOrNull() }

        val targetMinutes = if (o.isNull("targetMinutes")) null else o.optInt("targetMinutes")
        val deadlineMs     = if (o.isNull("deadlineMs")) null else o.optLong("deadlineMs")
        val completedAtMs  = if (o.isNull("completedAtMs")) null else o.optLong("completedAtMs")

        val reminderMode = runCatching {
            GoalReminderMode.valueOf(o.optString("reminderMode", "NONE"))
        }.getOrDefault(GoalReminderMode.NONE)

        val schedule = scheduleFromJson(o.optJSONObject("schedule"))

        return Goal(
            id            = o.optString("id", UUID.randomUUID().toString()),
            title         = o.getString("title"),
            description   = o.optString("description", ""),
            category      = category,
            targetMinutes = targetMinutes,
            checklist     = checklist,
            isMajor       = o.optBoolean("isMajor", false),
            tags          = tags,
            reminderMode  = reminderMode,
            schedule      = schedule,
            deadlineMs    = deadlineMs,
            createdAtMs   = o.optLong("createdAtMs", System.currentTimeMillis()),
            isDone        = o.optBoolean("isDone", false),
            completedAtMs = completedAtMs,
        )
    }
}