package com.example.memogotchi.ui.page

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.ui.theme.GildaDisplay
import com.example.memogotchi.ui.theme.Comfortaa
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.ExperimentalMaterial3Api
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// ── Colors ────────────────────────────────────────────────────────────────────
private val BgColor = Color(0xFF16171C)
private val SurfaceColor = Color(0xFF1F2125)
private val AccentGreen = Color(0xFF77C59D)
private val TextPrimary = Color(0xFFE8E6F0)
private val TextSecondary = Color(0xFF888888)

// ── Data models ───────────────────────────────────────────────────────────────
data class BatteryState(
    val key: String,
    val label: String,
    val colorLow: Color,
    val colorHigh: Color,
    val value: Float? = null
)

data class DiaryEntry(
    val id: String = UUID.randomUUID().toString(),
    val dateLabel: String,
    val dayLabel: String,
    val sortKey: Long,
    val text: String,
    val categories: List<String>,
    val sliderSnapshot: List<Float>?,
    val timeLabel: String = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date()),
    val isStateLog: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis(),
    val photoUri: String? = null,
    val audioUri: String? = null,
)



enum class DiaryMode { NONE, TODAY, YESTERDAY, OTHER_DAY }

fun stateIcon(key: String, pct: Float): ImageVector = when (key) {
    "emotional" -> when {
        pct <= 20f -> Icons.Outlined.HeartBroken
        pct <= 40f -> Icons.Outlined.FavoriteBorder
        else -> Icons.Outlined.Favorite
    }

    "social" -> when {
        pct <= 20f -> Icons.Outlined.PersonOff
        pct <= 40f -> Icons.Outlined.Person
        pct <= 60f -> Icons.Outlined.People
        else -> Icons.Outlined.Groups
    }

    "physical" -> when {
        pct <= 20f -> Icons.Outlined.Bedtime
        pct <= 40f -> Icons.Outlined.DirectionsWalk
        pct <= 60f -> Icons.Outlined.DirectionsRun
        else -> Icons.Outlined.FitnessCenter
    }

    "motivation" -> when {
        pct <= 20f -> Icons.Outlined.SentimentVeryDissatisfied
        pct <= 40f -> Icons.Outlined.LocalFireDepartment
        pct <= 60f -> Icons.Outlined.LocalFireDepartment
        else -> Icons.Outlined.ElectricBolt
    }

    else -> Icons.Outlined.Circle
}

private fun entryAvgPct(snapshot: List<Float>?, currentStates: List<BatteryState>): Float {
    return if (!snapshot.isNullOrEmpty()) {
        snapshot.average().toFloat()
    } else {
        val vals = currentStates.mapNotNull { it.value }
        if (vals.isEmpty()) 50f else vals.average().toFloat()
    }
}

private fun entryBorderColor(snapshot: List<Float>?, currentStates: List<BatteryState>): Color {
    val avg = entryAvgPct(snapshot, currentStates)
    return when {
        avg >= 75f -> Color(0xFF77C59D)
        avg >= 50f -> Color(0xFFD4C44A)
        avg >= 25f -> Color(0xFFD4920A)
        else -> Color(0xFFD4537E)
    }
}

private fun sortKeyForLabel(dateLabel: String): Long {
    return when (dateLabel) {
        "Today" -> Long.MAX_VALUE
        "Yesterday" -> Long.MAX_VALUE - 1
        else -> runCatching {
            SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).parse(dateLabel)?.time ?: 0L
        }.getOrDefault(0L)
    }
}

private fun dayOfWeekForLabel(dateLabel: String): String {
    return when (dateLabel) {
        "Today" -> SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())
        "Yesterday" -> {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            SimpleDateFormat("EEEE", Locale.ENGLISH).format(cal.time)
        }

        else -> runCatching {
            val d = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).parse(dateLabel)
            SimpleDateFormat("EEEE", Locale.ENGLISH).format(d!!)
        }.getOrDefault("")
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  WELLNESS SCREEN — now just orchestration; each section is its own composable
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun WellnessScreen(
    states: MutableList<BatteryState>,
    sliderValues: MutableList<Float>,
    diaryEntries: MutableList<DiaryEntry>
) {
    var isExpanded by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    var diaryMode by remember { mutableStateOf(DiaryMode.NONE) }
    var pickedDate by remember { mutableStateOf("") }
    var editingEntry by remember { mutableStateOf<DiaryEntry?>(null) }

    val context = LocalContext.current

    var datePickerShown by remember { mutableStateOf(false) }

    var showPhotoSourceSheet by remember { mutableStateOf(false) }
    var showVoiceRecorderSheet by remember { mutableStateOf(false) }
    var pendingPhotoUri by remember { mutableStateOf<String?>(null) }   // photo taken via FAB, not yet attached to an entry
    var pendingAudioUri by remember { mutableStateOf<String?>(null) }   // audio recorded via FAB, not yet attached to an entry

    val audioRecorder = rememberDiaryAudioRecorder()
    val photoPicker = rememberDiaryPhotoPicker(onPhotoPicked = { uri ->
        pendingPhotoUri = uri
        // A photo taken from the FAB (not inside the entry editor) immediately opens
        // a new "Today" entry pre-filled with that photo, so the user can add text/tags.
        diaryMode = DiaryMode.TODAY
    })

    if (diaryMode == DiaryMode.OTHER_DAY && pickedDate.isEmpty() && !datePickerShown) {
        LaunchedEffect(Unit) {
            datePickerShown = true
            val cal = Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, y, m, d ->
                    val cal2 = Calendar.getInstance().apply { set(y, m, d) }
                    pickedDate = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).format(cal2.time)
                    datePickerShown = false
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).apply {
                setOnCancelListener { diaryMode = DiaryMode.NONE; datePickerShown = false }
                show()
            }
        }
    }

    val showEntry = diaryMode == DiaryMode.TODAY ||
            diaryMode == DiaryMode.YESTERDAY ||
            (diaryMode == DiaryMode.OTHER_DAY && pickedDate.isNotEmpty()) ||
            editingEntry != null

    if (showEntry) {
        val dateLabel = editingEntry?.dateLabel ?: when (diaryMode) {
            DiaryMode.TODAY -> "Today"
            DiaryMode.YESTERDAY -> "Yesterday"
            else -> pickedDate
        }
        DiaryEntryScreen(
            dateLabel = dateLabel,
            states = states,
            sliderValues = sliderValues,
            existingEntry = editingEntry,
            initialPhotoUri = pendingPhotoUri,
            initialAudioUri = pendingAudioUri,
            onSubmit = { text, cats, updatedSliders, finalDateLabel, photoUri, audioUri ->
                handleDiarySubmit(
                    context, states, sliderValues, diaryEntries,
                    editingEntry, text, cats, updatedSliders, finalDateLabel,
                    photoUri, audioUri
                )
                editingEntry = null; diaryMode = DiaryMode.NONE; pickedDate = ""
                pendingPhotoUri = null; pendingAudioUri = null
            },
            onBack = {
                editingEntry = null; diaryMode = DiaryMode.NONE; pickedDate = ""
                pendingPhotoUri = null; pendingAudioUri = null
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        WellnessMainContent(
            states = states,
            sliderValues = sliderValues,
            diaryEntries = diaryEntries,
            isExpanded = isExpanded,
            onExpandedChange = { isExpanded = it },
            onEntryClick = { entry -> if (!entry.isStateLog) editingEntry = entry },
            onDeleteEntry = { entry -> handleDiaryDelete(context, diaryEntries, entry) }
        )

        if (fabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { fabExpanded = false }
            )
        }

        WellnessFab(
            fabExpanded = fabExpanded,
            onFabExpandedChange = { fabExpanded = it },
            onPickMode = { mode -> diaryMode = mode },
            onVoiceClick = { showVoiceRecorderSheet = true },
            onPhotoClick = { showPhotoSourceSheet = true },
        )

        if (showPhotoSourceSheet) {
            PhotoSourceSheet(
                onDismiss = { showPhotoSourceSheet = false },
                onCameraClick = { photoPicker.launchCamera() },
                onGalleryClick = { photoPicker.launchGallery() },
            )
        }

        if (showVoiceRecorderSheet) {
            VoiceRecorderSheet(
                recorder = audioRecorder,
                onDismiss = { showVoiceRecorderSheet = false },
                onAttach = { uri ->
                    pendingAudioUri = uri
                    diaryMode = DiaryMode.TODAY   // same idea as photo: open a fresh "Today" entry with this attached
                }
            )
        }
    }
}

private fun handleDiaryDelete(
    context: android.content.Context,
    diaryEntries: MutableList<DiaryEntry>,
    entry: DiaryEntry,
) {
    diaryEntries.removeAll { it.id == entry.id }
    WellnessStore.saveDiaryEntries(context, diaryEntries)
}

// ── Submit handler extracted out of WellnessScreen's body ─────────────────────
private fun handleDiarySubmit(
    context: android.content.Context,
    states: MutableList<BatteryState>,
    sliderValues: MutableList<Float>,
    diaryEntries: MutableList<DiaryEntry>,
    editingEntry: DiaryEntry?,
    text: String,
    cats: List<String>,
    updatedSliders: List<Float>?,
    finalDateLabel: String,
    photoUri: String?,
    audioUri: String?,
) {
    updatedSliders?.forEachIndexed { i, v ->
        states[i] = states[i].copy(value = v)
        sliderValues[i] = v
    }
    val newEntry = DiaryEntry(
        id = editingEntry?.id ?: UUID.randomUUID().toString(),
        dateLabel = finalDateLabel,
        dayLabel = dayOfWeekForLabel(finalDateLabel),
        sortKey = sortKeyForLabel(finalDateLabel),
        text = text,
        categories = cats,
        sliderSnapshot = updatedSliders,
        photoUri = photoUri,
        audioUri = audioUri,
        createdAtMs = editingEntry?.createdAtMs ?: System.currentTimeMillis(),
    )

    val previousTags = editingEntry?.categories ?: emptyList()
    val newlyAddedTags = cats.filterNot { it in previousTags }
    GoalStore.incrementTagTally(context, newlyAddedTags)

    if (editingEntry != null) {
        val idx = diaryEntries.indexOfFirst { it.id == editingEntry.id }
        if (idx >= 0) diaryEntries[idx] = newEntry
    } else {
        diaryEntries.add(newEntry)
    }
    val sorted = diaryEntries.sortedWith(
        compareByDescending<DiaryEntry> { it.sortKey }.thenByDescending { it.createdAtMs }
    )
    diaryEntries.clear()
    diaryEntries.addAll(sorted)

    WellnessStore.saveDiaryEntries(context, diaryEntries)
}

// ── Auto-log handler, also extracted ───────────────────────────────────────────
private fun handleBatterySubmit(
    context: android.content.Context,
    states: MutableList<BatteryState>,
    sliderValues: List<Float>,
    diaryEntries: MutableList<DiaryEntry>
) {
    states.forEachIndexed { i, state -> states[i] = state.copy(value = sliderValues[i]) }

    val today = "Today"
    val snapshot = sliderValues.toList()
    val existingLogIdx = diaryEntries.indexOfFirst { it.dateLabel == today && it.isStateLog }
    if (existingLogIdx >= 0) {
        diaryEntries[existingLogIdx] =
            diaryEntries[existingLogIdx].copy(sliderSnapshot = snapshot)
    } else {
        diaryEntries.add(
            DiaryEntry(
                dateLabel = today,
                dayLabel = dayOfWeekForLabel(today),
                sortKey = sortKeyForLabel(today),
                text = "",
                categories = emptyList(),
                sliderSnapshot = snapshot,
                isStateLog = true,
            )
        )
        android.widget.Toast.makeText(context, "ADD CALLED, new size: ${diaryEntries.size}", android.widget.Toast.LENGTH_LONG).show()
    }
    val sorted = diaryEntries.sortedWith(
        compareByDescending<DiaryEntry> { it.sortKey }.thenByDescending { it.createdAtMs }
    )
    diaryEntries.clear()
    diaryEntries.addAll(sorted)

    WellnessStore.saveSliders(context, snapshot)
    WellnessStore.saveDiaryEntries(context, diaryEntries)
}

// ════════════════════════════════════════════════════════════════════════════
//  MAIN SCROLLABLE CONTENT (battery card + states + diary list)
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun WellnessMainContent(
    states: MutableList<BatteryState>,
    sliderValues: MutableList<Float>,
    diaryEntries: MutableList<DiaryEntry>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onEntryClick: (DiaryEntry) -> Unit,
    onDeleteEntry: (DiaryEntry) -> Unit,
) {
    val loggedCount = states.count { it.value != null }
    val batteryPct = if (loggedCount == 0) null
    else states.mapNotNull { it.value }.average().roundToInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "Wellness", fontFamily = GildaDisplay, fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold, color = TextPrimary
        )
        Spacer(Modifier.height(16.dp))

        OverallBatteryCard(batteryPct = batteryPct, loggedCount = loggedCount)

        Spacer(Modifier.height(8.dp))

        BatteryStatesCard(
            states = states,
            sliderValues = sliderValues,
            diaryEntries = diaryEntries,   // ← now passed straight through
            isExpanded = isExpanded,
            onExpandedChange = onExpandedChange,
        )

        Spacer(Modifier.height(20.dp))

        Text(
            "Diary", fontFamily = GildaDisplay, fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold, color = TextPrimary
        )
        Spacer(Modifier.height(10.dp))

        DiaryList(
            diaryEntries = diaryEntries,
            states = states,
            onEntryClick = onEntryClick,
            onDelete = onDeleteEntry,   // new param threaded down from WellnessScreen
        )

        Spacer(Modifier.height(120.dp))
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  OVERALL BATTERY CARD
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun OverallBatteryCard(batteryPct: Int?, loggedCount: Int) {
    val bgFillColor = if (batteryPct != null)
        lerp(Color(0xFF3D1A1A), Color(0xFF1A3D2E), batteryPct / 100f)
    else SurfaceColor

    val infiniteTransition = rememberInfiniteTransition(label = "battery_gradient")
    val shinePosition by infiniteTransition.animateFloat(
        initialValue = -0.3f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shine_position"
    )
    val baseDark = lerp(bgFillColor, Color.Black, 0.15f)
    val baseMid = lerp(bgFillColor, Color.White, 0.08f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
    ) {
        if (batteryPct != null) {
            Box(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(batteryPct / 100f)
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(listOf(baseDark, baseMid, baseDark))
                            )
                    )
                    val shineAlpha = when {
                        shinePosition < -0.2f || shinePosition > 1.2f -> 0f
                        shinePosition < 0f -> (shinePosition + 0.2f) / 0.2f
                        shinePosition > 1f -> 1f - (shinePosition - 1f) / 0.2f
                        else -> 1f
                    }
                    if (shineAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colorStops = arrayOf(
                                            0f to Color.Transparent,
                                            maxOf(
                                                0f,
                                                shinePosition - 0.2f
                                            ) to Color.Transparent,
                                            shinePosition.coerceIn(
                                                0f,
                                                1f
                                            ) to Color.White.copy(alpha = 0.18f * shineAlpha),
                                            minOf(
                                                1f,
                                                shinePosition + 0.2f
                                            ) to Color.Transparent,
                                            1f to Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Overall battery", fontSize = 20.sp, color = TextPrimary, fontFamily = GildaDisplay)
                Spacer(Modifier.height(1.dp))
                Text(
                    text = if (loggedCount < 4) "$loggedCount / 4 logged" else "All batteries logged",
                    fontSize = 12.sp, color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(BgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (batteryPct != null) "$batteryPct%" else "—",
                    fontSize = if (batteryPct != null) 18.sp else 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (batteryPct != null) AccentGreen else TextSecondary
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  BATTERY STATES CARD (icons row + expandable sliders + submit button)
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun BatteryStatesCard(
    states: MutableList<BatteryState>,
    sliderValues: MutableList<Float>,
    diaryEntries: MutableList<DiaryEntry>,   // ← now a direct parameter
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                states.forEachIndexed { i, state ->
                    val pct = state.value ?: 0f
                    val iconColor = if (state.value != null)
                        lerp(state.colorLow, state.colorHigh, pct / 100f)
                    else TextSecondary
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (!isExpanded) {
                                    states.forEachIndexed { j, s ->
                                        sliderValues[j] = s.value ?: 50f
                                    }
                                    onExpandedChange(true)
                                }
                            }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = stateIcon(
                                state.key,
                                if (state.value != null) pct else 50f
                            ),
                            contentDescription = state.label,
                            tint = iconColor, modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            state.label, fontSize = 10.sp,
                            color = if (isExpanded) state.colorHigh else TextSecondary
                        )
                        if (state.value != null) {
                            Text(
                                "${state.value.roundToInt()}%",
                                fontSize = 9.sp,
                                color = iconColor
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ExpandedSliders(states = states, sliderValues = sliderValues)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isExpanded) AccentGreen else SurfaceColor)
                        .clickable {
                            if (isExpanded) {
                                handleBatterySubmit(
                                    context,
                                    states,
                                    sliderValues,
                                    diaryEntries   // ← direct param, no ambient lookup
                                )
                                onExpandedChange(false)
                            } else {
                                states.forEachIndexed { i, state ->
                                    sliderValues[i] = state.value ?: 50f
                                }
                                onExpandedChange(true)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.Check else Icons.Outlined.Add,
                        contentDescription = if (isExpanded) "Submit" else "Log",
                        tint = if (isExpanded) Color.White else AccentGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun ExpandedSliders(
    states: MutableList<BatteryState>,
    sliderValues: MutableList<Float>
) {
    val trackHeight = 180.dp
    val trackHeightPx = with(LocalDensity.current) { trackHeight.toPx() }
    Column {
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            states.forEachIndexed { i, state ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(levelLabel(sliderValues[i]), fontSize = 10.sp, color = state.colorHigh)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${sliderValues[i].roundToInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    VerticalSlider(
                        stateKey = state.key, index = i, state = state,
                        value = sliderValues[i], trackHeight = trackHeight,
                        trackHeightPx = trackHeightPx, onValueChange = { sliderValues[i] = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(state.label, fontSize = 10.sp, color = TextSecondary)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  DIARY LIST + CARD
// ════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryList(
    diaryEntries: List<DiaryEntry>,
    states: List<BatteryState>,
    onEntryClick: (DiaryEntry) -> Unit,
    onDelete: (DiaryEntry) -> Unit,   // ← NEW
) {
    if (diaryEntries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.Book,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("No entries yet", fontSize = 13.sp, color = TextSecondary)
                Text(
                    "Tap the pen icon to write your first entry",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    } else {
        diaryEntries.forEach { entry ->
            key(entry.id) {
                SwipeableDiaryEntry(
                    entry = entry,
                    states = states,
                    onClick = { onEntryClick(entry) },
                    onDelete = { onDelete(entry) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableDiaryEntry(
    entry: DiaryEntry,
    states: List<BatteryState>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFD4537E)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .size(22.dp)
                )
            }
        }
    ) {
        DiaryEntryCard(entry = entry, states = states, onClick = onClick)
    }
}

@Composable
private fun DiaryEntryCard(
    entry: DiaryEntry,
    states: List<BatteryState>,
    onClick: () -> Unit
) {
    val borderColor = entryBorderColor(entry.sliderSnapshot, states)
    val avgPct = entryAvgPct(entry.sliderSnapshot, states)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.5f),
            )
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.dayLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = borderColor,
                    fontFamily = GildaDisplay,
                )
                Text(text = "  ·  ${entry.dateLabel}", fontSize = 12.sp, color = TextSecondary)
                if (entry.isStateLog) {
                    Spacer(Modifier.weight(1f))
                    Text("Battery log", fontSize = 10.sp, color = TextSecondary)
                }


            }

            entry.photoUri?.let { uri ->
                Spacer(Modifier.height(10.dp))
                DiaryPhotoThumbnail(photoUri = uri)
            }
            entry.audioUri?.let { uri ->
                Spacer(Modifier.height(10.dp))
                DiaryAudioPlayer(audioUri = uri)
            }

            if (entry.categories.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.categories.take(4).forEach { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .border(
                                    1.dp,
                                    borderColor.copy(alpha = 0.4f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(cat, fontSize = 10.sp, color = borderColor.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            if (entry.sliderSnapshot != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    states.forEachIndexed { i, state ->
                        val v = entry.sliderSnapshot.getOrNull(i) ?: 50f
                        Icon(
                            imageVector = stateIcon(state.key, v),
                            contentDescription = state.label,
                            tint = lerp(state.colorLow, state.colorHigh, v / 100f),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 6.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${avgPct.roundToInt()}%", fontSize = 10.sp, color = borderColor)
                }
                Spacer(Modifier.height(6.dp))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF13111A))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((avgPct / 100f).coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(borderColor)
                )
            }

            if (entry.text.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(entry.text, fontSize = 13.sp, color = TextPrimary, lineHeight = 20.sp)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  ARC FAB
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun BoxScope.WellnessFab(
    fabExpanded: Boolean,
    onFabExpandedChange: (Boolean) -> Unit,
    onPickMode: (DiaryMode) -> Unit,
    onVoiceClick: () -> Unit,
    onPhotoClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 20.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val arcItems = listOf(
            Triple(DiaryMode.YESTERDAY, Icons.Outlined.History, "Yesterday"),
            Triple(DiaryMode.TODAY, Icons.Outlined.Today, "Today"),
            Triple(DiaryMode.OTHER_DAY, Icons.Outlined.CalendarMonth, "Other day"),
        )
        val arcOffsets = listOf(
            Pair(-90.dp, -80.dp),
            Pair(0.dp, -110.dp),
            Pair(90.dp, -80.dp),
        )

        arcItems.forEachIndexed { i, (mode, icon, label) ->
            ArcFabItem(
                index = i,
                icon = icon,
                label = label,
                expanded = fabExpanded,
                offset = arcOffsets[i],
                onClick = { onFabExpandedChange(false); onPickMode(mode) }
            )
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SurfaceColor)
                    .border(1.dp, TextSecondary.copy(alpha = 0.3f), CircleShape)
                    .clickable { onVoiceClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Mic,
                    contentDescription = "Voice",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            val rotation by animateFloatAsState(
                targetValue = if (fabExpanded) 135f else 0f,
                animationSpec = tween(200), label = "write_rotation"
            )
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(AccentGreen)
                    .clickable { onFabExpandedChange(!fabExpanded) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Write entry",
                    tint = BgColor,
                    modifier = Modifier
                        .size(26.dp)
                        .rotate(rotation)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SurfaceColor)
                    .border(1.dp, TextSecondary.copy(alpha = 0.3f), CircleShape)
                    .clickable { onPhotoClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.PhotoCamera,
                    contentDescription = "Photo",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ArcFabItem(
    index: Int,
    icon: ImageVector,
    label: String,
    expanded: Boolean,
    offset: Pair<Dp, Dp>,
    onClick: () -> Unit
) {
    val transition = updateTransition(expanded, label = "arc_$index")
    val offsetX by transition.animateDp(
        transitionSpec = {
            if (targetState) spring(
                dampingRatio = 0.6f,
                stiffness = 220f
            ) else tween(150)
        },
        label = "arcX_$index"
    ) { if (it) offset.first else 0.dp }
    val offsetY by transition.animateDp(
        transitionSpec = {
            if (targetState) spring(
                dampingRatio = 0.6f,
                stiffness = 220f
            ) else tween(150)
        },
        label = "arcY_$index"
    ) { if (it) offset.second else 0.dp }
    val alpha by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 180 + index * 40 else 100) },
        label = "arcAlpha_$index"
    ) { if (it) 1f else 0f }
    val scale by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 180 + index * 40 else 100) },
        label = "arcScale_$index"
    ) { if (it) 1f else 0.5f }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .alpha(alpha)
            .scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SurfaceColor)
                .border(1.dp, AccentGreen.copy(alpha = 0.4f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = AccentGreen,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 9.sp, color = TextSecondary)
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  DIARY ENTRY SCREEN  (create + edit) — unchanged from before
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun DiaryEntryScreen(
    dateLabel: String,
    states: List<BatteryState>,
    sliderValues: List<Float>,
    existingEntry: DiaryEntry? = null,
    initialPhotoUri: String? = null,
    initialAudioUri: String? = null,
    onSubmit: (String, List<String>, List<Float>?, String, String?, String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val localSliders = remember {
        mutableStateListOf(
            *(existingEntry?.sliderSnapshot?.toTypedArray() ?: sliderValues.toTypedArray())
        )
    }
    var sliderOpen by remember { mutableStateOf(false) }
    val selectedCats = remember {
        mutableStateListOf<String>().also {
            it.addAll(
                existingEntry?.categories ?: emptyList()
            )
        }
    }
    var entryText by remember { mutableStateOf(existingEntry?.text ?: "") }
    var showError by remember { mutableStateOf(false) }

    var currentDateLabel by remember { mutableStateOf(dateLabel) }

    var photoUri by remember { mutableStateOf(existingEntry?.photoUri ?: initialPhotoUri) }
    var audioUri by remember { mutableStateOf(existingEntry?.audioUri ?: initialAudioUri) }
    var showPhotoSourceSheetInline by remember { mutableStateOf(false) }
    var showVoiceRecorderSheetInline by remember { mutableStateOf(false) }
    val inlineRecorder = rememberDiaryAudioRecorder()
    val inlinePhotoPicker = rememberDiaryPhotoPicker(onPhotoPicked = { uri -> photoUri = uri })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .verticalScroll(rememberScrollState())
    ) {
        DiaryEntryTopBar(
            currentDateLabel = currentDateLabel,
            onDateChange = { currentDateLabel = it },
            entryText = entryText,
            isUpdate = existingEntry != null,
            onBack = onBack,
            onSubmit = {
                if (entryText.isNotBlank() || photoUri != null || audioUri != null) onSubmit(
                    entryText,
                    selectedCats.toList(),
                    localSliders.toList(),
                    currentDateLabel,
                    photoUri,
                    audioUri
                )
                else showError = true
            }
        )

        if (showError) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3D1A1A))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "Please write something, add a photo, or record a voice note before submitting.",
                    fontSize = 12.sp,
                    color = Color(0xFFD4537E)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        DiaryFeelingPicker(
            states = states,
            localSliders = localSliders,
            sliderOpen = sliderOpen,
            onToggleOpen = { sliderOpen = !sliderOpen }
        )

        Spacer(Modifier.height(12.dp))

        DiaryPhotoPreview(photoUri = photoUri, onRemove = { photoUri = null })
        if (photoUri != null) Spacer(Modifier.height(12.dp))

        audioUri?.let { uri ->
            Box(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                DiaryAudioPlayer(audioUri = uri, onRemove = { audioUri = null })
            }
            Spacer(Modifier.height(12.dp))
        }

        if (photoUri == null && audioUri == null) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (photoUri == null) {
                    AttachButton(
                        icon = Icons.Outlined.PhotoCamera, label = "Add photo",
                        modifier = Modifier.weight(1f),
                        onClick = { showPhotoSourceSheetInline = true }
                    )
                }
                if (audioUri == null) {
                    AttachButton(
                        icon = Icons.Outlined.Mic, label = "Add voice",
                        modifier = Modifier.weight(1f),
                        onClick = { showVoiceRecorderSheetInline = true }
                    )
                }
            }
            if (photoUri == null || audioUri == null) Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "What's this about?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(Modifier.height(12.dp))
                GroupedTagPicker(
                    selectedTags = selectedCats,
                    onToggleTag = { cat ->
                        if (cat in selectedCats) selectedCats.remove(cat) else selectedCats.add(
                            cat
                        )
                    },
                    accentColor = AccentGreen,
                    textSecondaryColor = TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "Write your entry",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(Modifier.height(10.dp))
                TextField(
                    value = entryText,
                    onValueChange = { entryText = it; if (it.isNotBlank()) showError = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),
                    placeholder = {
                        Text(
                            "What's on your mind?",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 22.sp
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (entryText.isNotBlank() || photoUri != null || audioUri != null) AccentGreen else Color(0xFF2A2A30))
                .clickable {
                    if (entryText.isNotBlank() || photoUri != null || audioUri != null) onSubmit(
                        entryText,
                        selectedCats.toList(),
                        localSliders.toList(),
                        currentDateLabel,
                        photoUri,
                        audioUri
                    )
                    else showError = true
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (existingEntry != null) "Update entry" else "Save entry",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = if (entryText.isNotBlank() || photoUri != null || audioUri != null) BgColor else TextSecondary
            )
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showPhotoSourceSheetInline) {
        PhotoSourceSheet(
            onDismiss = { showPhotoSourceSheetInline = false },
            onCameraClick = { inlinePhotoPicker.launchCamera() },
            onGalleryClick = { inlinePhotoPicker.launchGallery() },
        )
    }
    if (showVoiceRecorderSheetInline) {
        VoiceRecorderSheet(
            recorder = inlineRecorder,
            onDismiss = { showVoiceRecorderSheetInline = false },
            onAttach = { uri -> audioUri = uri }
        )
    }
}

@Composable
private fun AttachButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1F2125))
            .border(1.dp, Color(0xFF888888).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = Color(0xFF77C59D), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = Color(0xFFE8E6F0))
    }
}

@Composable
private fun DiaryEntryTopBar(
    currentDateLabel: String,
    onDateChange: (String) -> Unit,
    entryText: String,
    isUpdate: Boolean,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                Icons.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val cal2 = Calendar.getInstance().apply { set(y, m, d) }
                                val todayCal = Calendar.getInstance()
                                val yesterdayCal = Calendar.getInstance()
                                    .apply { add(Calendar.DAY_OF_YEAR, -1) }
                                val newLabel = when {
                                    cal2.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                                            cal2.get(Calendar.DAY_OF_YEAR) == todayCal.get(
                                        Calendar.DAY_OF_YEAR
                                    ) -> "Today"

                                    cal2.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                                            cal2.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(
                                        Calendar.DAY_OF_YEAR
                                    ) -> "Yesterday"

                                    else -> SimpleDateFormat(
                                        "MMMM d, yyyy",
                                        Locale.ENGLISH
                                    ).format(cal2.time)
                                }
                                onDateChange(newLabel)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "${dayOfWeekForLabel(currentDateLabel)}, $currentDateLabel",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Change date",
                    tint = TextSecondary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(10.dp))
                .background(if (entryText.isNotBlank() ) AccentGreen else Color(0xFF2A2A30))
                .clickable(onClick = onSubmit)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                if (isUpdate) "Update" else "Submit",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = if (entryText.isNotBlank() ) BgColor else TextSecondary
            )
        }
    }
}

@Composable
private fun DiaryFeelingPicker(
    states: List<BatteryState>,
    localSliders: MutableList<Float>,
    sliderOpen: Boolean,
    onToggleOpen: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "How are you feeling?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text("Optional", fontSize = 11.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                states.forEachIndexed { i, state ->
                    val pct = localSliders[i]
                    val iconColor = lerp(state.colorLow, state.colorHigh, pct / 100f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onToggleOpen() }
                            .padding(8.dp)
                    ) {
                        Icon(
                            stateIcon(state.key, pct),
                            contentDescription = state.label,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(state.label, fontSize = 10.sp, color = TextSecondary)
                        Text("${pct.roundToInt()}%", fontSize = 9.sp, color = iconColor)
                    }
                }
            }
            AnimatedVisibility(
                visible = sliderOpen,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val trackHeight = 160.dp
                val trackHeightPx = with(LocalDensity.current) { trackHeight.toPx() }
                Column {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        states.forEachIndexed { i, state ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    levelLabel(localSliders[i]),
                                    fontSize = 10.sp,
                                    color = state.colorHigh
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${localSliders[i].roundToInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                VerticalSlider(
                                    stateKey = state.key,
                                    index = i,
                                    state = state,
                                    value = localSliders[i],
                                    trackHeight = trackHeight,
                                    trackHeightPx = trackHeightPx,
                                    onValueChange = { localSliders[i] = it }
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(state.label, fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  SHARED VERTICAL SLIDER
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun VerticalSlider(
    stateKey: String,
    index: Int,
    state: BatteryState,
    value: Float,
    trackHeight: Dp,
    trackHeightPx: Float,
    onValueChange: (Float) -> Unit
) {
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    Box(
        modifier = Modifier
            .width(40.dp)
            .height(trackHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF13111A))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    val delta = -(dragAmount / trackHeightPx) * 100f
                    currentOnValueChange((currentValue + delta).coerceIn(0f, 100f))
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(value / 100f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    lerp(
                        state.colorLow,
                        state.colorHigh,
                        value / 100f
                    ).copy(alpha = 0.5f)
                )
        )
        Box(
            modifier = Modifier
                .padding(bottom = (trackHeight * (value / 100f) - 20.dp).coerceAtLeast(0.dp))
                .size(34.dp)
                .clip(CircleShape)
                .background(lerp(state.colorLow, state.colorHigh, value / 100f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                stateIcon(stateKey, 50f), contentDescription = null,
                tint = Color.White, modifier = Modifier.size(15.dp)
            )
        }
    }
}

private fun levelLabel(value: Float): String = when {
    value >= 80 -> "Thriving"
    value >= 60 -> "Good"
    value >= 40 -> "Okay"
    value >= 20 -> "Low"
    else -> "Drained"
}
