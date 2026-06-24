package com.example.memogotchi.focusguard

/**
 * Minimal shape FocusGuardService needs to compile and reason about rules.
 * This is NOT the final data layer — FocusGuardStore (proto + DataStore,
 * discussed earlier but not yet built) will produce real instances of this
 * class from disk. This file exists now so FocusGuardService has something
 * concrete to compile against instead of referencing types that don't exist
 * anywhere.
 *
 * WHAT'S MISSING, ON PURPOSE, FOR NOW:
 *   - FocusGuardStore.loadActiveRulesSnapshot() is referenced by the service
 *     but not implemented anywhere yet. Calling it will not compile until
 *     that object exists. This is the next file to build.
 *   - isCurrentlyActive() below has a real implementation for schedule-based
 *     rules (checks day/time window) but block-mode-only rules (just "block
 *     this app always") should always return true — that branch is included.
 *   - Per-app unblock method (phrase/delay/math), time limits, and schedule
 *     grouping (the "Early morning" style named group) are NOT represented
 *     in this minimal class yet. FocusGuardService only needs to know
 *     "is this package blocked right now and in what mode" to do its job —
 *     the richer unblock-method data belongs to BlockInterceptOverlay's
 *     screen logic, not the polling service, so it's deliberately left out
 *     here to keep this service's dependency surface small.
 */
data class FocusGuardRule(
    val packageName: String,
    val appLabel: String,
    val blockMode: BlockMode,
    val schedule: ScheduleWindow? = null
) {
    fun isCurrentlyActive(): Boolean {
        val window = schedule ?: return true // always-on block, no schedule restriction
        return window.isActiveNow()
    }
}

enum class BlockMode {
    SOFT,
    HARD
}

/**
 * Placeholder schedule window — day-of-week + start/end time check.
 * Real implementation pending FocusGuardStore build-out; this is enough
 * for FocusGuardService to call isCurrentlyActive() without crashing.
 */
data class ScheduleWindow(
    val daysOfWeek: Set<Int>, // Calendar.MONDAY..SUNDAY values
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
) {
    fun isActiveNow(): Boolean {
        val now = java.util.Calendar.getInstance()
        val today = now.get(java.util.Calendar.DAY_OF_WEEK)
        if (today !in daysOfWeek) return false

        val nowMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes <= endMinutes) {
            nowMinutes in startMinutes..endMinutes
        } else {
            // Window spans midnight (e.g. 10pm–6am) — handle wraparound.
            nowMinutes >= startMinutes || nowMinutes <= endMinutes
        }
    }
}