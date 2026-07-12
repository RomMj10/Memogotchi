package com.example.memogotchi.usage

import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.memogotchi.api.ApiClient
import com.example.memogotchi.api.UpdateInsightRequest
import com.example.memogotchi.ui.page.hasUsageStatsPermission
import com.example.memogotchi.ui.page.loadWeekData
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Periodically reports the current user's app-usage minutes for every ACTIVE
 * buddy_event they're a consenting participant in, via POST /events/insight.
 *
 * Reuses the existing UsageStatsManager-reading logic from ScreenTimeScreen.kt
 * (hasUsageStatsPermission() / loadWeekData()) rather than re-implementing it.
 *
 * Consent is checked locally (the Firestore query below only returns rows
 * where consentedToShare == true) AND re-checked server-side — the backend
 * silently no-ops (written: false) if consent isn't currently active there,
 * so a momentarily-stale local read is safe to send.
 */
class UsageReportWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        // UsageStatsManager-based permission check (matches ScreenTimeScreen.kt) is
        // only meaningful on API 29+; on older devices there's nothing safe to report.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return Result.success()
        if (!applicationContext.hasUsageStatsPermission()) return Result.success()

        return try {
            val db = Firebase.firestore

            val consentedParticipantDocs = db.collection("buddy_event_participants")
                .whereEqualTo("userId", uid)
                .whereEqualTo("consentedToShare", true)
                .get()
                .await()

            for (participantDoc in consentedParticipantDocs.documents) {
                val eventId = participantDoc.getString("eventId") ?: continue

                // Each event is handled independently — one failing event (e.g. a
                // transient network error, or a since-ended event) shouldn't stop
                // the rest of the batch from reporting.
                try {
                    val eventDoc = db.collection("buddy_events").document(eventId).get().await()
                    if (eventDoc.getString("status") != "active") continue

                    val goalConfig = eventDoc.get("goalConfig") as? Map<*, *> ?: continue
                    val targetApp = goalConfig["targetApp"] as? String ?: continue
                    val duration = goalConfig["duration"] as? String ?: "today"

                    val minutesUsed = computeMinutesUsed(applicationContext, targetApp, duration)

                    ApiClient.service.updateInsightShare(
                        UpdateInsightRequest(
                            eventId = eventId,
                            metric = "minutes_used",
                            value = minutesUsed
                        )
                    )
                } catch (perEventError: Exception) {
                    continue
                }
            }

            Result.success()
        } catch (e: Exception) {
            // Broader failure (e.g. couldn't even read participant docs) — worth a retry.
            Result.retry()
        }
    }

    /**
     * Minutes spent in [targetApp] for "today" (the last entry loadWeekData returns)
     * or summed across all 7 entries for "this_week". Timezone handling matches
     * loadWeekData's existing local-midnight day boundaries — see the master
     * alignment checklist item on timezone consistency for "today" goals.
     */
    private suspend fun computeMinutesUsed(context: Context, targetApp: String, duration: String): Double {
        val weekData = loadWeekData(context)
        val millisUsed = when (duration) {
            "this_week" -> weekData.sumOf { day ->
                day.apps.firstOrNull { it.packageName == targetApp }?.totalTimeMs ?: 0L
            }
            else -> weekData.lastOrNull()
                ?.apps?.firstOrNull { it.packageName == targetApp }?.totalTimeMs ?: 0L
        }
        return millisUsed / 60_000.0
    }
}

/**
 * Schedules/cancels the periodic usage-report job. Call schedule() once the user
 * is signed in (e.g. alongside the FCM token save in Step 1's sign-in flow) and
 * cancel() on sign-out. Safe to call schedule() repeatedly — KEEP policy means
 * an already-scheduled job is left alone rather than restarted.
 */
object UsageReportScheduler {
    private const val WORK_NAME = "usage_report_worker"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<UsageReportWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}