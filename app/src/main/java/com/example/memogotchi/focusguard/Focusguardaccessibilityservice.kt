package com.example.memogotchi.focusguard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * FocusGuardAccessibilityService — hard block enforcement.
 *
 * WHAT THIS DOES
 * ----------------
 * Receives a system callback every time the foreground window changes
 * (configured via focus_guard_accessibility_config.xml's
 * accessibilityEventTypes="typeWindowStateChanged" — the system pushes
 * these events, this service doesn't poll). When the new foreground
 * package matches an active HARD-mode rule, it:
 *   1. Calls performGlobalAction(GLOBAL_ACTION_HOME) immediately, so the
 *      blocked app is never left running in the foreground even for a
 *      moment.
 *   2. Launches BlockInterceptOverlay on top of the now-home screen, so
 *      the user still sees "Do you really want to use {appname}?" rather
 *      than being silently dumped at home with no explanation.
 *
 * WHY HOME, NOT BACK
 * ---------------------
 * GLOBAL_ACTION_BACK's destination depends on the blocked app's own
 * navigation stack — if the app has no back stack (e.g. just cold-launched),
 * back can exit to whatever was foreground before it, which might itself be
 * another blocked app, causing a flicker loop between two block screens.
 * GLOBAL_ACTION_HOME always lands somewhere neutral regardless of the
 * blocked app's internal state.
 *
 * SINGLE SOURCE OF TRUTH — NO INDEPENDENT RULE STORAGE
 * ---------------------------------------------------------
 * This service does NOT call FocusGuardStore or maintain its own rule
 * cache. It reads FocusGuardService.currentRules directly — the exact same
 * in-memory list the polling service uses for soft block. This is
 * deliberate (see FocusGuardService's class doc): two services independently
 * deciding "is this app blocked" risks them disagreeing, double-triggering,
 * or one acting on stale data the other already updated. There is ONE rule
 * cache, owned by FocusGuardService's companion object; this service is a
 * reader of it, not a second writer.
 *
 * SOFT-BLOCK APPS ARE DELIBERATELY IGNORED HERE
 * ---------------------------------------------------
 * This service only acts on rules where blockMode == BlockMode.HARD.
 * Soft-blocked apps are left alone here entirely — FocusGuardService's
 * polling loop already handles them by launching BlockInterceptOverlay on
 * its own, and having both services react to the same soft-blocked app
 * would cause a double-launch race. Hard and soft are intentionally
 * non-overlapping responsibilities between these two services.
 *
 * FALLBACK BEHAVIOR WHEN THIS SERVICE ISN'T RUNNING
 * -------------------------------------------------------
 * If the user has a HARD-mode rule but hasn't granted Accessibility access,
 * this service simply never runs, so this enforcement never fires for that
 * app. FocusGuardService's polling loop still launches BlockInterceptOverlay
 * for it (since FocusGuardRule.isCurrentlyActive() doesn't care about
 * accessibility status) — meaning a "hard" rule silently behaves as soft
 * until accessibility is granted. This is a known, accepted gap (see
 * conversation notes) — the friction screen still appears, but the app
 * isn't forcibly closed. AppBlockerScreen should surface this to the user
 * near the hard-block toggle when that screen is built, so the silence is
 * scoped to enforcement only, not the whole UX.
 */
class FocusGuardAccessibilityService : AccessibilityService() {

    private var lastHandledPackage: String? = null
    private var lastHandledTimestamp: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Skip our own app entirely — without this, opening MainActivity
        // itself (e.g. the FocusGuard screens) could theoretically match a
        // rule if the user ever blocked their own package by mistake, and
        // more importantly, BlockInterceptOverlay's own window state change
        // would otherwise be inspected as if it were a foreground app
        // switch worth evaluating.
        if (packageName == applicationContext.packageName) return

        // Debounce: typeWindowStateChanged can fire multiple times in quick
        // succession for the same package during a single app launch
        // (e.g. a splash screen transitioning to the main screen still
        // counts as the same package). Without this, performGlobalAction
        // could fire repeatedly for what's really one single app-open event.
        val now = System.currentTimeMillis()
        if (packageName == lastHandledPackage && now - lastHandledTimestamp < DEBOUNCE_MS) {
            return
        }

        val rule = FocusGuardService.currentRules.firstOrNull { it.packageName == packageName }
            ?: return

        if (rule.blockMode != BlockMode.HARD) return
        if (!rule.isCurrentlyActive()) return

        lastHandledPackage = packageName
        lastHandledTimestamp = now

        if (FocusGuardService.isUnblockGraceActive(packageName)) return
        performGlobalAction(GLOBAL_ACTION_HOME)
        launchInterceptOverlay(packageName, rule)
    }

    private fun launchInterceptOverlay(packageName: String, rule: FocusGuardRule) {
        val intent = Intent(applicationContext, BlockInterceptOverlay::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(BlockInterceptOverlay.EXTRA_PACKAGE_NAME, packageName)
            putExtra(BlockInterceptOverlay.EXTRA_APP_LABEL, rule.appLabel)
            putExtra(BlockInterceptOverlay.EXTRA_BLOCK_MODE, rule.blockMode.name)
        }
        applicationContext.startActivity(intent)
    }

    override fun onInterrupt() {
        // Required override, no-op — called by the system if the service
        // is interrupted (e.g. another accessibility service takes over).
        // Nothing to clean up: this service holds no resources beyond the
        // two small fields above.
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // No additional AccessibilityServiceInfo configuration needed here —
        // everything (event types, feedback type, flags) is already set in
        // focus_guard_accessibility_config.xml via the manifest's meta-data
        // reference. Configuring it again programmatically here would be
        // redundant and a second place to keep in sync.
    }

    companion object {
        private const val DEBOUNCE_MS = 1500L
    }
}