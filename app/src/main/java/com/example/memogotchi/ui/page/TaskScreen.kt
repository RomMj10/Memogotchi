package com.example.memogotchi.ui.page

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.alpha
import androidx.core.content.ContextCompat
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay

enum class TaskCardStatus {
    IDLE,
    ACTIVE,
    LOCKED,
    DONE
}
// ── Palette (matches ScreenTimeScreen) ───────────────────────────────────────
private val BgColor        = Color(0xFF16171C)
private val SurfaceColor   = Color(0xFF1F2125)
private val AccentGreen    = Color(0xFF77C59D)
private val AccentOrange   = Color(0xFFE8925A)
private val AccentBlue     = Color(0xFF6B9FD4)
private val AccentPurple   = Color(0xFFB07FD4)
private val TextPrimary    = Color(0xFFFFFFFF)
private val TextSecondary  = Color(0xFF888888)
private val TrackColor     = Color(0xFF2C2E34)
private val LockedColor    = Color(0xFF2A2B30)

// ── Category accent colours ───────────────────────────────────────────────────
private fun categoryColor(cat: AppCategory) = when (cat) {
    AppCategory.SOCIAL        -> Color(0xFF6B9FD4)
    AppCategory.GAMES         -> Color(0xFFB07FD4)
    AppCategory.ENTERTAINMENT -> Color(0xFFE8925A)
    AppCategory.BROWSER       -> Color(0xFF77C59D)
    AppCategory.PRODUCTIVITY  -> Color(0xFFD4C56B)
    AppCategory.OTHER         -> Color(0xFF888888)
}

// ── Permission-safe wrapper around syncGoalNotifications ─────────────────────
private fun syncGoalNotificationsSafely(context: android.content.Context, goals: List<Goal>) {
    val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    if (granted) {
        try {
            syncGoalNotifications(context, goals)
        } catch (e: SecurityException) {
            // Permission revoked between check and call — fail silently, no crash
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  ROOT
// ════════════════════════════════════════════════════════════════════════════

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TasksScreen(today: DayData? = null, weekData: List<DayData> = emptyList(),
                onTasksGenerated: (List<AnalogTask>) -> Unit = {},
                onStartTaskTimer: (AnalogTask) -> Unit = {},
                activeTaskTimer: ActiveTaskTimer? = null,
                activeElapsedSeconds: Long = 0L,
                onCancelTaskTimer: (AnalogTask) -> Unit = {},
                onTaskCompleted: (AnalogTask) -> Unit = {},
) {
    var geminiStatus by remember { mutableStateOf("IDLE") }

    val context      = LocalContext.current
    val batteryLevel = remember { getBatteryLevel(context) }
    val totalHours   = remember(today) { (today?.totalMs ?: 0L) / 3_600_000.0 }
    val totalFocusLabel = remember(totalHours) {
        val h = totalHours.toInt()
        val m = ((totalHours - h) * 60).toInt()
        if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    val dateKey = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }


    val THRESHOLD_MIN = 60 * 60 * 1000L

    var tasks by remember { mutableStateOf<List<AnalogTask>>(emptyList()) }
    var taskSource by remember { mutableStateOf("rule") }

    // ── Goals state ───────────────────────────────────────────────────────
    var goals by remember { mutableStateOf<List<Goal>>(emptyList()) }
    var showAddGoalSheet by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<Goal?>(null) }

    LaunchedEffect(Unit) {
        goals = GoalStore.loadGoals(context)
        syncGoalNotificationsSafely(context, goals)
    }
    LaunchedEffect(activeTaskTimer) {
        TaskStore.loadTasksForDate(context, dateKey)?.let {tasks = it}
    }

    fun persistGoals(updated: List<Goal>) {
        goals = updated
        GoalStore.saveGoals(context, updated)
        syncGoalNotificationsSafely(context, updated)
    }

    // Today's minutes used per category (for screen-time-tracked goals)
    val categoryMinutesToday = remember(today) {
        val cats = today?.apps?.map { app ->
            getAppCategory(context, app.packageName) to (app.totalTimeMs / 60_000).toInt()
        } ?: emptyList()
        cats.groupBy { it.first }.mapValues { (_, v) -> v.sumOf { it.second } }
    }

    val activeGoals    = goals.filter { !it.isEffectivelyDone() }
    val completedGoals = goals.filter { it.isEffectivelyDone() }

    LaunchedEffect(today, batteryLevel, dateKey) {
        val cached = TaskStore.loadTasksForDate(context, dateKey)
        if (cached != null && cached.isNotEmpty()) {
            tasks = cached
            taskSource = TaskStore.getSourceForDate(context, dateKey) ?: "rule"
            return@LaunchedEffect
        }
        PersonalityStore.rollupScreenCategoryTallyIfNeeded(context, today)

        val screenTimeMs = today?.totalMs ?: 0L

        if (screenTimeMs >= THRESHOLD_MIN) {
            val ruleTasks = generateAnalogTasks(context, today, batteryLevel)
            tasks = ruleTasks
            taskSource = "rule"
            TaskStore.saveTasksForDate(context, dateKey, ruleTasks, "rule")
            onTasksGenerated(ruleTasks)

            if (isNetworkAvailable(context)) {
                val cats = today?.apps?.take(10)?.map { app ->
                    CategorizedApp(
                        info = app,
                        category = getAppCategory(context, app.packageName),
                        hours = app.totalTimeMs / 3_600_000.0,
                    )
                } ?: emptyList()

                val completedHistory = TaskStore.getCompletedHistory(context)
                geminiStatus = "Status: Connecting"

                var geminiTasks = emptyList<AnalogTask>()
                try {
                    geminiTasks =
                        generateTasksWithGemini(weekData, batteryLevel, cats, completedHistory)
                } catch (e: Exception) {
                    geminiStatus = "Status: Failed"
                }

                if (geminiTasks.isNotEmpty()) {
                    tasks = geminiTasks
                    geminiStatus = "Status: Success"
                    taskSource = "gemini"
                    TaskStore.saveTasksForDate(context, dateKey, geminiTasks, "gemini")
                    onTasksGenerated(ruleTasks)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        LazyColumn(
            modifier            = Modifier.fillMaxSize().background(BgColor),
            contentPadding      = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            item {
                HeaderCard(
                    taskCount       = tasks.size,
                    totalFocusLabel = totalFocusLabel,
                    batteryLevel    = batteryLevel,
                )
            }

            item {
                GoalsSectionHeader(onAddGoal = {
                    editingGoal = null
                    showAddGoalSheet = true
                })
            }

            if (activeGoals.isEmpty() && completedGoals.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No goals yet. Tap \"Add Goal\" to set one.",
                            color = TextSecondary, fontFamily = GildaDisplay, fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(activeGoals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        todayCategoryMinutes = goal.category?.let { categoryMinutesToday[it] },
                        onToggleDone = {
                            persistGoals(goals.map {
                                if (it.id == goal.id) it.copy(isDone = !it.isDone) else it
                            })
                        },
                        onToggleChecklistItem = { itemId ->
                            persistGoals(goals.map { g ->
                                if (g.id == goal.id) {
                                    g.copy(checklist = g.checklist.map { item ->
                                        if (item.id == itemId) item.copy(isDone = !item.isDone) else item
                                    })
                                } else g
                            })
                        },
                        onUnpin = {
                            persistGoals(goals.map {
                                if (it.id == goal.id) it.copy(reminderMode = GoalReminderMode.NONE) else it
                            })
                        },
                        onRepin = {
                            persistGoals(goals.map {
                                if (it.id == goal.id) it.copy(reminderMode = GoalReminderMode.PINNED) else it
                            })
                        },
                        onEdit = {
                            editingGoal = goal
                            showAddGoalSheet = true
                        },
                        onDelete = {
                            persistGoals(goals.filterNot { it.id == goal.id })
                        }
                    )
                }

                if (completedGoals.isNotEmpty()) {
                    item {
                        Text(
                            "Completed", fontFamily = GildaDisplay, fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold, color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(completedGoals, key = { it.id + "_completed" }) { goal ->
                        CompletedGoalRow(
                            goal = goal,
                            onDelete = { persistGoals(goals.filterNot { it.id == goal.id }) }
                        )
                    }
                }
            }

            item { SectionHeader(title = "Analog Tasks") }

            if (tasks.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No tasks yet, suggestions are generated the more you use your phone.",
                            color    = TextSecondary,
                            fontFamily = GildaDisplay,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }
            } else {
                items(tasks, key = { it.id }) { task ->
                    val status = when {
                        task.isDone -> TaskCardStatus.DONE
                        activeTaskTimer?.taskId == task.id -> TaskCardStatus.ACTIVE
                        activeTaskTimer != null -> TaskCardStatus.LOCKED
                        else -> TaskCardStatus.IDLE
                    }
                    val remaining = if (status == TaskCardStatus.ACTIVE)
                        (activeTaskTimer!!.targetSeconds - activeElapsedSeconds).coerceAtLeast(0L)
                    else 0L

                    TaskCard(
                        task = task,
                        status = status,
                        remainingSeconds = remaining,
                        onStartTask = onStartTaskTimer,
                        onSkip = { skipped ->
                            tasks = tasks.filterNot { it.id == skipped.id }
                            TaskStore.saveTasksForDate(context, dateKey, tasks, taskSource)
                        },
                        onConvertToGoal = { t ->
                            persistGoals(goals + Goal(title = t.title, description = t.description, category = t.category))
                            tasks = tasks.filterNot { it.id == t.id }
                            TaskStore.saveTasksForDate(context, dateKey, tasks, taskSource)
                        },
                        onCancelTask = onCancelTaskTimer,
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }

        if (showAddGoalSheet) {
            AddGoalSheet(
                existingGoal = editingGoal,
                onDismiss = {
                    showAddGoalSheet = false
                    editingGoal = null
                },
                onCreate = { savedGoal ->
                    val isEdit = goals.any { it.id == savedGoal.id }
                    persistGoals(
                        if (isEdit) goals.map { if (it.id == savedGoal.id) savedGoal else it }
                        else goals + savedGoal
                    )
                    showAddGoalSheet = false
                    editingGoal = null
                }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  HEADER CARD
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun HeaderCard(taskCount: Int, totalFocusLabel: String, batteryLevel: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0D0E11))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Tasks",
                fontFamily = GildaDisplay,
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
            )
            Row(
                modifier            = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text       = taskCount.toString(),
                        fontFamily = GildaDisplay,
                        fontSize   = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                    )
                    Text("Tasks Found", fontFamily = GildaDisplay, fontSize = 11.sp, color = TextSecondary)
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(Color(0xFF2A2B30))
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text       = totalFocusLabel,
                        fontFamily = GildaDisplay,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color      = AccentGreen,
                    )
                    Text("Total Focus",fontFamily = GildaDisplay, fontSize = 11.sp, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(16.dp))

        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  SECTION HEADER
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun SectionHeader(title: String) {
    Text(
        text       = title,
        fontFamily = GildaDisplay,
        fontSize   = 16.sp,
        fontWeight = FontWeight.Bold,
        color      = TextPrimary,
        modifier   = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

// ════════════════════════════════════════════════════════════════════════════
//  TASK CARD
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: AnalogTask,
    status: TaskCardStatus,
    remainingSeconds: Long = 0L,
    onStartTask: (AnalogTask) -> Unit = {},
    onSkip: (AnalogTask) -> Unit = {},
    onConvertToGoal: (AnalogTask) -> Unit = {},
    onCancelTask: (AnalogTask) -> Unit = {},
) {
    val accent = categoryColor(task.category)
    val cardAlpha = if (status == TaskCardStatus.LOCKED) 0.45f else 1f
    val bgColor by animateColorAsState(
        when (status) {
            TaskCardStatus.DONE   -> Color(0xFF1A1D1A)
            TaskCardStatus.ACTIVE -> Color(0xFF1B2A22)
            else                  -> SurfaceColor
        }, tween(300), label = "taskbg"
    )
    var showQuickMenu by remember { mutableStateOf(false) }

    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .alpha(cardAlpha)
            .then(
                if (status == TaskCardStatus.ACTIVE)
                    Modifier.border(1.5.dp, AccentGreen.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                else Modifier
            )
    ) {
        Box {
            Row(
                modifier = Modifier
                    .padding(14.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (status == TaskCardStatus.IDLE || status == TaskCardStatus.ACTIVE) showQuickMenu = true
                        }
                    ),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.15f))
                        .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(task.category.emoji, fontSize = 18.sp) }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (status == TaskCardStatus.DONE) TextSecondary else TextPrimary,
                        textDecoration = if (status == TaskCardStatus.DONE) TextDecoration.LineThrough else null,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(task.description, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Pill(text = task.triggerReason, color = accent)
                        Pill(text = "${task.durationMinutes} min", color = TextSecondary)
                    }
                }

                when (status) {
                    TaskCardStatus.DONE -> Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape).background(AccentGreen),
                        contentAlignment = Alignment.Center
                    ) { Text("✓", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold) }

                    TaskCardStatus.ACTIVE -> {
                        val m = remainingSeconds / 60
                        val s = remainingSeconds % 60
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(String.format("%02d:%02d", m, s), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                            Text("left", fontSize = 8.sp, color = TextSecondary)
                        }
                    }

                    TaskCardStatus.LOCKED -> Icon(
                        Icons.Outlined.Lock, contentDescription = "Locked",
                        tint = TextSecondary, modifier = Modifier.size(18.dp)
                    )

                    TaskCardStatus.IDLE -> Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape)
                            .border(1.5.dp, TrackColor, CircleShape)
                    )
                }
            }

            DropdownMenu(expanded = showQuickMenu, onDismissRequest = { showQuickMenu = false }) {
                when (status) {
                    TaskCardStatus.IDLE -> {
                        DropdownMenuItem(
                            text = { Text("Start Task") },
                            leadingIcon = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) },
                            onClick = { showQuickMenu = false; onStartTask(task) }
                        )
                        DropdownMenuItem(
                            text = { Text("Skip") },
                            leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                            onClick = { showQuickMenu = false; onSkip(task) }
                        )
                        DropdownMenuItem(
                            text = { Text("Convert to Goal") },
                            leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                            onClick = { showQuickMenu = false; onConvertToGoal(task) }
                        )
                    }
                    TaskCardStatus.ACTIVE -> DropdownMenuItem(
                        text = { Text("Cancel Task") },
                        leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                        onClick = { showQuickMenu = false; onCancelTask(task) }
                    )
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun Pill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}