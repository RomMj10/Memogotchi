package com.example.memogotchi.ui.page

import android.content.Context

enum class TimerMode {STOPWATCH, POMODORO}

object PomodoroStore {
    private const val PREFS = "memogotchi_pomodoro"
    private const val KEY_START = "start_timestamp"
    private const val KEY_ACCUMULATED = "accumulated_seconds"
    private const val KEY_RUNNING = "is_running"
    private const val KEY_MODE = "timer_mode"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadElapsedSeconds(context: Context): Long {
        val p = prefs(context)
        val accumulated = p.getLong(KEY_ACCUMULATED, 0L)
        val running = p.getBoolean(KEY_RUNNING, true)
        return if (running) {
            val start = p.getLong(KEY_START, System.currentTimeMillis())
            accumulated + (System.currentTimeMillis() - start) / 1000
        } else accumulated
    }

    fun isRunning(context: Context): Boolean = prefs(context).getBoolean(KEY_RUNNING, true)

    fun loadMode(context: Context): TimerMode =
        runCatching { TimerMode.valueOf(prefs(context).getString(KEY_MODE, TimerMode.STOPWATCH.name) !!)
        }.getOrDefault(TimerMode.STOPWATCH)

    fun setMode(context: Context, mode: TimerMode) {
        prefs(context).edit()
            .putString(KEY_MODE, mode.name)
            .putLong(KEY_ACCUMULATED, 0L)
            .putLong(KEY_START, System.currentTimeMillis())
            .putBoolean(KEY_RUNNING, false)
            .apply()
    }

    fun start(context: Context, currentElapsed: Long) {
        prefs(context).edit()
            .putLong(KEY_ACCUMULATED, currentElapsed)
            .putLong(KEY_START, System.currentTimeMillis())
            .putBoolean(KEY_RUNNING, true)
            .apply()
    }

    fun pause(context: Context, currentElapsed: Long) {
        prefs(context).edit()
            .putLong(KEY_ACCUMULATED, currentElapsed)
            .putBoolean(KEY_RUNNING, false)
            .apply()
    }

    fun reset(context: Context) {
        prefs(context).edit()
            .putLong(KEY_ACCUMULATED, 0L)
            .putLong(KEY_START, System.currentTimeMillis())
            .putBoolean(KEY_RUNNING, false)
            .apply()
    }
}