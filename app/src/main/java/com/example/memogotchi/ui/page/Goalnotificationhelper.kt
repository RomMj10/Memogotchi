package com.example.memogotchi.ui.page

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.memogotchi.R
import java.util.Calendar

// ════════════════════════════════════════════════════════════════════════════
//  CHANNELS
// ════════════════════════════════════════════════════════════════════════════

private const val CHANNEL_GOALS_PINNED = "goal_pinned_alerts"
private const val CHANNEL_GOALS_DAILY  = "goal_daily_alerts"

private const val NOTIF_ID_MINOR_PINNED = 2001
private const val NOTIF_ID_MINOR_DAILY  = 2002

private fun majorPinnedNotifId(goalId: String) = 3_000_000 + (goalId.hashCode() and 0xFFFF)
private fun majorDailyNotifId(goalId: String)  = 4_000_000 + (goalId.hashCode() and 0xFFFF)
private fun majorAlarmRequestCode(goalId: String, dayOffset: Int = 0) =
    5_000_000 + (goalId.hashCode() and 0xFFFF) * 10 + dayOffset

fun createGoalNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GOALS_PINNED,
                "Pinned Goal Reminders",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Always-visible reminders for goals you've pinned"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GOALS_DAILY,
                "Scheduled Goal Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for goals at the time and days you've scheduled"
            }
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  PERMISSION HELPER
// ════════════════════════════════════════════════════════════════════════════

private fun canNotify(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

// ════════════════════════════════════════════════════════════════════════════
//  PINNED (FIXED) NOTIFICATIONS
//  Major goals: one persistent notification each.
//  Minor goals: all combined into a single persistent notification.
// ════════════════════════════════════════════════════════════════════════════

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun refreshPinnedGoalNotifications(context: Context, goals: List<Goal>) {
    if (!canNotify(context)) return

    val pinnedActive = goals.filter { it.reminderMode == GoalReminderMode.PINNED && !it.isEffectivelyDone() }

    val pinnedMajors = pinnedActive.filter { it.isMajor }
    pinnedMajors.forEach { goal ->
        val (done, total) = goal.checklistProgress()
        val contentText = if (goal.hasChecklist) {
            "$done / $total steps done · tap to open the app"
        } else {
            goal.description.ifBlank { "Tap to open the app" }
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_GOALS_PINNED)
            .setSmallIcon(R.drawable.ic_nav_pet)
            .setContentTitle("🚩 ${goal.title}")
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        NotificationManagerCompat.from(context).notify(majorPinnedNotifId(goal.id), builder.build())
    }

    val allMajorIds = goals.filter { it.isMajor }.map { it.id }.toSet()
    val stillPinnedMajorIds = pinnedMajors.map { it.id }.toSet()
    (allMajorIds - stillPinnedMajorIds).forEach { goalId ->
        NotificationManagerCompat.from(context).cancel(majorPinnedNotifId(goalId))
    }

    val pinnedMinors = pinnedActive.filter { !it.isMajor }
    if (pinnedMinors.isNotEmpty()) {
        val inboxStyle = NotificationCompat.InboxStyle()
        pinnedMinors.forEach { goal ->
            val subtitle = if (goal.hasChecklist) {
                val (done, total) = goal.checklistProgress()
                "$done/$total steps"
            } else {
                goal.description.ifBlank { "" }
            }
            val line = if (subtitle.isNotBlank()) "${goal.title} — $subtitle" else goal.title
            inboxStyle.addLine(line)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_GOALS_PINNED)
            .setSmallIcon(R.drawable.ic_nav_pet)
            .setContentTitle("Today's goals (${pinnedMinors.size})")
            .setContentText(pinnedMinors.joinToString(", ") { it.title })
            .setStyle(inboxStyle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        NotificationManagerCompat.from(context).notify(NOTIF_ID_MINOR_PINNED, builder.build())
    } else {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID_MINOR_PINNED)
    }
}

fun cancelPinnedGoalNotification(context: Context, goal: Goal) {
    val id = if (goal.isMajor) majorPinnedNotifId(goal.id) else NOTIF_ID_MINOR_PINNED
    NotificationManagerCompat.from(context).cancel(id)
}

// ════════════════════════════════════════════════════════════════════════════
//  SCHEDULED NOTIFICATIONS (time + days, default midnight/every day)
// ════════════════════════════════════════════════════════════════════════════

class GoalDailyReminderReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val goalId = intent.getStringExtra("goal_id")
        val isMajor = intent.getBooleanExtra("is_major", false)
        val title = intent.getStringExtra("title") ?: "Goal reminder"
        val text = intent.getStringExtra("text") ?: "Don't forget about this goal today."

        if (!canNotify(context)) return

        val builder = NotificationCompat.Builder(context, CHANNEL_GOALS_DAILY)
            .setSmallIcon(R.drawable.ic_nav_pet)
            .setContentTitle(if (isMajor) "🚩 $title" else title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notifId = if (isMajor && goalId != null) majorDailyNotifId(goalId) else NOTIF_ID_MINOR_DAILY
        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }
}

/**
 * Schedules a single major goal's reminder according to its GoalSchedule.
 * If schedule.isEveryDay, sets one repeating daily alarm.
 * Otherwise, sets one repeating-weekly alarm per selected day.
 */
fun scheduleGoalReminder(context: Context, goal: Goal) {
    cancelGoalReminder(context, goal) // clear any previous alarms for this goal first

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val schedule = goal.schedule

    val baseIntent = Intent(context, GoalDailyReminderReceiver::class.java).apply {
        putExtra("goal_id", goal.id)
        putExtra("is_major", true)
        putExtra("title", goal.title)
        putExtra("text", goal.description.ifBlank { "Check in on this goal." })
    }

    if (schedule.isEveryDay) {
        val pendingIntent = PendingIntent.getBroadcast(
            context, majorAlarmRequestCode(goal.id, 0), baseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = nextTriggerTimeMillis(schedule.hour, schedule.minute)
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, AlarmManager.INTERVAL_DAY, pendingIntent)
    } else {
        schedule.daysOfWeek.forEach { dayOfWeek ->
            val pendingIntent = PendingIntent.getBroadcast(
                context, majorAlarmRequestCode(goal.id, dayOfWeek), baseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerTime = nextTriggerTimeMillisForDay(schedule.hour, schedule.minute, dayOfWeek)
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP, triggerTime,
                AlarmManager.INTERVAL_DAY * 7, pendingIntent
            )
        }
    }
}

fun cancelGoalReminder(context: Context, goal: Goal) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    // Cancel the "every day" slot
    val everyDayIntent = Intent(context, GoalDailyReminderReceiver::class.java)
    val everyDayPending = PendingIntent.getBroadcast(
        context, majorAlarmRequestCode(goal.id, 0), everyDayIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(everyDayPending)

    // Cancel all possible weekly day slots (1=Sun..7=Sat)
    for (day in 1..7) {
        val intent = Intent(context, GoalDailyReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, majorAlarmRequestCode(goal.id, day), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }
}

/**
 * Combined scheduled reminder for all minor goals sharing SCHEDULED mode.
 * Uses the schedule of the first minor goal found (since minors share one notification,
 * they're expected to share one schedule too — the UI should enforce/clarify this).
 */
fun scheduleCombinedMinorReminder(context: Context, minorGoals: List<Goal>) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val activeMinors = minorGoals.filter { it.reminderMode == GoalReminderMode.SCHEDULED && !it.isEffectivelyDone() }

    // Cancel any existing combined alarms first
    val everyDayIntent = Intent(context, GoalDailyReminderReceiver::class.java)
    alarmManager.cancel(
        PendingIntent.getBroadcast(context, 9_999_999, everyDayIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    )
    for (day in 1..7) {
        val intent = Intent(context, GoalDailyReminderReceiver::class.java)
        alarmManager.cancel(
            PendingIntent.getBroadcast(context, 9_999_999 + day, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )
    }

    if (activeMinors.isEmpty()) return

    val schedule = activeMinors.first().schedule
    val text = activeMinors.joinToString(" · ") { it.title }

    val baseIntent = Intent(context, GoalDailyReminderReceiver::class.java).apply {
        putExtra("goal_id", "minor_combined")
        putExtra("is_major", false)
        putExtra("title", "Today's goals (${activeMinors.size})")
        putExtra("text", text)
    }

    if (schedule.isEveryDay) {
        val pendingIntent = PendingIntent.getBroadcast(
            context, 9_999_999, baseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = nextTriggerTimeMillis(schedule.hour, schedule.minute)
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, AlarmManager.INTERVAL_DAY, pendingIntent)
    } else {
        schedule.daysOfWeek.forEach { dayOfWeek ->
            val pendingIntent = PendingIntent.getBroadcast(
                context, 9_999_999 + dayOfWeek, baseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerTime = nextTriggerTimeMillisForDay(schedule.hour, schedule.minute, dayOfWeek)
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP, triggerTime,
                AlarmManager.INTERVAL_DAY * 7, pendingIntent
            )
        }
    }
}

private fun nextTriggerTimeMillis(hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (cal.timeInMillis <= System.currentTimeMillis()) {
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return cal.timeInMillis
}

private fun nextTriggerTimeMillisForDay(hour: Int, minute: Int, dayOfWeek: Int): Long {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        set(Calendar.DAY_OF_WEEK, dayOfWeek)
    }
    if (cal.timeInMillis <= System.currentTimeMillis()) {
        cal.add(Calendar.DAY_OF_YEAR, 7)
    }
    return cal.timeInMillis
}

// ════════════════════════════════════════════════════════════════════════════
//  SYNC ENTRY POINT
//  Call this whenever the goals list changes (create/update/delete/toggle).
// ════════════════════════════════════════════════════════════════════════════

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun syncGoalNotifications(context: Context, goals: List<Goal>) {
    refreshPinnedGoalNotifications(context, goals)

    val activeGoals = goals.filter { !it.isEffectivelyDone() }

    val scheduledMajors = activeGoals.filter { it.isMajor && it.reminderMode == GoalReminderMode.SCHEDULED }
    scheduledMajors.forEach { goal -> scheduleGoalReminder(context, goal) }

    val allMajors = goals.filter { it.isMajor }
    allMajors.filterNot { it.reminderMode == GoalReminderMode.SCHEDULED && !it.isEffectivelyDone() }
        .forEach { goal -> cancelGoalReminder(context, goal) }

    val scheduledMinors = activeGoals.filter { !it.isMajor }
    scheduleCombinedMinorReminder(context, scheduledMinors)
}