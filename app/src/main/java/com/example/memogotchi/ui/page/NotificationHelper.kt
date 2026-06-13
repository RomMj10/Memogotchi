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

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Time Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts when your screen time is high or exceeds your daily limit"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
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
        .setSmallIcon(R.drawable.ic_nav_pet)
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
