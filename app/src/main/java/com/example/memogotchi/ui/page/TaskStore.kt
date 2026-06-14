package com.example.memogotchi.ui.page

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CompletedTaskRecord(
    val id: String,
    val title: String,
    val category: AppCategory,
    val durationMinutes: Int,
    val source: String,
    val completedAtMs: Long,
    val dateLabel: String,
)

object TaskStore {
    private const val PREFS = "memogotchi_tasks"
    private const val KEY_TASKS = "tasks_"
    private const val KEY_SOURCE = "tasks_source_"
    private const val KEY_HISTORY = "completed_history"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveTasksForDate(context: Context, dateKey: String, tasks: List<AnalogTask>, source: String) {
        val arr = JSONArray()
        tasks.forEach { arr.put(taskToJson(it)) }
        prefs(context).edit()
            .putString(KEY_TASKS + dateKey, arr.toString())
            .putString(KEY_SOURCE + dateKey, source)
            .apply()
    }

    fun loadTasksForDate(context: Context, dateKey: String): List<AnalogTask>? {
        val str = prefs(context).getString(KEY_TASKS + dateKey, null) ?: return null
        return try {
            val arr = JSONArray(str)
            (0 until arr.length()).map { taskFromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) { null }
    }

    fun getSourceForDate(context: Context, dateKey: String): String? =
        prefs(context).getString(KEY_SOURCE + dateKey, null)

    fun updateTaskDone(context: Context, dateKey: String, taskId: String, isDone: Boolean) {
        val tasks = loadTasksForDate(context, dateKey)?.toMutableList() ?: return
        val idx = tasks.indexOfFirst { it.id == taskId }
        if (idx >= 0) {
            tasks[idx] = tasks[idx].copy(isDone = isDone)
            saveTasksForDate(context, dateKey, tasks, getSourceForDate(context, dateKey) ?: "rule")
        }
    }

    fun addCompletedTask(context: Context, record: CompletedTaskRecord) {
        val history = (getCompletedHistory(context) + record).takeLast(100)
        val arr = JSONArray()
        history.forEach { arr.put(completedToJson(it)) }
        prefs(context).edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun removeCompletedTask(context: Context, taskId: String, dateLabel: String) {
        val history = getCompletedHistory(context).filterNot { it.id == taskId && it.dateLabel == dateLabel }
        val arr = JSONArray()
        history.forEach { arr.put(completedToJson(it)) }
        prefs(context).edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun getCompletedHistory(context: Context): List<CompletedTaskRecord> {
        val str = prefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(str)
            (0 until arr.length()).map { completedFromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }

    private fun taskToJson(t: AnalogTask) = JSONObject().apply {
        put("id", t.id); put("title", t.title); put("description", t.description)
        put("category", t.category.name); put("durationMinutes", t.durationMinutes)
        put("triggerReason", t.triggerReason); put("isDone", t.isDone)
    }

    private fun taskFromJson(o: JSONObject) = AnalogTask(
        id = o.getString("id"),
        title = o.getString("title"),
        description = o.getString("description"),
        category = runCatching { AppCategory.valueOf(o.getString("category")) }.getOrDefault(AppCategory.OTHER),
        durationMinutes = o.optInt("durationMinutes", 15),
        triggerReason = o.getString("triggerReason"),
        isDone = o.optBoolean("isDone", false),
    )

    private fun completedToJson(r: CompletedTaskRecord) = JSONObject().apply {
        put("id", r.id); put("title", r.title); put("category", r.category.name)
        put("durationMinutes", r.durationMinutes); put("source", r.source)
        put("completedAtMs", r.completedAtMs); put("dateLabel", r.dateLabel)
    }

    private fun completedFromJson(o: JSONObject) = CompletedTaskRecord(
        id = o.getString("id"),
        title = o.getString("title"),
        category = runCatching { AppCategory.valueOf(o.getString("category")) }.getOrDefault(AppCategory.OTHER),
        durationMinutes = o.optInt("durationMinutes", 15),
        source = o.getString("source"),
        completedAtMs = o.getLong("completedAtMs"),
        dateLabel = o.getString("dateLabel"),
    )
}