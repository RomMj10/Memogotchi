package com.example.memogotchi.focusguard

import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.ui.theme.GildaDisplay
import kotlinx.coroutines.launch
import java.util.UUID

// ── Theme tokens ──────────────────────────────────────────────────────────────
private val BgColor       = Color(0xFF16171C)
private val SurfaceColor  = Color(0xFF1F2125)
private val AccentGreen   = Color(0xFF77C59D)
private val TextSecondary = Color(0xFF888888)
private val TextPrimary   = Color(0xFFF2F2F2)
private val WarnRed       = Color(0xFFD4537E)

// ── Helpers ───────────────────────────────────────────────────────────────────

private data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable
)

private val ALL_DAYS = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
)

private val DAY_LABELS = mapOf(
    DayOfWeek.MONDAY to "Mon", DayOfWeek.TUESDAY to "Tue",
    DayOfWeek.WEDNESDAY to "Wed", DayOfWeek.THURSDAY to "Thu",
    DayOfWeek.FRIDAY to "Fri", DayOfWeek.SATURDAY to "Sat",
    DayOfWeek.SUNDAY to "Sun"
)

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "%d:%02d %s".format(h, minute, amPm)
}

private fun daysSummary(days: List<DayOfWeek>): String {
    if (days.isEmpty()) return "No days"
    val sorted = days.sortedBy { it.number }
    // Check for common shorthands
    val weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    val weekend = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    return when {
        sorted == ALL_DAYS.sortedBy { it.number } -> "Every day"
        sorted == weekdays.sortedBy { it.number } -> "Mon–Fri"
        sorted == weekend.sortedBy { it.number }  -> "Weekends"
        else -> sorted.mapNotNull { DAY_LABELS[it] }.joinToString(", ")
    }
}

private fun loadInstalledApps(context: android.content.Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    @Suppress("DEPRECATION")
    return pm.queryIntentActivities(intent, 0)
        .map { it.activityInfo.applicationInfo }
        .filterNot { it.packageName == context.packageName }
        .distinctBy { it.packageName }
        .mapNotNull { info: ApplicationInfo ->
            runCatching {
                AppInfo(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    icon = pm.getApplicationIcon(info)
                )
            }.getOrNull()
        }
        .sortedBy { it.label.lowercase() }
}

private fun Drawable.toBitmapCompat(): android.graphics.Bitmap {
    if (this is android.graphics.drawable.BitmapDrawable) return bitmap
    val w = intrinsicWidth.takeIf { it > 0 } ?: 96
    val h = intrinsicHeight.takeIf { it > 0 } ?: 96
    val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bmp
}

// ── Internal form state ───────────────────────────────────────────────────────

private data class ScheduleFormState(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val selectedPackages: Set<String> = emptySet(),
    val activeDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    ),
    val startHour: Int = 8,
    val startMinute: Int = 0,
    val endHour: Int = 17,
    val endMinute: Int = 0
)

private fun ScheduleBlock.toFormState() = ScheduleFormState(
    id = id,
    name = name,
    selectedPackages = packageNamesList.toSet(),
    activeDays = activeDaysList.toSet(),
    startHour = startHour,
    startMinute = startMinute,
    endHour = endHour,
    endMinute = endMinute
)

private fun ScheduleFormState.toProto(enabled: Boolean = true): ScheduleBlock =
    ScheduleBlock.newBuilder()
        .setId(id)
        .setName(name.trim())
        .addAllPackageNames(selectedPackages)
        .addAllActiveDays(activeDays)
        .setStartHour(startHour)
        .setStartMinute(startMinute)
        .setEndHour(endHour)
        .setEndMinute(endMinute)
        .setIsEnabled(enabled)
        .build()

// ── Root composable ───────────────────────────────────────────────────────────

@Composable
fun ScheduleScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var schedules by remember { mutableStateOf<List<ScheduleBlock>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    // null = list view, non-null = editing/creating that form state
    var editingForm by remember { mutableStateOf<ScheduleFormState?>(null) }
    // Track whether we're in "delete confirm" mode for a given id
    var deleteTargetId by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        schedules = FocusGuardStore.loadConfig(context).schedulesList
    }

    LaunchedEffect(Unit) {
        installedApps = loadInstalledApps(context)
        reload()
        isLoading = false
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────
    val deleteTarget = deleteTargetId?.let { id -> schedules.firstOrNull { it.id == id } }
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTargetId = null },
            containerColor = SurfaceColor,
            title = {
                Text("Delete schedule?", color = TextPrimary, fontFamily = GildaDisplay)
            },
            text = {
                Text(
                    "\"${deleteTarget.name}\" will be removed permanently.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        FocusGuardStore.removeSchedule(context, deleteTarget.id)
                        reload()
                        deleteTargetId = null
                    }
                }) {
                    Text("Delete", color = WarnRed, fontFamily = GildaDisplay)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // ── Route to form or list ──────────────────────────────────────────────
    if (editingForm != null) {
        ScheduleFormView(
            form = editingForm!!,
            installedApps = installedApps,
            isNew = schedules.none { it.id == editingForm!!.id },
            onFormChange = { editingForm = it },
            onSave = { form, enabled ->
                scope.launch {
                    FocusGuardStore.upsertSchedule(context, form.toProto(enabled))
                    reload()
                    editingForm = null
                }
            },
            onBack = { editingForm = null }
        )
        return
    }

    // ── List view ──────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            ) {
                TextButton(onClick = onBack) {
                    Text("< Back", color = AccentGreen, fontFamily = GildaDisplay, fontSize = 16.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Schedules",
                    fontFamily = GildaDisplay,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(64.dp))
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGreen)
                }
            } else if (schedules.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No schedules yet", color = TextSecondary, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tap + to create a named block schedule",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(schedules, key = { it.id }) { schedule ->
                        ScheduleRow(
                            schedule = schedule,
                            onToggleEnabled = { enabled ->
                                scope.launch {
                                    FocusGuardStore.upsertSchedule(
                                        context,
                                        schedule.toBuilder().setIsEnabled(enabled).build()
                                    )
                                    reload()
                                }
                            },
                            onEdit = { editingForm = schedule.toFormState() },
                            onDelete = { deleteTargetId = schedule.id }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { editingForm = ScheduleFormState() },
            containerColor = AccentGreen,
            contentColor = BgColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "New schedule")
        }
    }
}

// ── Schedule list row ─────────────────────────────────────────────────────────

@Composable
private fun ScheduleRow(
    schedule: ScheduleBlock,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val appCount = schedule.packageNamesCount
    val daysSummary = daysSummary(schedule.activeDaysList)
    val timeRange = "${formatTime(schedule.startHour, schedule.startMinute)} – ${formatTime(schedule.endHour, schedule.endMinute)}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceColor)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.name,
                    color = TextPrimary,
                    fontFamily = GildaDisplay,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$appCount app${if (appCount != 1) "s" else ""} · $daysSummary",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = timeRange,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = schedule.isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(checkedTrackColor = AccentGreen)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentGreen)
                Spacer(Modifier.width(4.dp))
                Text("Edit", color = AccentGreen, fontSize = 13.sp)
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = WarnRed)
                Spacer(Modifier.width(4.dp))
                Text("Delete", color = WarnRed, fontSize = 13.sp)
            }
        }
    }
}

// ── Create / Edit form ────────────────────────────────────────────────────────

@Composable
private fun ScheduleFormView(
    form: ScheduleFormState,
    installedApps: List<AppInfo>,
    isNew: Boolean,
    onFormChange: (ScheduleFormState) -> Unit,
    onSave: (ScheduleFormState, Boolean) -> Unit,
    onBack: () -> Unit
) {
    // For editing an existing schedule, preserve its current enabled state.
    // New schedules default to enabled = true.
    var isEnabled by remember { mutableStateOf(true) }
    var showAppPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val canSave = form.name.isNotBlank() && form.activeDays.isNotEmpty()

    if (showAppPicker) {
        AppPickerSheet(
            installedApps = installedApps,
            selected = form.selectedPackages,
            onDismiss = { showAppPicker = false },
            onConfirm = { chosen ->
                onFormChange(form.copy(selectedPackages = chosen))
                showAppPicker = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            ) {
                TextButton(onClick = onBack) {
                    Text("< Back", color = AccentGreen, fontFamily = GildaDisplay, fontSize = 16.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (isNew) "New Schedule" else "Edit Schedule",
                    fontFamily = GildaDisplay,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(64.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // ── Name ──────────────────────────────────────────────────
                SectionLabel("Schedule Name")
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { onFormChange(form.copy(name = it)) },
                    placeholder = { Text("e.g. Early morning", color = TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentGreen
                    )
                )
                if (form.name.isBlank()) {
                    Text(
                        "Name is required",
                        color = WarnRed,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── Apps ──────────────────────────────────────────────────
                SectionLabel("Blocked Apps")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceColor)
                        .clickable { showAppPicker = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val count = form.selectedPackages.size
                    Text(
                        text = if (count == 0) "Tap to choose apps…"
                        else "$count app${if (count != 1) "s" else ""} selected",
                        color = if (count == 0) TextSecondary else TextPrimary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text("›", color = AccentGreen, fontSize = 18.sp)
                }

                Spacer(Modifier.height(20.dp))

                // ── Days ──────────────────────────────────────────────────
                SectionLabel("Active Days")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ALL_DAYS.forEach { day ->
                        val selected = day in form.activeDays
                        val label = DAY_LABELS[day] ?: "?"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) AccentGreen else SurfaceColor)
                                .clickable {
                                    val newDays = if (selected) form.activeDays - day
                                    else form.activeDays + day
                                    onFormChange(form.copy(activeDays = newDays))
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) BgColor else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                if (form.activeDays.isEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Select at least one day", color = WarnRed, fontSize = 11.sp)
                }

                Spacer(Modifier.height(20.dp))

                // ── Time range ────────────────────────────────────────────
                SectionLabel("Time Range")
                Text(
                    "Overnight ranges are supported (e.g. 10 PM → 6 AM).",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimeButton(
                        label = "Start",
                        hour = form.startHour,
                        minute = form.startMinute,
                        modifier = Modifier.weight(1f),
                        context = context,
                        onTimePicked = { h, m -> onFormChange(form.copy(startHour = h, startMinute = m)) }
                    )
                    TimeButton(
                        label = "End",
                        hour = form.endHour,
                        minute = form.endMinute,
                        modifier = Modifier.weight(1f),
                        context = context,
                        onTimePicked = { h, m -> onFormChange(form.copy(endHour = h, endMinute = m)) }
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── Enabled toggle (only relevant when editing) ───────────
                if (!isNew) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceColor)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Schedule enabled", color = TextPrimary, fontSize = 14.sp,
                            modifier = Modifier.weight(1f))
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = AccentGreen)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // ── Save ──────────────────────────────────────────────────
                Button(
                    onClick = { if (canSave) onSave(form, isEnabled) },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = BgColor,
                        disabledContainerColor = AccentGreen.copy(alpha = 0.25f),
                        disabledContentColor = TextSecondary
                    )
                ) {
                    Text(
                        if (isNew) "Create Schedule" else "Save Changes",
                        fontFamily = GildaDisplay,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ── App picker bottom sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(
    installedApps: List<AppInfo>,
    selected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var draft by remember { mutableStateOf(selected) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = if (searchQuery.isBlank()) installedApps
    else installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Title + search
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "Choose Apps",
                    fontFamily = GildaDisplay,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps…", color = TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentGreen
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${draft.size} selected",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val isChecked = app.packageName in draft
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(BgColor)
                            .clickable {
                                draft = if (isChecked) draft - app.packageName
                                else draft + app.packageName
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bitmap = remember(app.icon) { app.icon.toBitmapCompat() }
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            app.label,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                draft = if (it) draft + app.packageName else draft - app.packageName
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AccentGreen,
                                uncheckedColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // Confirm row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) { Text("Cancel") }

                Button(
                    onClick = { onConfirm(draft) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = BgColor
                    )
                ) {
                    Text("Confirm", fontFamily = GildaDisplay, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun TimeButton(
    label: String,
    hour: Int,
    minute: Int,
    modifier: Modifier = Modifier,
    context: android.content.Context,
    onTimePicked: (Int, Int) -> Unit
) {
    Column(modifier = modifier) {
        Text(label, color = TextSecondary, fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceColor)
                .border(1.dp, TextSecondary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .clickable {
                    TimePickerDialog(
                        context,
                        { _, h, m -> onTimePicked(h, m) },
                        hour,
                        minute,
                        false // 12-hour format
                    ).show()
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                formatTime(hour, minute),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = GildaDisplay
            )
        }
    }
}