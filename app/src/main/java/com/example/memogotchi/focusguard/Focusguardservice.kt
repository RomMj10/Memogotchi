package com.example.memogotchi.focusguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.memogotchi.MainActivity
import com.example.memogotchi.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * FocusGuardService — the monitoring backbone for soft-block mode.
 *
 * WHAT THIS DOES
 * ----------------
 * Runs as a foreground service with a polling loop that asks
 * UsageStatsManager every ~500ms "what app moved to foreground since I last
 * checked?" via queryEvents(). When the foreground app matches a currently
 * active block rule, it launches BlockInterceptOverlay on top of it.
 *
 * WHAT THIS DOES NOT DO
 * ------------------------
 * - Hard block enforcement. That's FocusGuardAccessibilityService's job —
 *   this service still detects the foreground app and still launches the
 *   overlay for hard-blocked apps too (so the friction screen still shows),
 *   but it does NOT attempt to forcibly close/hide the app itself. Only the
 *   AccessibilityService can do that (via performGlobalAction or window
 *   manipulation), and only when the user has granted accessibility access.
 * - Reading rules from disk on every tick. Rules are held in memory
 *   (currentRules) and only refreshed when FocusGuardStore reports a
 *   change, via the rules StateFlow it exposes. See refreshRules().
 *
 * COORDINATION WITH FocusGuardAccessibilityService
 * ----------------------------------------------------
 * This service is the single source of truth for "what are the current
 * rules." FocusGuardAccessibilityService does NOT maintain its own copy —
 * it reads currentRules via the companion object's exposed StateFlow rather
 * than querying FocusGuardStore independently. This avoids the two services
 * disagreeing about state or double-triggering the overlay for the same
 * app switch.
 */
class FocusGuardService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob)
    private var pollingJob: Job? = null
    private var initialRuleLoadJob: Job? = null

    private lateinit var usageStatsManager: UsageStatsManager

    // Tracks the timestamp of the last event we've already processed, so
    // each queryEvents() call only walks events newer than what we've seen —
    // this is what avoids re-reading the entire day's event history on every
    // single tick.
    private var lastEventTimestamp: Long = System.currentTimeMillis()

    // The package name currently shown by BlockInterceptOverlay, if any.
    // Prevents re-launching the overlay repeatedly while the user is already
    // looking at it for the same app (queryEvents can report the same
    // foreground app across multiple ticks while the overlay Activity itself
    // is what's actually on screen).
    private var currentlyInterceptedPackage: String? = null

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        createForegroundNotificationChannel()
        runningInstance = this

        // Pick up whatever rules already exist on disk once, at startup.
        // Captured as a Job so startPollingIfNeeded() can join() it before
        // the polling loop's first tick — otherwise there's a race where
        // polling could start checking the foreground app against an
        // empty currentRules list before this load finishes. After this
        // initial load, we rely on refreshRules() being called by
        // FocusGuardStore's change notifications — see companion object.
        initialRuleLoadJob = serviceScope.launch {
            currentRules = FocusGuardStore.loadActiveRulesSnapshot(applicationContext)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        startPollingIfNeeded()
        // START_STICKY: if the system kills this service under memory
        // pressure, restart it without redelivering the last intent — block
        // rules should resume monitoring automatically, not wait for the
        // user to reopen the app.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        pollingJob?.cancel()
        serviceJob.cancel()
        if (runningInstance === this) {
            runningInstance = null
        }
        super.onDestroy()
    }

    private fun startPollingIfNeeded() {
        if (pollingJob?.isActive == true) return
        pollingJob = serviceScope.launch {
            // Wait for the one-time startup rule load to finish before the
            // first tick. join() is a no-op if it already completed by the
            // time onStartCommand runs — this only adds latency on the
            // narrow window where onCreate's disk read is still in flight,
            // not on every subsequent call to startPollingIfNeeded().
            initialRuleLoadJob?.join()
            while (isActive) {
                checkForegroundApp()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * Single tick: read new usage events since lastEventTimestamp, find the
     * most recent MOVE_TO_FOREGROUND, and decide whether to intercept.
     */
    private fun checkForegroundApp() {
        val now = System.currentTimeMillis()
        val events: UsageEvents = usageStatsManager.queryEvents(lastEventTimestamp, now)

        var latestForegroundPackage: String? = null
        var latestForegroundTimestamp: Long = lastEventTimestamp

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                // Walk all events in this window; the LAST foreground event
                // is the one that matters, since the user may have switched
                // through several apps within one 500ms tick.
                if (event.timeStamp >= latestForegroundTimestamp) {
                    latestForegroundPackage = event.packageName
                    latestForegroundTimestamp = event.timeStamp
                }
            }
        }

        lastEventTimestamp = now

        // Skip our own package — never intercept Memogotchi itself. Without
        // this, opening the app's own UI (e.g. from the foreground
        // notification, or just switching back to it) gets treated like any
        // other foreground-app event and can trigger the overlay on top of
        // our own Activity if a stale/misconfigured rule ever matched it.
        if (latestForegroundPackage == applicationContext.packageName) return

        if (latestForegroundPackage == null) {
            // No app switch happened this tick — nothing to do. We
            // deliberately do NOT clear currentlyInterceptedPackage here,
            // since the overlay Activity itself is now the foreground
            // "app" from the system's point of view in some cases, and we
            // don't want to spuriously re-trigger.
            return
        }

        // If the foreground app changed to something that ISN'T the
        // previously intercepted package, clear the intercept lock — the
        // user genuinely navigated away (e.g. pressed back from the
        // overlay, or switched to a different app entirely).
        if (latestForegroundPackage != currentlyInterceptedPackage) {
            currentlyInterceptedPackage = null
        }

        val rule = currentRules.firstOrNull { it.packageName == latestForegroundPackage }
            ?: return // not a blocked app, nothing to do

        if (!rule.isCurrentlyActive()) {
            // Rule exists but isn't in effect right now (e.g. a schedule
            // block outside its configured time window) — let it through.
            return
        }

        if (latestForegroundPackage == currentlyInterceptedPackage) {
            // Already showing the overlay for this exact app — don't
            // re-launch it on top of itself.
            return
        }

        if (FocusGuardService.isUnblockGraceActive(latestForegroundPackage)) return

        launchInterceptOverlay(latestForegroundPackage, rule)
    }

    private fun launchInterceptOverlay(packageName: String, rule: FocusGuardRule) {
        currentlyInterceptedPackage = packageName
        val intent = Intent(applicationContext, BlockInterceptOverlay::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(BlockInterceptOverlay.EXTRA_PACKAGE_NAME, packageName)
            putExtra(BlockInterceptOverlay.EXTRA_APP_LABEL, rule.appLabel)
            putExtra(BlockInterceptOverlay.EXTRA_BLOCK_MODE, rule.blockMode.name)
        }
        applicationContext.startActivity(intent)
    }

    private fun createForegroundNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Guard monitoring",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps app blocking and time limits active in the background."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Guard is active")
            .setContentText("Watching for blocked apps and schedules.")
            .setSmallIcon(R.drawable.ic_nav_focusguard)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "focus_guard_monitoring"
        private const val NOTIFICATION_ID = 4271
        private const val POLL_INTERVAL_MS = 500L
        private val unblockedPackages = mutableMapOf<String, Long>()
        private const val UNBLOCK_GRACE_MS = 10_000L // 10 seconds

        // Held at the companion-object level (not inside the Service
        // instance) so FocusGuardAccessibilityService — a completely
        // separate Service class — can read the exact same in-memory rules
        // without going back to disk or duplicating FocusGuardStore calls.
        // This is the "single source of truth" referenced in the class doc
        // comment above.
        @Volatile
        var currentRules: List<FocusGuardRule> = emptyList()
            private set

        /**
         * Called by FocusGuardStore whenever rules are added, edited,
         * removed, or a schedule's active window starts/ends. Keeps
         * currentRules fresh without the polling loop ever touching disk.
         *
         * FocusGuardStore is responsible for calling this — see its
         * onRulesChanged hook, wired up wherever rules are persisted.
         */
        fun refreshRules(updatedRules: List<FocusGuardRule>) {
            currentRules = updatedRules
        }

        fun start(context: Context) {
            val intent = Intent(context, FocusGuardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FocusGuardService::class.java))
        }

        fun grantUnblockGrace(packageName: String) {
            unblockedPackages[packageName] = System.currentTimeMillis() + UNBLOCK_GRACE_MS
        }

        fun isUnblockGraceActive(packageName: String): Boolean {
            val expiry = unblockedPackages[packageName] ?: return false
            if (System.currentTimeMillis() > expiry) {
                unblockedPackages.remove(packageName)
                return false
            }
            return true
        }

        // The currently running instance, if the service is alive. Needed
        // so BlockInterceptOverlay (a separate Activity) can tell the
        // running service "I'm finishing" and have that update the actual
        // instance field that checkForegroundApp() reads on the next tick.
        // Nulled out in onDestroy() so a dead instance is never referenced.
        @Volatile
        private var runningInstance: FocusGuardService? = null

        /**
         * Called by BlockInterceptOverlay.onDestroy() regardless of how the
         * overlay closed (Nevermind, successful unblock, back gesture,
         * swipe-away from recents, process death). Without this, the lock
         * is only cleared on the *next* foreground-app-changed tick — which
         * normally happens automatically once the launcher or another app
         * takes focus, but NOT if the user backs out of the overlay back
         * into the still-resumed blocked app underneath it. In that case
         * latestForegroundPackage == currentlyInterceptedPackage and the
         * service skips re-evaluating, leaving the blocked app visible and
         * usable. Calling this eagerly closes that gap.
         */
        fun clearInterceptedPackage(packageName: String) {
            runningInstance?.let { instance ->
                if (instance.currentlyInterceptedPackage == packageName) {
                    instance.currentlyInterceptedPackage = null
                }
            }
        }
    }
}