package com.example.memogotchi.ui.page

import android.content.Context

data class ActiveTaskTimer(
    val taskId: String,
    val taskTitle: String,
    val targetSeconds: Int,
    val dateKey: String,
)

fun xpForTask(durationMinutes: Int): Int = (10 + durationMinutes * 2).coerceAtLeast(10)

object TaskTimerStore {
    private const val PREFS = "memogotchi_task_timer"
    private const val KEY_TASK_ID = "active_task_id"
    private const val KEY_TASK_TITLE = "active_task_title"
    private const val KEY_TARGET = "active_target_seconds"
    private const val KEY_DATE = "active_date_key"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun start(context: Context, taskId: String, taskTitle: String, durationMinutes: Int, dateKey: String) {
        prefs(context).edit()
            .putString(KEY_TASK_ID, taskId)
            .putString(KEY_TASK_TITLE, taskTitle)
            .putInt(KEY_TARGET, durationMinutes * 60)
            .putString(KEY_DATE, dateKey)
            .apply()
    }
    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun load(context: Context): ActiveTaskTimer? {
        val p = prefs(context)
        val id = p.getString(KEY_TASK_ID, null) ?: return null
        return ActiveTaskTimer(
            taskId = id,
            taskTitle = p.getString(KEY_TASK_TITLE, "") ?: "",
            targetSeconds = p.getInt(KEY_TARGET, 0),
            dateKey = p.getString(KEY_DATE, "") ?: "",
        )
    }

    fun completeActiveTaskAndClear(context: Context): ActiveTaskTimer? {
        val active = load(context) ?: return null
        TaskStore.updateTaskDone(context, active.dateKey, active.taskId, true)
        val cachedTask = TaskStore.loadTasksForDate(context, active.dateKey)?.find { it.id == active.taskId }
        TaskStore.addCompletedTask(
            context,
            CompletedTaskRecord(
                id = active.taskId,
                title = active.taskTitle,
                category = cachedTask?.category ?: AppCategory.OTHER,
                durationMinutes = active.targetSeconds / 60,
                source = TaskStore.getSourceForDate(context, active.dateKey) ?: "rule",
                completedAtMs = System.currentTimeMillis(),
                dateLabel = active.dateKey,
            )
        )
        XpStore.addXp(context, xpForTask(active.targetSeconds / 60))
        clear(context)
        return active
    }
}