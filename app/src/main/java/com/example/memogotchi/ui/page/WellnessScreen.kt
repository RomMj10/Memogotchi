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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// ── Colors ────────────────────────────────────────────────────────────────────
private val BgColor       = Color(0xFF16171C)
private val SurfaceColor  = Color(0xFF1F2125)
private val AccentGreen   = Color(0xFF77C59D)
private val TextPrimary   = Color(0xFFE8E6F0)
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
    val dayLabel: String,        // e.g. "Monday"
    val sortKey: Long,           // epoch ms for sorting; today=Long.MAX_VALUE-offset
    val text: String,
    val categories: List<String>,
    val sliderSnapshot: List<Float>?,
    val timeLabel: String = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date()),
)

enum class DiaryMode { NONE, TODAY, YESTERDAY, OTHER_DAY }

fun stateIcon(key: String, pct: Float): ImageVector = when (key) {
    "emotional" -> when {
        pct <= 20f -> Icons.Outlined.HeartBroken
        pct <= 40f -> Icons.Outlined.FavoriteBorder
        else       -> Icons.Outlined.Favorite
    }
    "social" -> when {
        pct <= 20f -> Icons.Outlined.PersonOff
        pct <= 40f -> Icons.Outlined.Person
        pct <= 60f -> Icons.Outlined.People
        else       -> Icons.Outlined.Groups
    }
    "physical" -> when {
        pct <= 20f -> Icons.Outlined.Bedtime
        pct <= 40f -> Icons.Outlined.DirectionsWalk
        pct <= 60f -> Icons.Outlined.DirectionsRun
        else       -> Icons.Outlined.FitnessCenter
    }
    "motivation" -> when {
        pct <= 20f -> Icons.Outlined.SentimentVeryDissatisfied
        pct <= 40f -> Icons.Outlined.LocalFireDepartment
        pct <= 60f -> Icons.Outlined.LocalFireDepartment
        else       -> Icons.Outlined.ElectricBolt
    }
    else -> Icons.Outlined.Circle
}

// Diary tags now use the shared `tagCategories` grouping from TagCategories.kt

// ── Entry border color from sliderSnapshot or fallback to current states ──────
private fun entryBorderColor(
    snapshot: List<Float>?,
    currentStates: List<BatteryState>
): Color {
    val avg = if (!snapshot.isNullOrEmpty()) {
        snapshot.average().toFloat()
    } else {
        val vals = currentStates.mapNotNull { it.value }
        if (vals.isEmpty()) 50f else vals.average().toFloat()
    }
    return when {
        avg >= 75f -> Color(0xFF77C59D)   // green
        avg >= 50f -> Color(0xFFD4C44A)   // yellow
        avg >= 25f -> Color(0xFFD4920A)   // orange
        else       -> Color(0xFFD4537E)   // red
    }
}

// ── Sort key helpers ──────────────────────────────────────────────────────────
private fun sortKeyForLabel(dateLabel: String): Long {
    return when (dateLabel) {
        "Today"     -> Long.MAX_VALUE
        "Yesterday" -> Long.MAX_VALUE - 1
        else        -> runCatching {
            SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).parse(dateLabel)?.time ?: 0L
        }.getOrDefault(0L)
    }
}

private fun dayOfWeekForLabel(dateLabel: String): String {
    return when (dateLabel) {
        "Today"     -> SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())
        "Yesterday" -> {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            SimpleDateFormat("EEEE", Locale.ENGLISH).format(cal.time)
        }
        else        -> runCatching {
            val d = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).parse(dateLabel)
            SimpleDateFormat("EEEE", Locale.ENGLISH).format(d!!)
        }.getOrDefault("")
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  WELLNESS SCREEN
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun WellnessScreen(
    states: MutableList<BatteryState>,
    sliderValues: MutableList<Float>,
    diaryEntries: MutableList<DiaryEntry>
) {
    var isExpanded  by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    var diaryMode   by remember { mutableStateOf(DiaryMode.NONE) }
    var pickedDate  by remember { mutableStateOf("") }
    var editingEntry by remember { mutableStateOf<DiaryEntry?>(null) }

    val loggedCount = states.count { it.value != null }
    val batteryPct  = if (loggedCount == 0) null
    else states.mapNotNull { it.value }.average().roundToInt()

    val bgFillColor = if (batteryPct != null)
        lerp(Color(0xFF3D1A1A), Color(0xFF1A3D2E), batteryPct / 100f)
    else SurfaceColor

    val context = LocalContext.current

    var datePickerShown by remember { mutableStateOf(false) }
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
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).apply {
                setOnCancelListener {
                    diaryMode = DiaryMode.NONE
                    datePickerShown = false
                }
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
            DiaryMode.TODAY     -> "Today"
            DiaryMode.YESTERDAY -> "Yesterday"
            else                -> pickedDate
        }
        DiaryEntryScreen(
            dateLabel    = dateLabel,
            states       = states,
            sliderValues = sliderValues,
            existingEntry = editingEntry,
            onSubmit     = { text, cats, updatedSliders, finalDateLabel ->
                updatedSliders?.forEachIndexed { i, v ->
                    states[i] = states[i].copy(value = v)
                    sliderValues[i] = v
                }
                val newEntry = DiaryEntry(
                    id            = editingEntry?.id ?: UUID.randomUUID().toString(),
                    dateLabel     = finalDateLabel,
                    dayLabel      = dayOfWeekForLabel(finalDateLabel),
                    sortKey       = sortKeyForLabel(finalDateLabel),
                    text          = text,
                    categories    = cats,
                    sliderSnapshot = updatedSliders,
                )

                // Feed the shared personality tag tally — only count tags that are
                // new compared to what this entry had before (avoids double-counting on edits)
                val previousTags = editingEntry?.categories ?: emptyList()
                val newlyAddedTags = cats.filterNot { it in previousTags }
                GoalStore.incrementTagTally(context, newlyAddedTags)

                if (editingEntry != null) {
                    val idx = diaryEntries.indexOfFirst { it.id == editingEntry!!.id }
                    if (idx >= 0) diaryEntries[idx] = newEntry
                } else {
                    diaryEntries.add(newEntry)
                }
                // Sort: highest sortKey first (today on top)
                val sorted = diaryEntries.sortedByDescending { it.sortKey }
                diaryEntries.clear()
                diaryEntries.addAll(sorted)

                editingEntry = null
                diaryMode    = DiaryMode.NONE
                pickedDate   = ""
            },
            onBack = {
                editingEntry = null
                diaryMode    = DiaryMode.NONE
                pickedDate   = ""
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Text("Wellness", fontFamily = GildaDisplay, fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(16.dp))

            // ── Overall battery card ──────────────────────────────────────
            val infiniteTransition = rememberInfiniteTransition(label = "battery_gradient")
            val shinePosition by infiniteTransition.animateFloat(
                initialValue = -0.3f, targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ), label = "shine_position"
            )
            val baseDark = lerp(bgFillColor, Color.Black, 0.15f)
            val baseMid  = lerp(bgFillColor, Color.White, 0.08f)

            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SurfaceColor)
            ) {
                if (batteryPct != null) {
                    Box(modifier = Modifier.matchParentSize()) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(batteryPct / 100f)) {
                            Box(modifier = Modifier.matchParentSize().background(
                                Brush.horizontalGradient(listOf(baseDark, baseMid, baseDark))
                            ))
                            val shineAlpha = when {
                                shinePosition < -0.2f || shinePosition > 1.2f -> 0f
                                shinePosition < 0f -> (shinePosition + 0.2f) / 0.2f
                                shinePosition > 1f -> 1f - (shinePosition - 1f) / 0.2f
                                else -> 1f
                            }
                            if (shineAlpha > 0f) {
                                Box(modifier = Modifier.matchParentSize().background(
                                    Brush.horizontalGradient(colorStops = arrayOf(
                                        0f to Color.Transparent,
                                        maxOf(0f, shinePosition - 0.2f) to Color.Transparent,
                                        shinePosition.coerceIn(0f, 1f) to Color.White.copy(alpha = 0.18f * shineAlpha),
                                        minOf(1f, shinePosition + 0.2f) to Color.Transparent,
                                        1f to Color.Transparent
                                    ))
                                ))
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Overall battery", fontSize = 20.sp, color = TextPrimary)
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = if (loggedCount < 4) "$loggedCount / 4 logged" else "All batteries logged",
                            fontSize = 12.sp, color = TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier.size(58.dp).clip(CircleShape).background(BgColor),
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

            Spacer(Modifier.height(8.dp))

            // ── States section ────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(SurfaceColor).padding(16.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        states.forEachIndexed { i, state ->
                            val pct = state.value ?: 0f
                            val iconColor = if (state.value != null)
                                lerp(state.colorLow, state.colorHigh, pct / 100f)
                            else TextSecondary
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (!isExpanded) {
                                            states.forEachIndexed { j, s -> sliderValues[j] = s.value ?: 50f }
                                            isExpanded = true
                                        }
                                    }.padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = stateIcon(state.key, if (state.value != null) pct else 50f),
                                    contentDescription = state.label,
                                    tint = iconColor, modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(state.label, fontSize = 10.sp,
                                    color = if (isExpanded) state.colorHigh else TextSecondary)
                                if (state.value != null) {
                                    Text("${state.value.roundToInt()}%", fontSize = 9.sp, color = iconColor)
                                }
                            }
                        }
                    }
                    AnimatedVisibility(visible = isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        val trackHeight   = 180.dp
                        val trackHeightPx = with(LocalDensity.current) { trackHeight.toPx() }
                        Column {
                            Spacer(Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                                states.forEachIndexed { i, state ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text(levelLabel(sliderValues[i]), fontSize = 10.sp, color = state.colorHigh)
                                        Spacer(Modifier.height(4.dp))
                                        Text("${sliderValues[i].roundToInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(if (isExpanded) AccentGreen else SurfaceColor)
                                .clickable {
                                    if (isExpanded) {
                                        states.forEachIndexed { i, state -> states[i] = state.copy(value = sliderValues[i]) }
                                        isExpanded = false
                                    } else {
                                        states.forEachIndexed { i, state -> sliderValues[i] = state.value ?: 50f }
                                        isExpanded = true
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

            Spacer(Modifier.height(20.dp))

            Text("Diary", fontFamily = GildaDisplay, fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(10.dp))

            // ── Diary entries ─────────────────────────────────────────────
            if (diaryEntries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(SurfaceColor).padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Book, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No entries yet", fontSize = 13.sp, color = TextSecondary)
                        Text("Tap the pen icon to write your first entry", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            } else {
                diaryEntries.forEach { entry ->
                    val borderColor = entryBorderColor(entry.sliderSnapshot, states)
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
                            .clickable { editingEntry = entry }
                            .padding(16.dp)
                    ) {
                        Column {
                            // DAY, DATE row
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = entry.dayLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = borderColor,
                                    fontFamily = GildaDisplay,
                                )
                                Text(
                                    text = "  ·  ${entry.dateLabel}",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                )
                            }

                            // Tags below the date
                            if (entry.categories.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    entry.categories.take(4).forEach { cat ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(cat, fontSize = 10.sp, color = borderColor.copy(alpha = 0.8f))
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Text(entry.text, fontSize = 13.sp, color = TextPrimary, lineHeight = 20.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(120.dp))
        }

        // ── Scrim ─────────────────────────────────────────────────────────
        if (fabExpanded) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { fabExpanded = false }
            )
        }

        // ── Arc FAB row: Voice | Write | Photo ────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            // Arc items: Today, Yesterday, Other Day — fan upward from center
            val arcItems = listOf(
                Triple(DiaryMode.TODAY,     Icons.Outlined.Today,         "Today"),
                Triple(DiaryMode.YESTERDAY, Icons.Outlined.History,       "Yesterday"),
                Triple(DiaryMode.OTHER_DAY, Icons.Outlined.CalendarMonth, "Other day"),
            )
            // Arc offsets: fan left-center-right upward
            val arcOffsets = listOf(
                Pair(-90.dp, -80.dp),
                Pair(0.dp, -110.dp),
                Pair(90.dp, -80.dp),
            )

            arcItems.forEachIndexed { i, (mode, icon, label) ->
                val transition = updateTransition(fabExpanded, label = "arc_$i")
                val offsetX by transition.animateDp(
                    transitionSpec = { if (targetState) spring(dampingRatio = 0.6f, stiffness = 220f) else tween(150) },
                    label = "arcX_$i"
                ) { expanded -> if (expanded) arcOffsets[i].first else 0.dp }
                val offsetY by transition.animateDp(
                    transitionSpec = { if (targetState) spring(dampingRatio = 0.6f, stiffness = 220f) else tween(150) },
                    label = "arcY_$i"
                ) { expanded -> if (expanded) arcOffsets[i].second else 0.dp }
                val alpha by transition.animateFloat(
                    transitionSpec = { tween(if (targetState) 180 + i * 40 else 100) },
                    label = "arcAlpha_$i"
                ) { if (it) 1f else 0f }
                val scale by transition.animateFloat(
                    transitionSpec = { tween(if (targetState) 180 + i * 40 else 100) },
                    label = "arcScale_$i"
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
                            .clickable {
                                fabExpanded = false
                                diaryMode   = mode
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = label, tint = AccentGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(label, fontSize = 9.sp, color = TextSecondary)
                }
            }

            // ── Bottom bar: Voice | Write (center FAB) | Photo ────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Voice FAB (left)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceColor)
                        .border(1.dp, TextSecondary.copy(alpha = 0.3f), CircleShape)
                        .clickable { /* voice — future */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Mic, contentDescription = "Voice", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }

                // Write FAB (center, larger)
                val rotation by animateFloatAsState(
                    targetValue = if (fabExpanded) 45f else 0f,
                    animationSpec = tween(200), label = "write_rotation"
                )
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(AccentGreen)
                        .clickable { fabExpanded = !fabExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Write entry",
                        tint = BgColor,
                        modifier = Modifier.size(26.dp).rotate(rotation)
                    )
                }

                // Photo FAB (right)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceColor)
                        .border(1.dp, TextSecondary.copy(alpha = 0.3f), CircleShape)
                        .clickable { /* photo — future */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Photo", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  DIARY ENTRY SCREEN  (create + edit)
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun DiaryEntryScreen(
    dateLabel: String,
    states: List<BatteryState>,
    sliderValues: List<Float>,
    existingEntry: DiaryEntry? = null,
    onSubmit: (String, List<String>, List<Float>?, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val localSliders  = remember { mutableStateListOf(*(existingEntry?.sliderSnapshot?.toTypedArray() ?: sliderValues.toTypedArray())) }
    var sliderOpen    by remember { mutableStateOf(false) }
    val selectedCats  = remember { mutableStateListOf<String>().also { it.addAll(existingEntry?.categories ?: emptyList()) } }
    var entryText     by remember { mutableStateOf(existingEntry?.text ?: "") }
    var showError     by remember { mutableStateOf(false) }

    // Editable date
    var currentDateLabel by remember { mutableStateOf(dateLabel) }
    var datePickerShown  by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(BgColor).verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Box(
            modifier = Modifier.fillMaxWidth().background(SurfaceColor)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(22.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                // Tappable date — opens date picker for "Other day" or to change date
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
                                    val today = Calendar.getInstance()
                                    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                                    currentDateLabel = when {
                                        cal2.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                                cal2.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
                                        cal2.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                                                cal2.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
                                        else -> SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).format(cal2.time)
                                    }
                                },
                                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${dayOfWeekForLabel(currentDateLabel)}, $currentDateLabel",
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Outlined.Edit, contentDescription = "Change date", tint = TextSecondary, modifier = Modifier.size(13.dp))
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (entryText.isNotBlank()) AccentGreen else Color(0xFF2A2A30))
                    .clickable {
                        if (entryText.isNotBlank()) onSubmit(entryText, selectedCats.toList(), localSliders.toList(), currentDateLabel)
                        else showError = true
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    if (existingEntry != null) "Update" else "Submit",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = if (entryText.isNotBlank()) BgColor else TextSecondary
                )
            }
        }

        if (showError) {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF3D1A1A)).padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Please write something before submitting.", fontSize = 12.sp, color = Color(0xFFD4537E))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Sliders
        Box(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)).background(SurfaceColor).padding(16.dp)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("How are you feeling?", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text("Optional", fontSize = 11.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    states.forEachIndexed { i, state ->
                        val pct = localSliders[i]
                        val iconColor = lerp(state.colorLow, state.colorHigh, pct / 100f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { sliderOpen = !sliderOpen }.padding(8.dp)
                        ) {
                            Icon(stateIcon(state.key, pct), contentDescription = state.label, tint = iconColor, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(state.label, fontSize = 10.sp, color = TextSecondary)
                            Text("${pct.roundToInt()}%", fontSize = 9.sp, color = iconColor)
                        }
                    }
                }
                AnimatedVisibility(visible = sliderOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    val trackHeight   = 160.dp
                    val trackHeightPx = with(LocalDensity.current) { trackHeight.toPx() }
                    Column {
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                            states.forEachIndexed { i, state ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(levelLabel(localSliders[i]), fontSize = 10.sp, color = state.colorHigh)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${localSliders[i].roundToInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                    Spacer(Modifier.height(8.dp))
                                    VerticalSlider(
                                        stateKey = state.key, index = i, state = state,
                                        value = localSliders[i], trackHeight = trackHeight,
                                        trackHeightPx = trackHeightPx, onValueChange = { localSliders[i] = it }
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

        Spacer(Modifier.height(12.dp))

        // Categories
        Box(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)).background(SurfaceColor).padding(16.dp)) {
            Column {
                Text("What's this about?", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                GroupedTagPicker(
                    selectedTags = selectedCats,
                    onToggleTag = { cat ->
                        if (cat in selectedCats) selectedCats.remove(cat) else selectedCats.add(cat)
                    },
                    accentColor = AccentGreen,
                    textSecondaryColor = TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Text entry
        Box(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)).background(SurfaceColor).padding(16.dp)) {
            Column {
                Text("Write your entry", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                TextField(
                    value = entryText,
                    onValueChange = { entryText = it; if (it.isNotBlank()) showError = false },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                    placeholder = { Text("What's on your mind?", fontSize = 14.sp, color = TextSecondary) },
                    textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary, lineHeight = 22.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Bottom submit
        Box(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (entryText.isNotBlank()) AccentGreen else Color(0xFF2A2A30))
                .clickable {
                    if (entryText.isNotBlank()) onSubmit(entryText, selectedCats.toList(), localSliders.toList(), currentDateLabel)
                    else showError = true
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (existingEntry != null) "Update entry" else "Save entry",
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = if (entryText.isNotBlank()) BgColor else TextSecondary
            )
        }

        Spacer(Modifier.height(32.dp))
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
            .width(40.dp).height(trackHeight)
            .clip(RoundedCornerShape(12.dp)).background(Color(0xFF13111A))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    val delta = -(dragAmount / trackHeightPx) * 100f
                    currentOnValueChange((currentValue + delta).coerceIn(0f, 100f))
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(value / 100f)
                .clip(RoundedCornerShape(12.dp))
                .background(lerp(state.colorLow, state.colorHigh, value / 100f).copy(alpha = 0.5f))
        )
        Box(
            modifier = Modifier
                .padding(bottom = (trackHeight * (value / 100f) - 20.dp).coerceAtLeast(0.dp))
                .size(34.dp).clip(CircleShape)
                .background(lerp(state.colorLow, state.colorHigh, value / 100f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(stateIcon(stateKey, 50f), contentDescription = null,
                tint = Color.White, modifier = Modifier.size(15.dp))
        }
    }
}

private fun levelLabel(value: Float): String = when {
    value >= 80 -> "Thriving"
    value >= 60 -> "Good"
    value >= 40 -> "Okay"
    value >= 20 -> "Low"
    else        -> "Drained"
}