package com.example.memogotchi.ui.page

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import android.app.usage.UsageEvents
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.memogotchi.R
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone


// ════════════════════════════════════════════════════════════════════════════
//  PET STATE  — hook your Rive / animation engine here later
// ════════════════════════════════════════════════════════════════════════════


enum class PetMood { IDLE, HAPPY, CONCERNED, TIRED, ALARMED }


data class PetState(
    val mood: PetMood = PetMood.IDLE,
    val speechBubble: String? = null,
)

fun petStateFromScreenTime(totalMs: Long, hourOfDay: Int): PetState {
    val hours = totalMs / 3_600_000.0
    return when {
        hourOfDay >= 22 && hours >= 4  -> PetState(PetMood.TIRED,    "It's late and you've been on your phone for ${hours.toInt()} hrs. Maybe rest?")
        hours >= 12                    -> PetState(PetMood.ALARMED,  "${hours.toInt()} hrs?! That's a lot…")
        hours >= 6                     -> PetState(PetMood.CONCERNED,"${hours.toInt()} hrs today. Take a break soon!")
        hours >= 2                     -> PetState(PetMood.IDLE,    null)
        else                           -> PetState(PetMood.HAPPY,     "Looking good today 🌱")
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  PALETTE
// ════════════════════════════════════════════════════════════════════════════

private val BgColor       = Color(0xFF16171C)
private val SurfaceColor  = Color(0xFF1F2125)
private val TrackColor    = Color(0xFF536257)
private val AccentGreen   = Color(0xFF77C59D)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF888888)

// ════════════════════════════════════════════════════════════════════════════
//  MODELS & HELPERS
// ════════════════════════════════════════════════════════════════════════════

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val totalTimeMs: Long,
)

data class DayData(
    val label: String,
    val fullLabel: String,
    val totalMs: Long,
    val apps: List<AppUsageInfo>,
)

fun formatMs(ms: Long): String {
    val m = ms / 60000
    val h = m / 60; val rm = m % 60
    return when { h > 0 && rm > 0 -> "${h}h ${rm}m"; h > 0 -> "${h}h"; m > 0 -> "${m}m"; else -> "<1m" }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun Context.hasUsageStatsPermission(): Boolean {
    val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    return ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
}

suspend fun loadWeekData(context: Context): List<DayData> = withContext(Dispatchers.IO) {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val pm  = context.packageManager
    val full  = listOf("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
    val short = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")

    (6 downTo 0).map { ago ->
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            add(Calendar.DAY_OF_YEAR, -ago)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val dow = cal.get(Calendar.DAY_OF_WEEK) - 1
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis


        val foregroundMs = mutableMapOf<String, Long>()
        val resumeTime = mutableMapOf<String, Long>()

        val events = usm.queryEvents(start, end)
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when(event.eventType) {
                android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                    resumeTime[event.packageName] = event.timeStamp
                }
                android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val resume  = resumeTime.remove(event.packageName)
                    if (resume != null) {
                        val delta = event.timeStamp - resume
                        if (delta > 0) {
                            foregroundMs[event.packageName] =
                                (foregroundMs[event.packageName] ?: 0L) + delta
                        }
                    }
                }
            }
        }
        resumeTime.forEach { (pkg, resume) ->
            val delta = end.coerceAtMost(System.currentTimeMillis()) - resume
            if (delta > 0) {
                foregroundMs[pkg] = (foregroundMs[pkg] ?: 0L) + delta
            }
        }

        val apps  = foregroundMs
            .filter { it.value > 0 }
            .mapNotNull { (pkg, ms) ->
                try {
                    val info = pm.getApplicationInfo(pkg, 0)
                    AppUsageInfo(
                        packageName  = pkg,
                        appName      = pm.getApplicationLabel(info).toString(),
                        icon         = pm.getApplicationIcon(pkg),
                        totalTimeMs  = ms,
                    )
                } catch (e: PackageManager.NameNotFoundException) { null }
            }.sortedByDescending { it.totalTimeMs }
        DayData(short[dow], full[dow], apps.sumOf { it.totalTimeMs }, apps)
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  ROOT — adapts to phone vs tablet
// ════════════════════════════════════════════════════════════════════════════

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ScreenTimeScreen(
    windowSizeClass: WindowSizeClass? = null,
    weekData: List<DayData> = emptyList(),
    isLoading: Boolean = false,
    hasPermission: Boolean = false,
    onGrantPermission: () -> Unit = {},
    onRefreshPermission: () -> Unit = {},
) {

    val today       = weekData.lastOrNull()
    val hourNow     = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val petState    = remember(today) { petStateFromScreenTime(today?.totalMs ?: 0L, hourNow) }
    val isWide      = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded


    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {

        when {
            isLoading -> CircularProgressIndicator(color = AccentGreen, modifier = Modifier.align(Alignment.Center))
            isWide    -> TabletLayout(weekData = weekData, petState = petState)
            else      -> PhoneLayout(weekData = weekData, petState = petState)
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  PHONE LAYOUT  — pet on top, tab switcher below, panels beneath
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun PhoneLayout(weekData: List<DayData>, petState: PetState) {
    var selectedTab by remember { mutableStateOf(0) }
    var selectedDay by remember { mutableStateOf(weekData.lastIndex.coerceAtLeast(0)) }
    val currentDay  = weekData.getOrNull(selectedDay)

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Pet area (top ~40% of screen) ────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.63f),
            contentAlignment = Alignment.Center
        ) {
            PetPlaceholder(
                petState = petState,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Tab switcher ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceColor)
                .padding(4.dp),
        ) {
            TabBtn("App Usage",     selectedTab == 0) { selectedTab = 0 }
            TabBtn("Daily Screen Time", selectedTab == 1) { selectedTab = 1 }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Content panel ─────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(0.58f)) {
            if (selectedTab == 0) {
                AppListPanel(day = currentDay, showDayHeader = false)
            } else {
                ChartPanel(
                    weekData    = weekData,
                    selectedDay = selectedDay,
                    onDaySelect = { selectedDay = it },
                    compact     = true
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  TABLET LAYOUT  — pet + bubble top-center, two columns below
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun TabletLayout(weekData: List<DayData>, petState: PetState) {
    var selectedDay by remember { mutableStateOf(weekData.lastIndex.coerceAtLeast(0)) }
    val currentDay  = weekData.getOrNull(selectedDay)

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {

        // ── Header ────────────────────────────────────────────────────────────
        Text("Memogotchi", fontSize = 22.sp, fontFamily = GildaDisplay, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        // ── Pet (smaller, centered) ───────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            PetPlaceholder(petState = petState, modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Two-column content ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AppListPanel(day = currentDay, showDayHeader = true)
            }
            Box(modifier = Modifier.weight(1f)) {
                ChartPanel(
                    weekData    = weekData,
                    selectedDay = selectedDay,
                    onDaySelect = { selectedDay = it },
                    compact     = false
                )
            }
        }
    }
}
@Composable
fun PetPlaceholder(petState: PetState, modifier: Modifier = Modifier) {

    val rawRes = when (petState.mood) {
        PetMood.IDLE   -> R.raw.pet_idle
        PetMood.HAPPY   -> R.raw.pet_happy
        PetMood.CONCERNED   -> R.raw.pet_concerned
        PetMood.TIRED   -> R.raw.pet_concerned
        PetMood.ALARMED   -> R.raw.pet_concerned

    }

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(rawRes)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(64.dp))

            LottieAnimation(
                composition = composition,
                progress    = { progress },
                modifier    = Modifier.height(400.dp).width(512.dp)
            )
            // ─────────────────────────────────────────────────────────────────

            // Mood label (remove once real animation shows mood visually)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text  = petState.mood.name.lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 16.sp,
                fontFamily = Comfortaa,
                color = TextSecondary
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  CHART PANEL
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun ChartPanel(
    weekData: List<DayData>,
    selectedDay: Int,
    onDaySelect: (Int) -> Unit,
    compact: Boolean,
) {
    val day = weekData.getOrNull(selectedDay)

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Screen Time", fontSize = 13.sp, color = TextSecondary, fontFamily = Comfortaa, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (day != null) formatMs(day.totalMs) else "—",
                        fontSize = if (compact) 28.sp else 34.sp,
                        fontFamily = Comfortaa,
                        fontWeight = FontWeight.Bold,
                        color = if ((day?.totalMs ?: 0) >= 12 * 3_600_000) Color(0xFFE87573) else TextPrimary
                    )
                    Text(day?.fullLabel ?: "", fontSize = 12.sp, fontFamily = Comfortaa, color = TextSecondary)
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(20.dp), color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Last 7 days", fontSize = 12.sp, fontFamily = Comfortaa, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    WeekBarChart(weekData = weekData, selectedDay = selectedDay, onDaySelect = onDaySelect)
                }
            }
        }

        if (day != null && day.apps.isNotEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Top apps", fontSize = 12.sp, fontFamily = Comfortaa, color = TextSecondary)
                        Spacer(Modifier.height(10.dp))
                        day.apps.take(3).forEach { app ->
                            MiniAppRow(app = app, totalMs = day.totalMs)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ── Bar chart ─────────────────────────────────────────────────────────────────

@Composable
fun WeekBarChart(weekData: List<DayData>, selectedDay: Int, onDaySelect: (Int) -> Unit) {
    val maxMs = weekData.maxOfOrNull { it.totalMs } ?: 1L
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        weekData.forEachIndexed { i, day ->
            val frac   = if (maxMs > 0) day.totalMs.toFloat() / maxMs else 0f
            val isSel  = i == selectedDay
            val barClr by animateColorAsState(if (isSel) AccentGreen else TrackColor, tween(200), label = "b$i")
            val lblClr by animateColorAsState(if (isSel) AccentGreen else TextSecondary, tween(200), label = "l$i")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f).height(88.dp).clickable { onDaySelect(i) }
            ) {
                Box(Modifier.height(68.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.width(18.dp).height((frac * 68).coerceAtLeast(4f).dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(barClr))
                }
                Spacer(Modifier.height(4.dp))
                Text(day.label, fontFamily = Comfortaa, fontSize = 10.sp, color = lblClr, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

// ── Mini app row ──────────────────────────────────────────────────────────────

@Composable
fun MiniAppRow(app: AppUsageInfo, totalMs: Long) {
    val frac = if (totalMs > 0) app.totalTimeMs.toFloat() / totalMs else 0f
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AppIcon(app.icon, 30)
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(app.appName, fontSize = 12.sp, color = TextPrimary, fontFamily = Comfortaa, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(formatMs(app.totalTimeMs), fontSize = 11.sp, fontFamily = Comfortaa, color = TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(TrackColor)) {
                Box(Modifier.fillMaxWidth(frac.coerceIn(0f,1f)).fillMaxHeight().background(AccentGreen))
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  APP LIST PANEL
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun AppListPanel(day: DayData?, showDayHeader: Boolean) {
    if (day == null || day.apps.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data for this day", fontFamily = Comfortaa, color = TextSecondary, fontSize = 14.sp)
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showDayHeader) {
            item {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(day.fullLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = Comfortaa, color = TextPrimary)
                    Text(formatMs(day.totalMs), fontSize = 13.sp, fontFamily = Comfortaa, color = AccentGreen)
                }
            }
        }
        items(day.apps, key = { it.packageName }) { app ->
            AppListRow(app = app, totalMs = day.totalMs)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun AppListRow(app: AppUsageInfo, totalMs: Long) {
    val frac    = if (totalMs > 0) app.totalTimeMs.toFloat() / totalMs else 0f
    val percent = (frac * 100).toInt()
    Surface(shape = RoundedCornerShape(16.dp), color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIcon(app.icon, 44)
            Column(Modifier.weight(1f)) {
                Text(app.appName, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = Comfortaa, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Screentime: ${formatMs(app.totalTimeMs)}", fontFamily = Comfortaa, fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.height(5.dp))
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(TrackColor)) {
                    Box(Modifier.fillMaxWidth(frac.coerceIn(0f,1f)).fillMaxHeight().background(AccentGreen))
                }
            }
            Text("$percent%", fontFamily = GildaDisplay, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}

// ── App icon ──────────────────────────────────────────────────────────────────

@Composable
fun AppIcon(drawable: Drawable?, sizeDp: Int) {
    if (drawable != null) {
        val bmp = remember(drawable) { drawable.toBitmap(sizeDp * 2, sizeDp * 2).asImageBitmap() }
        Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size(sizeDp.dp).clip(RoundedCornerShape(10.dp)))
    } else {
        Box(Modifier.size(sizeDp.dp).clip(RoundedCornerShape(10.dp)).background(TrackColor))
    }
}

// ── Tab button ────────────────────────────────────────────────────────────────

@Composable
fun RowScope.TabBtn(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg  by animateColorAsState(if (selected) AccentGreen else Color.Transparent, tween(200), label = "tb")
    val clr by animateColorAsState(if (selected) Color.White else TextSecondary, tween(200), label = "tc")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(bg).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp)
    ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = Comfortaa, color = clr, maxLines = 1) }
}

// ── Permission screen ─────────────────────────────────────────────────────────

@Composable
fun PermissionScreen(onGrant: () -> Unit, onRefresh: () -> Unit) {
    var isVisible by remember { mutableStateOf(false)}
    LaunchedEffect(Unit) { isVisible = true }
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(2000)
            ),
            exit = fadeOut(),

            ) {
            Text("📊", fontSize = 52.sp)
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(1000)
            ),
            exit = fadeOut(),

            ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(20.dp))
                Text("Usage access needed", fontFamily = GildaDisplay, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                Text("Memogotchi needs permission to read app usage data.", fontFamily = Comfortaa, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
                Spacer(Modifier.height(28.dp))
                Button(onClick = onGrant, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)) {
                    Text("Grant access", fontFamily = Comfortaa, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onRefresh) { Text("I already granted it — refresh", color = TextSecondary, fontFamily = Comfortaa, fontSize = 13.sp) }
            }

        }
    }
}