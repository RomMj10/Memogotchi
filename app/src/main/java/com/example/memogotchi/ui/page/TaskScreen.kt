package com.example.memogotchi.ui.page

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
import androidx.compose.foundation.clickable

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

// ════════════════════════════════════════════════════════════════════════════
//  ROOT
// ════════════════════════════════════════════════════════════════════════════

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TasksScreen(today: DayData? = null) {
    val context      = LocalContext.current
    val batteryLevel = remember { getBatteryLevel(context) }
    val totalHours   = remember(today) { (today?.totalMs ?: 0L) / 3_600_000.0 }
    val totalFocusLabel = remember(totalHours) {
        val h = totalHours.toInt()
        val m = ((totalHours - h) * 60).toInt()
        if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    var tasks by remember { mutableStateOf<List<AnalogTask>>(emptyList()) }
    val milestones = remember(totalHours) { generateMilestones(totalHours) }

    LaunchedEffect(today, batteryLevel) {
        val categorized = generateAnalogTasks(context, today, batteryLevel)
        tasks = categorized

        val cats = today?.apps?.take(10)?.map { app ->
            CategorizedApp(
                info = app,
                category = getAppCategory(context, app.packageName),
                hours = app.totalTimeMs / 3_600_000.0,
            )
        } ?: emptyList()

        val geminiTasks = generateTasksWithGemini(today, batteryLevel, cats)
        if (geminiTasks.isNotEmpty()) tasks = geminiTasks
    }

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(BgColor),
        contentPadding      = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        // ── Header stats card ─────────────────────────────────────────────
        item {
            HeaderCard(
                taskCount       = tasks.size,
                totalFocusLabel = totalFocusLabel,
                batteryLevel    = batteryLevel,
            )
        }

        // ── Analog Tasks section ──────────────────────────────────────────
        item {
            SectionHeader(title = "Analog Tasks")
        }

        if (tasks.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No tasks yet — use your phone a bit more to trigger suggestions 🌱",
                        color    = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                    )
                }
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                TaskCard(
                    task     = task,
                    onToggle = { toggled ->
                        tasks = tasks.map { if (it.id == toggled.id) it.copy(isDone = !it.isDone) else it }
                    }
                )
            }
        }

        // ── Milestones section ────────────────────────────────────────────
        item {
            SectionHeader(title = "Milestones")
        }

        items(milestones, key = { it.id }) { milestone ->
            MilestoneRow(milestone = milestone)
        }

        item { Spacer(Modifier.height(24.dp)) }
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
            Row(
                modifier            = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.CenterVertically,
            ) {
                // Tasks count
                Column {
                    Text(
                        text       = taskCount.toString(),
                        fontSize   = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                    )
                    Text("Tasks Found", fontSize = 11.sp, color = TextSecondary)
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(Color(0xFF2A2B30))
                )

                // Total Focus
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text       = totalFocusLabel,
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color      = AccentGreen,
                    )
                    Text("Total Focus", fontSize = 11.sp, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Battery row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔋", fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(TrackColor)
                ) {
                    val barColor = when {
                        batteryLevel <= 20 -> Color(0xFFE05252)
                        batteryLevel <= 50 -> AccentOrange
                        else               -> AccentGreen
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(batteryLevel / 100f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(barColor)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("$batteryLevel%", fontSize = 11.sp, color = TextSecondary)
            }
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
        fontSize   = 16.sp,
        fontWeight = FontWeight.Bold,
        color      = TextPrimary,
        modifier   = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

// ════════════════════════════════════════════════════════════════════════════
//  TASK CARD
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun TaskCard(task: AnalogTask, onToggle: (AnalogTask) -> Unit) {
    val accent    = categoryColor(task.category)
    val isDone    = task.isDone
    val bgColor by animateColorAsState(
        if (isDone) Color(0xFF1A1D1A) else SurfaceColor, tween(300), label = "taskbg"
    )

    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category emoji badge
            Box(
                modifier        = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.15f))
                    .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(task.category.emoji, fontSize = 18.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text           = task.title,
                    fontSize       = 14.sp,
                    fontWeight     = FontWeight.SemiBold,
                    color          = if (isDone) TextSecondary else TextPrimary,
                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                )
                Spacer(Modifier.height(2.dp))
                // Description
                Text(
                    text     = task.description,
                    fontSize = 12.sp,
                    color    = TextSecondary,
                    lineHeight = 17.sp,
                )
                Spacer(Modifier.height(8.dp))
                // Trigger pill + duration
                Column(horizontalAlignment = Alignment.End) {
                    Pill(text = task.triggerReason, color = accent)
                    Pill(text = "${task.durationMinutes} min", color = TextSecondary)
                }
            }

            // Done toggle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isDone) AccentGreen else TrackColor)
                    .clickable { onToggle(task) },
                contentAlignment = Alignment.Center
            ) {
                if (isDone) Text("✓", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
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

// ════════════════════════════════════════════════════════════════════════════
//  MILESTONE ROW
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun MilestoneRow(milestone: Milestone) {
    val isUnlocked = milestone.isUnlocked
    val bg         = if (isUnlocked) SurfaceColor else LockedColor
    val titleColor = if (isUnlocked) TextPrimary else TextSecondary
    val descColor  = if (isUnlocked) TextSecondary else Color(0xFF555555)

    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = bg,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Emoji badge
            Box(
                modifier        = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isUnlocked) AccentGreen.copy(alpha = 0.15f) else Color(0xFF222326)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = if (isUnlocked) milestone.emoji else "🔒",
                    fontSize = 16.sp,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(milestone.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
                Text(milestone.description, fontSize = 11.sp, color = descColor, lineHeight = 16.sp)
            }

            // Hours badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = "${milestone.requiredHours.toInt()}",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (isUnlocked) AccentGreen else Color(0xFF444444),
                )
                Text("HOURS", fontSize = 9.sp, color = descColor, fontWeight = FontWeight.Medium)
            }
        }
    }
}