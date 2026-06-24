package com.example.memogotchi.focusguard

import android.content.Context
import androidx.datastore.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * FocusGuardStore — DataStore-backed persistence for blocked apps, time
 * limits, and schedules, following the same object-with-load/save
 * convention as WellnessStore (rather than a class you instantiate or a
 * Hilt-injected repository), so it drops into this codebase idiomatically.
 *
 * WHY ONE PROTO FILE FOR THREE FEATURE AREAS
 * -----------------------------------------------
 * FocusGuardConfig holds blocked_apps, time_limits, and schedules together
 * as repeated fields. Proto DataStore persists one whole message per write —
 * there's no way to update just "the blocked apps list" without touching the
 * file holding everything else too. That's an accepted tradeoff for this
 * feature's actual write frequency (rules change rarely — a user sets up a
 * block once and rarely touches it again), NOT something that would hold up
 * for high-frequency writes.
 *
 * WHY USAGE COUNTERS ARE NOT IN THIS PROTO
 * ---------------------------------------------
 * AppTimeLimit in the proto only stores daily_limit_minutes — the user's
 * SETTING, not how many minutes they've used today. That runtime counter
 * (used_today_minutes, last_reset_date from the original Room proposal)
 * changes constantly while a timed app is open, and rewriting the entire
 * FocusGuardConfig blob (including unrelated blocked-app rules and every
 * schedule) on every tick of usage is wasteful disk I/O for data that's
 * fundamentally different in nature from user settings. Usage counters
 * belong in a separate, smaller Preferences DataStore (or even a plain
 * SharedPreferences-style store) keyed by package name + date — NOT built
 * yet, flagged here so it isn't forgotten when AppTimerScreen is built.
 *
 * THE refreshRules() CONTRACT WITH FocusGuardService
 * -------------------------------------------------------
 * FocusGuardService.currentRules is the in-memory source of truth the
 * polling loop reads every tick — it does NOT read this store directly on
 * each tick (see FocusGuardService.kt's class doc for why). Every function
 * below that mutates blocked_apps or schedules MUST call
 * notifyServiceOfRuleChange() afterward, or the running service will keep
 * enforcing stale rules until it's restarted. This is easy to forget when
 * adding new mutation functions later — if you add one, remember the call.
 */

private val Context.focusGuardDataStore by dataStore(
    fileName = "focus_guard_config.pb",
    serializer = FocusGuardConfigSerializer
)

object FocusGuardStore {

    // ── Reads ────────────────────────────────────────────────────────────

    suspend fun loadConfig(context: Context): FocusGuardConfig {
        return context.focusGuardDataStore.data.first()
    }

    /**
     * Used by FocusGuardService.onCreate() for its one-time startup load.
     * Converts the proto's BlockedApp list into the simpler FocusGuardRule
     * shape the polling loop actually consumes — the service doesn't need
     * to know about unblock methods or phrase tone, only "is this package
     * blocked right now and in what mode," so this function deliberately
     * drops everything FocusGuardRule doesn't model.
     */
    suspend fun loadActiveRulesSnapshot(context: Context): List<FocusGuardRule> {
        val config = loadConfig(context)
        return buildActiveRules(config)
    }

    // ── Writes: Blocked Apps ─────────────────────────────────────────────

    suspend fun upsertBlockedApp(context: Context, app: BlockedApp) {
        context.focusGuardDataStore.updateData { current ->
            val withoutExisting = current.blockedAppsList.filterNot {
                it.packageName == app.packageName
            }
            current.toBuilder()
                .clearBlockedApps()
                .addAllBlockedApps(withoutExisting + app)
                .build()
        }
        notifyServiceOfRuleChange(context)
    }

    suspend fun removeBlockedApp(context: Context, packageName: String) {
        context.focusGuardDataStore.updateData { current ->
            current.toBuilder()
                .clearBlockedApps()
                .addAllBlockedApps(current.blockedAppsList.filterNot { it.packageName == packageName })
                .build()
        }
        notifyServiceOfRuleChange(context)
    }

    suspend fun setBlockedAppActive(context: Context, packageName: String, isActive: Boolean) {
        context.focusGuardDataStore.updateData { current ->
            val updated = current.blockedAppsList.map {
                if (it.packageName == packageName) it.toBuilder().setIsActive(isActive).build() else it
            }
            current.toBuilder().clearBlockedApps().addAllBlockedApps(updated).build()
        }
        notifyServiceOfRuleChange(context)
    }

    // ── Writes: Time Limits ──────────────────────────────────────────────

    suspend fun upsertTimeLimit(context: Context, limit: AppTimeLimit) {
        context.focusGuardDataStore.updateData { current ->
            val withoutExisting = current.timeLimitsList.filterNot {
                it.packageName == limit.packageName
            }
            current.toBuilder()
                .clearTimeLimits()
                .addAllTimeLimits(withoutExisting + limit)
                .build()
        }
        // Time limits don't currently feed FocusGuardRule/the polling loop
        // (see class doc — usage counters and limit enforcement are a
        // separate not-yet-built piece), so no notifyServiceOfRuleChange()
        // call here. If AppTimerScreen's enforcement later folds into the
        // same rule system, this will need to call it too.
    }

    suspend fun removeTimeLimit(context: Context, packageName: String) {
        context.focusGuardDataStore.updateData { current ->
            current.toBuilder()
                .clearTimeLimits()
                .addAllTimeLimits(current.timeLimitsList.filterNot { it.packageName == packageName })
                .build()
        }
    }

    // ── Writes: Schedules ────────────────────────────────────────────────

    suspend fun upsertSchedule(context: Context, schedule: ScheduleBlock) {
        context.focusGuardDataStore.updateData { current ->
            val withoutExisting = current.schedulesList.filterNot { it.id == schedule.id }
            current.toBuilder()
                .clearSchedules()
                .addAllSchedules(withoutExisting + schedule)
                .build()
        }
        notifyServiceOfRuleChange(context)
    }

    suspend fun removeSchedule(context: Context, scheduleId: String) {
        context.focusGuardDataStore.updateData { current ->
            current.toBuilder()
                .clearSchedules()
                .addAllSchedules(current.schedulesList.filterNot { it.id == scheduleId })
                .build()
        }
        notifyServiceOfRuleChange(context)
    }

    // ── Internal: rule reconciliation + service notification ────────────

    /**
     * Converts proto BlockedApp + ScheduleBlock entries into the flat
     * FocusGuardRule list FocusGuardService expects. Schedule-based blocks
     * are expanded into one FocusGuardRule per package name in the
     * schedule, each carrying the same ScheduleWindow — this is what lets
     * FocusGuardRule.isCurrentlyActive() do a single, uniform check
     * regardless of whether a rule came from a direct app block or a
     * named schedule group.
     */
    private fun buildActiveRules(config: FocusGuardConfig): List<FocusGuardRule> {
        val directRules = config.blockedAppsList
            .filter { it.isActive }
            .map { app ->
                FocusGuardRule(
                    packageName = app.packageName,
                    appLabel = app.appLabel,
                    blockMode = if (app.isHardBlock) BlockMode.HARD else BlockMode.SOFT,
                    schedule = null
                )
            }

        val scheduleRules = config.schedulesList
            .filter { it.isEnabled }
            .flatMap { schedule ->
                val window = ScheduleWindow(
                    daysOfWeek = schedule.activeDaysList.map { it.toCalendarDay() }.toSet(),
                    startHour = schedule.startHour,
                    startMinute = schedule.startMinute,
                    endHour = schedule.endHour,
                    endMinute = schedule.endMinute
                )
                schedule.packageNamesList.map { packageName ->
                    FocusGuardRule(
                        packageName = packageName,
                        appLabel = packageName, // schedule entries don't cache
                        // a label per package today;
                        // acceptable since the overlay
                        // can resolve it via PackageManager
                        // if this proves visually wrong
                        blockMode = BlockMode.SOFT, // schedules are soft-block only
                        // for now — hard block via
                        // schedule isn't a case the
                        // original spec described
                        schedule = window
                    )
                }
            }

        // Direct app-blocks take priority if the same package somehow
        // appears in both a direct block AND a schedule — keeps a single,
        // unambiguous rule per package rather than letting the polling
        // loop pick arbitrarily between two conflicting entries.
        val directPackages = directRules.map { it.packageName }.toSet()
        return directRules + scheduleRules.filterNot { it.packageName in directPackages }
    }

    /**
     * Pushes a fresh rule snapshot into FocusGuardService's in-memory
     * cache, AND ensures the service is actually running.
     *
     * CRITICAL: this second part was missing in an earlier version of this
     * file. FocusGuardService.start() was written and sat in the
     * companion object, but nothing ever called it — meaning every rule
     * saved via upsertBlockedApp/upsertSchedule/etc. was persisted to disk
     * correctly, but the polling loop that's supposed to detect blocked
     * apps in the foreground was never running at all, for soft OR hard
     * block. The intercept screen could never have appeared, regardless
     * of how correct the rule data itself was. startForegroundService is
     * idempotent in practice here — calling start() when the service is
     * already running just re-delivers onStartCommand, which re-asserts
     * startForeground() (harmless) and calls startPollingIfNeeded(), which
     * itself no-ops if a polling job is already active. Safe to call on
     * every mutation rather than tracking "is it already running" here.
     *
     * PLATFORM CONSTRAINT WORTH KNOWING: Android 12+ restricts starting a
     * foreground service while the app is NOT in the foreground (e.g. from
     * a background task, a notification action handler, or a scheduled
     * job) — that can throw ForegroundServiceStartNotAllowedException.
     * Every current caller of this function (AppBlockerScreen,
     * ScheduleScreen once built) runs while the user is actively in the
     * app's UI, which Android always permits. If a future caller invokes
     * upsertBlockedApp/upsertSchedule/etc. from a background context, this
     * call will need a different mechanism (e.g. WorkManager, or the
     * service already running and listening for a broadcast instead of
     * being freshly started).
     */
    private fun notifyServiceOfRuleChange(context: Context) {
        FocusGuardService.start(context)
        CoroutineScope(Dispatchers.Default).launch {
            val config = loadConfig(context)
            FocusGuardService.refreshRules(buildActiveRules(config))
        }
    }
}

/**
 * Maps the proto DayOfWeek enum to java.util.Calendar's day constants,
 * since ScheduleWindow (in FocusGuardRule.kt) was written against
 * Calendar's day-of-week ints rather than redefining its own enum.
 */
private fun DayOfWeek.toCalendarDay(): Int = when (this) {
    DayOfWeek.MONDAY -> java.util.Calendar.MONDAY
    DayOfWeek.TUESDAY -> java.util.Calendar.TUESDAY
    DayOfWeek.WEDNESDAY -> java.util.Calendar.WEDNESDAY
    DayOfWeek.THURSDAY -> java.util.Calendar.THURSDAY
    DayOfWeek.FRIDAY -> java.util.Calendar.FRIDAY
    DayOfWeek.SATURDAY -> java.util.Calendar.SATURDAY
    DayOfWeek.SUNDAY -> java.util.Calendar.SUNDAY
    DayOfWeek.DAY_OF_WEEK_UNSPECIFIED, DayOfWeek.UNRECOGNIZED -> java.util.Calendar.SUNDAY
}