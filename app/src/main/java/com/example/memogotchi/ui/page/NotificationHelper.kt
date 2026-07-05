package com.example.memogotchi.ui.page

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.memogotchi.R

private const val CHANNEL_ID = "screen_time_alerts"
private const val NOTIF_ID_HEALTH_ALERT = 1001

private const val ACTIVE_TASK_CHANNEL_ID = "active_task_timer"
private const val NOTIF_ID_ACTIVE_TASK = 1002

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Time Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when your screen time is high or exceeds your daily limit"
        }
        val activeTaskChannel = NotificationChannel(
            ACTIVE_TASK_CHANNEL_ID,
            "Active Task Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Pinned notification shown while an analog task timer is running"
            setSound(null, null)
            enableVibration(false)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(activeTaskChannel)
    }
}

/** Whether the app is currently allowed to post notifications on this device/version. */
fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/**
 * Shows (or updates) a pinned, non-dismissable notification for the currently running task
 * timer. Safe to call repeatedly (e.g. once per tick) — setOnlyAlertOnce avoids re-alerting.
 */
@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun showOrUpdateActiveTaskNotification(
    context: Context,
    taskTitle: String,
    targetSeconds: Int,
    elapsedSeconds: Long,
) {
    if (!canPostNotifications(context)) return

    val remaining = (targetSeconds - elapsedSeconds).coerceAtLeast(0L)
    val minutes = remaining / 60
    val seconds = remaining % 60
    val timeLabel = String.format("%02d:%02d left", minutes, seconds)

    val progress = if (targetSeconds > 0) {
        ((elapsedSeconds.coerceIn(0L, targetSeconds.toLong()) * 100) / targetSeconds).toInt()
    } else 0

    val builder = NotificationCompat.Builder(context, ACTIVE_TASK_CHANNEL_ID)
        .setSmallIcon(R.drawable.memogotchi_vector)
        .setContentTitle(taskTitle)
        .setContentText(timeLabel)
        .setProgress(100, progress, false)
        .setOngoing(true)
        .setAutoCancel(false)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)

    try {
        NotificationManagerCompat.from(context).notify(NOTIF_ID_ACTIVE_TASK, builder.build())
    } catch (e: SecurityException) {
        // Permission revoked between check and call — fail silently, no crash
    }
}

/** Removes the pinned active-task notification, e.g. on cancel or completion. */
fun cancelActiveTaskNotification(context: Context) {
    NotificationManagerCompat.from(context).cancel(NOTIF_ID_ACTIVE_TASK)
}

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun maybeSendHealthAlert(context: Context, totalMs: Long, dailyLimitMin: Int) {
    if (!AppSettings.healthAlertsEnabled) return

    val limitMs = dailyLimitMin * 60 * 1000L
    if (limitMs <= 0L) return

    val (title, message) = when {
        totalMs >= limitMs -> "Daily limit reached" to "You've hit your ${formatMs(limitMs)} screen time limit today. Maybe take a break?"
        totalMs >= (limitMs * 0.8).toLong() -> "Almost at your limit" to "${formatMs(limitMs - totalMs)} left before you hit your daily limit."
        else -> return
    }
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.memogotchi_vector)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

    val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    if (canNotify) {
        NotificationManagerCompat.from(context).notify(NOTIF_ID_HEALTH_ALERT, builder.build())
    }
}