package com.example.memogotchi.ui.page

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import java.text.SimpleDateFormat
import java.util.*

// ── Palette (matches TasksScreen) ─────────────────────────────────────────────
private val BgColor       = Color(0xFF16171C)
private val SurfaceColor  = Color(0xFF1F2125)
private val AccentGreen   = Color(0xFF77C59D)
private val AccentOrange  = Color(0xFFE8925A)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF888888)
private val TrackColor    = Color(0xFF2C2E34)

private fun goalCategoryColor(cat: AppCategory?) = when (cat) {
    AppCategory.SOCIAL        -> Color(0xFF6B9FD4)
    AppCategory.GAMES         -> Color(0xFFB07FD4)
    AppCategory.ENTERTAINMENT -> Color(0xFFE8925A)
    AppCategory.BROWSER       -> Color(0xFF77C59D)
    AppCategory.PRODUCTIVITY  -> Color(0xFFD4C56B)
    AppCategory.OTHER, null   -> AccentGreen
}

// Goal tags now use the shared `tagCategories` grouping from TagCategories.kt

// ════════════════════════════════════════════════════════════════════════════
//  GOALS SECTION  (drop into TasksScreen's LazyColumn as items)
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun GoalsSectionHeader(onAddGoal: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Goals", fontFamily = GildaDisplay, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(AccentGreen.copy(alpha = 0.15f))
                .border(1.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .clickable { onAddGoal() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Add, contentDescription = "Add goal", tint = AccentGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Goal", fontSize = 11.sp, color = AccentGreen, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun GoalCard(
    goal: Goal,
    todayCategoryMinutes: Int?,   // minutes used today for goal.category, if applicable
    onToggleDone: () -> Unit,
    onToggleChecklistItem: (String) -> Unit,
    onUnpin: () -> Unit,          // removes pinned reminder, sets reminderMode back to NONE
    onRepin: () -> Unit,         // re-enables pinned reminder, sets reminderMode to PINNED
    onEdit: () -> Unit,          // opens AddGoalSheet pre-filled for editing
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val accent = goalCategoryColor(goal.category)
    val (doneCount, totalCount) = goal.checklistProgress()
    val isDone = goal.isEffectivelyDone()

    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = if (isDone) Color(0xFF1A1D1A) else SurfaceColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .shadow(
                elevation = if (isDone) 2.dp else 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.5f),
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Icon badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.15f))
                        .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (goal.isMajor) Icons.Outlined.Flag else Icons.Outlined.TrackChanges,
                        contentDescription = null, tint = accent, modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = goal.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = if (isDone) TextSecondary else TextPrimary,
                            textDecoration = if (isDone) TextDecoration.LineThrough else null,
                        )
                        if (goal.isMajor) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                    .background(accent.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text("MAJOR", fontSize = 8.sp, color = accent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (goal.description.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(goal.description, fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp)
                    }

                    // Tags
                    if (goal.tags.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            goal.tags.take(3).forEach { tag ->
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                        .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(tag, fontSize = 9.sp, color = accent.copy(alpha = 0.85f))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Screen-time progress bar (if tracked)
                    if (goal.isScreenTimeTracked) {
                        val used = todayCategoryMinutes ?: 0
                        val target = goal.targetMinutes ?: 1
                        val frac = (used.toFloat() / target).coerceIn(0f, 1f)
                        val overLimit = used > target
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.weight(1f).height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)).background(TrackColor)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(frac).fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (overLimit) Color(0xFFE05252) else accent)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            val usedH = used / 60
                            val usedM = used % 60
                            val targetH = target / 60
                            val targetM = target % 60
                            Text(
                                "${usedH}h${usedM}m / ${targetH}h${targetM}m",
                                fontSize = 10.sp, color = TextSecondary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Checklist progress + expand toggle (shown whenever checklist exists)
                    if (goal.hasChecklist) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$doneCount / $totalCount checklist items", fontSize = 11.sp, color = accent, fontWeight = FontWeight.Medium)
                            val rotation by animateFloatAsState(
                                targetValue = if (expanded) 180f else 0f,
                                animationSpec = tween(200),
                                label = "chevron"
                            )
                            Icon(
                                Icons.Outlined.ExpandMore, contentDescription = "Expand",
                                tint = TextSecondary, modifier = Modifier.size(16.dp).rotate(rotation)
                            )
                        }

                        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                goal.checklist.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                            .clickable { onToggleChecklistItem(item.id) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(18.dp).clip(CircleShape)
                                                .background(if (item.isDone) accent else TrackColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (item.isDone) Text("✓", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            item.text, fontSize = 12.sp,
                                            color = if (item.isDone) TextSecondary else TextPrimary,
                                            textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Deadline chip
                    if (goal.deadlineMs != null) {
                        Spacer(Modifier.height(6.dp))
                        val deadlineStr = SimpleDateFormat("MMM d", Locale.ENGLISH).format(Date(goal.deadlineMs))
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(TextSecondary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Due $deadlineStr", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                // ── Right side: delete, edit, pin row + done circle below ───
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete goal",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp).clickable { onDelete() }
                        )
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit goal",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp).clickable { onEdit() }
                        )
                        if (goal.reminderMode == GoalReminderMode.PINNED) {
                            Icon(
                                Icons.Filled.PushPin,
                                contentDescription = "Unpin reminder",
                                tint = accent,
                                modifier = Modifier.size(15.dp)
                                    .clickable {
                                        try {
                                            cancelPinnedGoalNotification(context, goal)
                                        } catch (e: SecurityException) { }
                                        onUnpin()
                                    }
                            )
                        } else {
                            Icon(
                                Icons.Outlined.PushPin,
                                contentDescription = "Pin reminder",
                                tint = TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(15.dp)
                                    .clickable { onRepin() }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    if (!goal.hasChecklist) {
                        Box(
                            modifier = Modifier.size(24.dp).clip(CircleShape)
                                .background(if (goal.isDone) AccentGreen else TrackColor)
                                .clickable { onToggleDone() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (goal.isDone) Text("✓", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier.size(24.dp).clip(CircleShape)
                                .background(if (isDone) AccentGreen else TrackColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) Text("✓", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompletedGoalRow(goal: Goal, onDelete: () -> Unit) {
    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = Color(0xFF1A1D1A),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.5f),
            )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(AccentGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
            }
            Text(
                goal.title, fontSize = 13.sp, color = TextSecondary,
                textDecoration = TextDecoration.LineThrough, modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Outlined.DeleteOutline, contentDescription = "Delete",
                tint = TextSecondary, modifier = Modifier.size(18.dp).clickable { onDelete() }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  ADD GOAL SHEET
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalSheet(
    existingGoal: Goal? = null,
    onDismiss: () -> Unit,
    onCreate: (Goal) -> Unit,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(existingGoal?.title ?: "") }
    var description by remember { mutableStateOf(existingGoal?.description ?: "") }
    var enableScreenTime by remember { mutableStateOf(existingGoal?.isScreenTimeTracked ?: false) }
    var selectedCategory by remember { mutableStateOf(existingGoal?.category) }
    var targetMinutesTotal by remember { mutableStateOf(existingGoal?.targetMinutes ?: 60) }
    var isMajor by remember { mutableStateOf(existingGoal?.isMajor ?: false) }
    val checklistItems = remember {
        mutableStateListOf<String>().also { it.addAll(existingGoal?.checklist?.map { c -> c.text } ?: emptyList()) }
    }
    var newChecklistText by remember { mutableStateOf("") }
    var deadlineMs by remember { mutableStateOf(existingGoal?.deadlineMs) }
    val selectedTags = remember {
        mutableStateListOf<String>().also { it.addAll(existingGoal?.tags ?: emptyList()) }
    }
    var reminderMode by remember { mutableStateOf(existingGoal?.reminderMode ?: GoalReminderMode.NONE) }
    var scheduleHour by remember { mutableStateOf(existingGoal?.schedule?.hour ?: 0) }
    var scheduleMinute by remember { mutableStateOf(existingGoal?.schedule?.minute ?: 0) }
    val scheduleDays = remember {
        mutableStateListOf<Int>().also { it.addAll(existingGoal?.schedule?.daysOfWeek ?: emptySet()) }
    }
    var showScheduleDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                if (existingGoal != null) "Edit Goal" else "New Goal",
                fontFamily = GildaDisplay, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary
            )
            Spacer(Modifier.height(16.dp))

            // Title
            TextField(
                value = title, onValueChange = { title = it },
                placeholder = { Text("What's the goal?", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceColor, unfocusedContainerColor = SurfaceColor,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(Modifier.height(10.dp))

            // Description
            TextField(
                value = description, onValueChange = { description = it },
                placeholder = { Text("Add a note (optional)", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 13.sp, color = TextPrimary),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceColor, unfocusedContainerColor = SurfaceColor,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(Modifier.height(16.dp))

            // Mark as major goal toggle (independent of checklist)
            ToggleRow(
                label = "Mark as major goal",
                checked = isMajor,
                onCheckedChange = { isMajor = it }
            )

            Spacer(Modifier.height(16.dp))

            // Tags (same set as diary entries — feeds Memo's tag tally)
            Text("Tags (optional)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            GroupedTagPicker(
                selectedTags = selectedTags,
                onToggleTag = { tag ->
                    if (tag in selectedTags) selectedTags.remove(tag) else selectedTags.add(tag)
                },
                accentColor = AccentGreen,
                textSecondaryColor = TextSecondary,
            )

            Spacer(Modifier.height(8.dp))

            // Reminder mode
            Text("Remind me", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReminderModeChip(
                    label = "None", selected = reminderMode == GoalReminderMode.NONE,
                    onClick = { reminderMode = GoalReminderMode.NONE }
                )
                ReminderModeChip(
                    label = "Pinned", selected = reminderMode == GoalReminderMode.PINNED,
                    onClick = { reminderMode = GoalReminderMode.PINNED }
                )
                ReminderModeChip(
                    label = "Daily",
                    selected = reminderMode == GoalReminderMode.SCHEDULED,
                    onClick = {
                        reminderMode = GoalReminderMode.SCHEDULED
                        scheduleHour = 0
                        scheduleMinute = 0
                        scheduleDays.clear() // every day, default midnight
                    }
                )
            }

            if (reminderMode == GoalReminderMode.SCHEDULED) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(SurfaceColor)
                        .clickable { showScheduleDialog = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeStr = String.format("%02d:%02d", scheduleHour, scheduleMinute)
                    val daysStr = if (scheduleDays.isEmpty()) "Every day"
                    else scheduleDays.sorted().joinToString(", ") { dayOfWeekShortName(it) }
                    Text("$timeStr · $daysStr", fontSize = 13.sp, color = TextPrimary)
                    Text("Schedule", fontSize = 12.sp, color = AccentGreen, fontWeight = FontWeight.Medium)
                }
            }

            if (reminderMode != GoalReminderMode.NONE) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (goalHasChecklistPreview(checklistItems))
                        "This goal has a checklist — open the app to check items off."
                    else
                        "You'll get a notification for this goal.",
                    fontSize = 10.sp, color = TextSecondary
                )
            }

            Spacer(Modifier.height(16.dp))

            // Screen-time toggle
            ToggleRow(
                label = "Track screen time for a category",
                checked = enableScreenTime,
                onCheckedChange = { enableScreenTime = it }
            )
            AnimatedVisibility(visible = enableScreenTime) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        AppCategory.entries.take(4).forEach { cat ->
                            val selected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) AccentGreen.copy(alpha = 0.2f) else SurfaceColor)
                                    .border(1.dp, if (selected) AccentGreen else TextSecondary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(cat.label, fontSize = 11.sp, color = if (selected) AccentGreen else TextSecondary)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Daily limit", fontSize = 11.sp, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    TimeDialPicker(
                        totalMinutes = targetMinutesTotal,
                        onValueChange = { targetMinutesTotal = it },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Checklist field (always visible, optional)
            Text("Checklist (optional)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Column {
                checklistItems.forEachIndexed { i, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("•", color = AccentGreen, modifier = Modifier.padding(end = 8.dp))
                        Text(item, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Outlined.Close, contentDescription = "Remove",
                            tint = TextSecondary, modifier = Modifier.size(16.dp)
                                .clickable { checklistItems.removeAt(i) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    TextField(
                        value = newChecklistText, onValueChange = { newChecklistText = it },
                        placeholder = { Text("Add a step", fontSize = 13.sp, color = TextSecondary) },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(fontSize = 13.sp, color = TextPrimary),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SurfaceColor, unfocusedContainerColor = SurfaceColor,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(AccentGreen)
                            .clickable {
                                if (newChecklistText.isNotBlank()) {
                                    checklistItems.add(newChecklistText.trim())
                                    newChecklistText = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add step", tint = BgColor, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Deadline
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(SurfaceColor)
                    .clickable {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> deadlineMs = Calendar.getInstance().apply { set(y, m, d) }.timeInMillis },
                            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = deadlineMs?.let { "Due " + SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).format(Date(it)) }
                        ?: "Set a deadline (optional)",
                    fontSize = 13.sp, color = if (deadlineMs != null) TextPrimary else TextSecondary
                )
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Create / Save button
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (title.isNotBlank()) AccentGreen else Color(0xFF2A2A30))
                    .clickable {
                        if (title.isNotBlank()) {
                            // Preserve isDone state for checklist items that already existed
                            val existingChecklistByText = existingGoal?.checklist
                                ?.associateBy { it.text } ?: emptyMap()
                            val newChecklist = checklistItems.map { text ->
                                existingChecklistByText[text]?.copy(text = text)
                                    ?: ChecklistItem(text = text)
                            }

                            val goal = Goal(
                                id            = existingGoal?.id ?: java.util.UUID.randomUUID().toString(),
                                title         = title.trim(),
                                description   = description.trim(),
                                category      = if (enableScreenTime) selectedCategory else null,
                                targetMinutes = if (enableScreenTime) targetMinutesTotal else null,
                                checklist     = newChecklist,
                                isMajor       = isMajor,
                                tags          = selectedTags.toList(),
                                reminderMode  = reminderMode,
                                schedule      = GoalSchedule(
                                    hour = scheduleHour,
                                    minute = scheduleMinute,
                                    daysOfWeek = scheduleDays.toSet(),
                                ),
                                deadlineMs    = deadlineMs,
                                createdAtMs   = existingGoal?.createdAtMs ?: System.currentTimeMillis(),
                                isDone        = existingGoal?.isDone ?: false,
                                completedAtMs = existingGoal?.completedAtMs,
                            )

                            // Only tally tags that weren't already on this goal (avoids double-counting on edits)
                            val previousTags = existingGoal?.tags ?: emptyList()
                            val newlyAddedTags = selectedTags.filterNot { it in previousTags }
                            GoalStore.incrementTagTally(context, newlyAddedTags)

                            onCreate(goal)
                        }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (existingGoal != null) "Save Changes" else "Create Goal",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = if (title.isNotBlank()) BgColor else TextSecondary
                )
            }
        }

        if (showScheduleDialog) {
            SchedulePickerDialog(
                initialHour = scheduleHour,
                initialMinute = scheduleMinute,
                initialDays = scheduleDays.toSet(),
                onDismiss = { showScheduleDialog = false },
                onConfirm = { hour, minute, days ->
                    scheduleHour = hour
                    scheduleMinute = minute
                    scheduleDays.clear()
                    scheduleDays.addAll(days)
                    showScheduleDialog = false
                }
            )
        }
    }
}

private fun dayOfWeekShortName(day: Int): String = when (day) {
    Calendar.SUNDAY    -> "Sun"
    Calendar.MONDAY    -> "Mon"
    Calendar.TUESDAY   -> "Tue"
    Calendar.WEDNESDAY -> "Wed"
    Calendar.THURSDAY  -> "Thu"
    Calendar.FRIDAY    -> "Fri"
    Calendar.SATURDAY  -> "Sat"
    else -> "?"
}

@Composable
private fun SchedulePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    initialDays: Set<Int>,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int, days: Set<Int>) -> Unit,
) {
    var totalMinutes by remember { mutableStateOf(initialHour * 60 + initialMinute) }
    val selectedDays = remember { mutableStateListOf<Int>().also { it.addAll(initialDays) } }
    val allDays = listOf(
        Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = BgColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Schedule reminder", fontFamily = GildaDisplay, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(16.dp))

                Text("Time", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                TimeDialPicker(
                    totalMinutes = totalMinutes,
                    onValueChange = { totalMinutes = it },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(16.dp))
                Text("Repeat on", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selectedDays.isEmpty()) AccentGreen.copy(alpha = 0.2f) else SurfaceColor)
                        .border(1.dp, if (selectedDays.isEmpty()) AccentGreen else TextSecondary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .clickable { selectedDays.clear() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Every day", fontSize = 12.sp, color = if (selectedDays.isEmpty()) AccentGreen else TextSecondary)
                }

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    allDays.forEach { day ->
                        val selected = day in selectedDays
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(36.dp)
                                .background(if (selected) AccentGreen.copy(alpha = 0.2f) else SurfaceColor)
                                .border(1.dp, if (selected) AccentGreen else TextSecondary.copy(alpha = 0.3f), CircleShape)
                                .clickable {
                                    if (selected) selectedDays.remove(day) else selectedDays.add(day)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                dayOfWeekShortName(day).take(1), fontSize = 11.sp,
                                color = if (selected) AccentGreen else TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(AccentGreen)
                        .clickable {
                            onConfirm(totalMinutes / 60, totalMinutes % 60, selectedDays.toSet())
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Done", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BgColor)
                }
            }
        }
    }
}

private fun goalHasChecklistPreview(items: List<String>) = items.isNotEmpty()

@Composable
private fun ReminderModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) AccentGreen.copy(alpha = 0.2f) else SurfaceColor)
            .border(1.dp, if (selected) AccentGreen else TextSecondary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 11.sp, color = if (selected) AccentGreen else TextSecondary)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AccentGreen, checkedThumbColor = Color.White,
                uncheckedTrackColor = TrackColor, uncheckedThumbColor = TextSecondary,
            )
        )
    }
}