package com.example.memogotchi.ui.page

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import java.util.Calendar
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
    val dateLabel: String,
    val text: String,
    val categories: List<String>,
    val sliderSnapshot: List<Float>?
)

enum class DiaryMode { NONE, TODAY, YESTERDAY, OTHER_DAY, VOICE, PHOTO }

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

private val diaryCategories = listOf(
    "Family", "Friends", "Work", "Hobbies", "Health",
    "Food", "Self-care", "Travel", "Learning", "Rest",
    "Exercise", "Creativity", "Gratitude", "Stress", "Love"
)

// ── WellnessScreen ────────────────────────────────────────────────────────────
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

    val loggedCount = states.count { it.value != null }
    val batteryPct  = if (loggedCount == 0) null
    else states.mapNotNull { it.value }.average().roundToInt()

    val bgFillColor = if (batteryPct != null)
        lerp(Color(0xFF3D1A1A), Color(0xFF1A3D2E), batteryPct / 100f)
    else SurfaceColor

    val context = LocalContext.current

    // Date picker for OTHER_DAY
    var datePickerShown by remember { mutableStateOf(false) }

    if (diaryMode == DiaryMode.OTHER_DAY && pickedDate.isEmpty() && !datePickerShown) {
        LaunchedEffect(Unit) {
            datePickerShown = true
            val cal = Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, y, m, d ->
                    val cal2 = Calendar.getInstance().apply { set(y, m, d) }
                    pickedDate = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.ENGLISH).format(cal2.time)
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

    // Show entry screen
    val showEntry = diaryMode == DiaryMode.TODAY ||
            diaryMode == DiaryMode.YESTERDAY ||
            (diaryMode == DiaryMode.OTHER_DAY && pickedDate.isNotEmpty())

    if (showEntry) {
        val dateLabel = when (diaryMode) {
            DiaryMode.TODAY     -> "Today"
            DiaryMode.YESTERDAY -> "Yesterday"
            else                -> pickedDate
        }
        DiaryEntryScreen(
            dateLabel    = dateLabel,
            states       = states,
            sliderValues = sliderValues,
            onSubmit     = { text, cats, updatedSliders ->
                updatedSliders?.forEachIndexed { i, v ->
                    states[i] = states[i].copy(value = v)
                    sliderValues[i] = v
                }
                diaryEntries.add(0, DiaryEntry(dateLabel, text, cats, updatedSliders))
                diaryMode  = DiaryMode.NONE
                pickedDate = ""
            },
            onBack = {
                diaryMode  = DiaryMode.NONE
                pickedDate = ""
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

            // Overall battery card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceColor)
            ) {
                if (batteryPct != null) {
                    Row(modifier = Modifier.matchParentSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(batteryPct / 100f)
                                .background(bgFillColor)
                        )
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

            // States section
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
                                            isExpanded = true
                                        }
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.value != null) stateIcon(state.key, pct) else stateIcon(state.key, 50f),
                                    contentDescription = state.label,
                                    tint = iconColor,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(state.label, fontSize = 10.sp,
                                    color = if (isExpanded) state.colorHigh else TextSecondary)
                                if (state.value != null) {
                                    Text("${state.value.roundToInt()}%",
                                        fontSize = 9.sp, color = iconColor)
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit  = shrinkVertically() + fadeOut()
                    ) {
                        val trackHeight   = 180.dp
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
                                        Text("${sliderValues[i].roundToInt()}%",
                                            fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                        Spacer(Modifier.height(8.dp))
                                        VerticalSlider(
                                            stateKey      = state.key,
                                            index         = i,
                                            state         = state,
                                            value         = sliderValues[i],
                                            trackHeight   = trackHeight,
                                            trackHeightPx = trackHeightPx,
                                            onValueChange = { sliderValues[i] = it }
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
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isExpanded) AccentGreen else SurfaceColor)
                                .clickable {
                                    if (isExpanded) {
                                        states.forEachIndexed { i, state ->
                                            states[i] = state.copy(value = sliderValues[i])
                                        }
                                        isExpanded = false
                                    } else {
                                        states.forEachIndexed { i, state ->
                                            sliderValues[i] = state.value ?: 50f
                                        }
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
                        Icon(Icons.Outlined.Book, contentDescription = null,
                            tint = TextSecondary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No entries yet", fontSize = 13.sp, color = TextSecondary)
                        Text("Tap + to write your first entry", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            } else {
                diaryEntries.forEach { entry ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
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
                                Text(entry.dateLabel, fontSize = 12.sp,
                                    color = AccentGreen, fontWeight = FontWeight.Medium)
                                if (entry.categories.isNotEmpty()) {
                                    Text(entry.categories.take(2).joinToString(", "),
                                        fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(entry.text, fontSize = 13.sp, color = TextPrimary, lineHeight = 20.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }

        // Scrim
        if (fabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { fabExpanded = false }
            )
        }

        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
        ) {
            val fabItems = listOf(
                Triple(DiaryMode.TODAY,     Icons.Outlined.Today,         "Today"),
                Triple(DiaryMode.YESTERDAY, Icons.Outlined.History,       "Yesterday"),
                Triple(DiaryMode.OTHER_DAY, Icons.Outlined.CalendarMonth, "Other day"),
                Triple(DiaryMode.VOICE,     Icons.Outlined.Mic,           "Voice"),
                Triple(DiaryMode.PHOTO,     Icons.Outlined.PhotoCamera,   "Photo"),
            )

            fabItems.forEachIndexed { i, (mode, icon, label) ->
                val transition = updateTransition(fabExpanded, label = "fab_$i")
                val offsetY by transition.animateDp(
                    transitionSpec = {
                        if (targetState) spring(dampingRatio = 0.6f, stiffness = 200f + i * 30f)
                        else tween(150)
                    }, label = "offsetY_$i"
                ) { expanded -> if (expanded) (-(i + 1) * 72).dp else 0.dp }
                val alpha by transition.animateFloat(
                    transitionSpec = { tween(if (targetState) 200 + i * 40 else 100) },
                    label = "alpha_$i"
                ) { if (it) 1f else 0f }
                val scale by transition.animateFloat(
                    transitionSpec = { tween(if (targetState) 200 + i * 40 else 100) },
                    label = "scale_$i"
                ) { if (it) 1f else 0.6f }

                Row(
                    modifier = Modifier
                        .offset(y = offsetY)
                        .alpha(alpha)
                        .scale(scale)
                        .align(Alignment.BottomEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceColor)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(label, fontSize = 11.sp, color = TextPrimary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceColor)
                            .clickable {
                                fabExpanded = false
                                diaryMode   = mode
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = label,
                            tint = AccentGreen, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // Main FAB
            val rotation by animateFloatAsState(
                targetValue = if (fabExpanded) 45f else 0f,
                animationSpec = tween(200), label = "fab_rotation"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AccentGreen)
                    .clickable { fabExpanded = !fabExpanded },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add diary entry",
                    tint = BgColor,
                    modifier = Modifier.size(26.dp).rotate(rotation))
            }
        }
    }
}

// ── Diary Entry Screen ────────────────────────────────────────────────────────
@Composable
fun DiaryEntryScreen(
    dateLabel: String,
    states: List<BatteryState>,
    sliderValues: List<Float>,
    onSubmit: (String, List<String>, List<Float>?) -> Unit,
    onBack: () -> Unit
) {
    val localSliders  = remember { mutableStateListOf(*sliderValues.toTypedArray()) }
    var sliderOpen    by remember { mutableStateOf(false) }
    val selectedCats  = remember { mutableStateListOf<String>() }
    var entryText     by remember { mutableStateOf("") }
    var showError     by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceColor)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back",
                    tint = TextPrimary, modifier = Modifier.size(22.dp))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(dateLabel, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                if (dateLabel != "Today" && dateLabel != "Yesterday") {
                    Text(dateLabel, fontSize = 12.sp, color = TextSecondary)
                } else {
                    val formatted = remember {
                        val cal = Calendar.getInstance()
                        java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.ENGLISH).format(Calendar.getInstance().time)
                    }
                    Text(formatted, fontSize = 12.sp, color = TextSecondary)
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (entryText.isNotBlank()) AccentGreen else Color(0xFF2A2A30))
                    .clickable {
                        if (entryText.isNotBlank()) {
                            onSubmit(entryText, selectedCats.toList(), localSliders.toList())
                        } else {
                            showError = true
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Submit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = if (entryText.isNotBlank()) BgColor else TextSecondary)
            }
        }

        if (showError) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3D1A1A))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("Please write something before submitting.",
                    fontSize = 12.sp, color = Color(0xFFD4537E))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Section 1: Sliders (optional)
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
                    Text("How are you feeling?", fontSize = 14.sp,
                        fontWeight = FontWeight.Medium, color = TextPrimary)
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
                                .clickable { sliderOpen = !sliderOpen }
                                .padding(8.dp)
                        ) {
                            Icon(stateIcon(state.key, pct), contentDescription = state.label,
                                tint = iconColor, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(state.label, fontSize = 10.sp, color = TextSecondary)
                            Text("${pct.roundToInt()}%", fontSize = 9.sp, color = iconColor)
                        }
                    }
                }
                AnimatedVisibility(
                    visible = sliderOpen,
                    enter = expandVertically() + fadeIn(),
                    exit  = shrinkVertically() + fadeOut()
                ) {
                    val trackHeight   = 160.dp
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
                                    Text(levelLabel(localSliders[i]), fontSize = 10.sp, color = state.colorHigh)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${localSliders[i].roundToInt()}%",
                                        fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                    Spacer(Modifier.height(8.dp))
                                    VerticalSlider(
                                        stateKey      = state.key,
                                        index         = i,
                                        state         = state,
                                        value         = localSliders[i],
                                        trackHeight   = trackHeight,
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

        Spacer(Modifier.height(12.dp))

        // Section 2: Categories
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .padding(16.dp)
        ) {
            Column {
                Text("What's this about?", fontSize = 14.sp,
                    fontWeight = FontWeight.Medium, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                diaryCategories.chunked(4).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { cat ->
                            key(cat) {
                                val selected = cat in selectedCats
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp, bottom = 8.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (selected) AccentGreen.copy(alpha = 0.2f)
                                            else Color(0xFF13111A)
                                        )
                                        .clickable {
                                            if (selected) selectedCats.remove(cat)
                                            else selectedCats.add(cat)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        cat, fontSize = 12.sp,
                                        color = if (selected) AccentGreen else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Section 3: Text entry
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .padding(16.dp)
        ) {
            Column {
                Text("Write your entry", fontSize = 14.sp,
                    fontWeight = FontWeight.Medium, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                TextField(
                    value = entryText,
                    onValueChange = {
                        entryText = it
                        if (it.isNotBlank()) showError = false
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                    placeholder = {
                        Text("What's on your mind?", fontSize = 14.sp, color = TextSecondary)
                    },
                    textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary, lineHeight = 22.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor    = Color.Transparent,
                        unfocusedContainerColor  = Color.Transparent,
                        focusedIndicatorColor    = Color.Transparent,
                        unfocusedIndicatorColor  = Color.Transparent
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Bottom submit
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (entryText.isNotBlank()) AccentGreen else Color(0xFF2A2A30))
                .clickable {
                    if (entryText.isNotBlank()) {
                        onSubmit(entryText, selectedCats.toList(), localSliders.toList())
                    } else {
                        showError = true
                    }
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Save entry", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = if (entryText.isNotBlank()) BgColor else TextSecondary)
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Shared vertical slider ────────────────────────────────────────────────────
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
                .background(lerp(state.colorLow, state.colorHigh, value / 100f).copy(alpha = 0.5f))
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
                imageVector = stateIcon(stateKey, 50f),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
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