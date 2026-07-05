package com.example.memogotchi.focusguard

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import com.example.memogotchi.AppTheme
import com.example.memogotchi.ui.theme.GildaDisplay
import kotlinx.coroutines.launch

// ── Theme tokens ──────────────────────────────────────────────────────────────
private val WarnRed       = Color(0xFFD4537E)

// ── Limit constants ───────────────────────────────────────────────────────────
private const val LIMIT_MIN  = 30   // hard floor — non-negotiable per spec
private const val LIMIT_MAX  = 240  // 4-hour practical ceiling
private const val LIMIT_STEP = 5    // 5-minute increments

// TODO: Daily usage progress ("22 / 60 min used today") is not displayable
//       here because no usage-counter store exists yet. AppTimeLimit only
//       stores the user's chosen limit; actual per-app usage minutes need a
//       separate Preferences DataStore keyed by package + date (e.g.
//       "usage_minutes:{packageName}:{yyyy-MM-dd}" → Int). Nothing currently
//       increments that counter — the natural call site is FocusGuardService's
//       polling loop. Until that store and its increment call are built, this
//       screen shows limits only, with no progress bar.

// ── App info ──────────────────────────────────────────────────────────────────

private data class TimerAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable
)

private fun loadInstalledApps(context: android.content.Context): List<TimerAppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    @Suppress("DEPRECATION")
    return pm.queryIntentActivities(intent, 0)
        .map { it.activityInfo.applicationInfo }
        .filterNot { it.packageName == context.packageName }
        .distinctBy { it.packageName }
        .mapNotNull { info: ApplicationInfo ->
            runCatching {
                TimerAppInfo(
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

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else   -> "${h}h ${m}m"
    }
}

// ── Root composable ───────────────────────────────────────────────────────────

@Composable
fun AppTimerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var installedApps by remember { mutableStateOf<List<TimerAppInfo>>(emptyList()) }
    var timeLimits by remember { mutableStateOf<List<AppTimeLimit>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAppPicker by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<AppTimeLimit?>(null) }

    suspend fun reload() {
        timeLimits = FocusGuardStore.loadConfig(context).timeLimitsList
    }

    LaunchedEffect(Unit) {
        installedApps = loadInstalledApps(context)
        reload()
        isLoading = false
    }

    // ── Delete confirmation ────────────────────────────────────────────────
    val target = deleteTarget
    if (target != null) {
        val label = installedApps.firstOrNull { it.packageName == target.packageName }?.label
            ?: target.packageName
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = AppTheme.current.surface,
            title = { Text("Remove limit?", color = AppTheme.current.textPrimary, fontFamily = GildaDisplay) },
            text = {
                Text(
                    "The daily time limit for $label will be removed.",
                    color = AppTheme.current.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        FocusGuardStore.removeTimeLimit(context, target.packageName)
                        reload()
                        deleteTarget = null
                    }
                }) { Text("Remove", color = WarnRed, fontFamily = GildaDisplay) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = AppTheme.current.textSecondary)
                }
            }
        )
    }

    // ── App picker sheet ───────────────────────────────────────────────────
    if (showAppPicker) {
        val alreadyLimited = timeLimits.map { it.packageName }.toSet()
        AppPickerSheet(
            apps = installedApps.filterNot { it.packageName in alreadyLimited },
            onDismiss = { showAppPicker = false },
            onPick = { pkg ->
                scope.launch {
                    FocusGuardStore.upsertTimeLimit(
                        context,
                        AppTimeLimit.newBuilder()
                            .setPackageName(pkg)
                            .setDailyLimitMinutes(60) // sensible default
                            .build()
                    )
                    reload()
                    showAppPicker = false
                }
            }
        )
    }

    // ── Main UI ────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(AppTheme.current.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            ) {
                TextButton(onClick = onBack) {
                    Text("< Back", color = AppTheme.current.accent, fontFamily = GildaDisplay, fontSize = 16.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Daily Limits",
                    fontFamily = GildaDisplay,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.current.textPrimary
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(64.dp))
            }

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppTheme.current.accent)
                }
                timeLimits.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No limits set", color = AppTheme.current.textSecondary, fontSize = 15.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tap + to add a daily limit for an app",
                            color = AppTheme.current.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(timeLimits, key = { it.packageName }) { limit ->
                        val TimerAppInfo = installedApps.firstOrNull { it.packageName == limit.packageName }
                        TimeLimitRow(
                            limit = limit,
                            TimerAppInfo = TimerAppInfo,
                            onLimitChange = { newMinutes ->
                                scope.launch {
                                    FocusGuardStore.upsertTimeLimit(
                                        context,
                                        limit.toBuilder().setDailyLimitMinutes(newMinutes).build()
                                    )
                                    reload()
                                }
                            },
                            onRemove = { deleteTarget = limit }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAppPicker = true },
            containerColor = AppTheme.current.accent,
            contentColor = AppTheme.current.bg,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add limit")
        }
    }
}

// ── Time limit row ────────────────────────────────────────────────────────────

@Composable
private fun TimeLimitRow(
    limit: AppTimeLimit,
    TimerAppInfo: TimerAppInfo?,
    onLimitChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    // Local slider state — mirrors saved value, updates optimistically so
    // the slider feels responsive without waiting for the store round-trip.
    var sliderValue by remember(limit.packageName) {
        mutableFloatStateOf(limit.dailyLimitMinutes.coerceIn(LIMIT_MIN, LIMIT_MAX).toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.current.surface)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (TimerAppInfo != null) {
                val bitmap = remember(TimerAppInfo.icon) { TimerAppInfo.icon.toBitmapCompat() }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = TimerAppInfo?.label ?: limit.packageName,
                    color = AppTheme.current.textPrimary,
                    fontFamily = GildaDisplay,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Limit: ${formatMinutes(sliderValue.toInt())} / day",
                    color = AppTheme.current.accent,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = WarnRed,
                    modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        // Slider — floor is LIMIT_MIN (30), no way to drag below it
        Slider(
            value = sliderValue,
            onValueChange = { raw ->
                // Snap to nearest LIMIT_STEP increment
                val snapped = (Math.round(raw / LIMIT_STEP) * LIMIT_STEP)
                    .coerceIn(LIMIT_MIN, LIMIT_MAX).toFloat()
                sliderValue = snapped
            },
            onValueChangeFinished = {
                onLimitChange(sliderValue.toInt())
            },
            valueRange = LIMIT_MIN.toFloat()..LIMIT_MAX.toFloat(),
            steps = (LIMIT_MAX - LIMIT_MIN) / LIMIT_STEP - 1,
            colors = SliderDefaults.colors(
                thumbColor = AppTheme.current.accent,
                activeTrackColor = AppTheme.current.accent,
                inactiveTrackColor = AppTheme.current.textSecondary.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${formatMinutes(LIMIT_MIN)}", color = AppTheme.current.textSecondary, fontSize = 10.sp)
            Text("${formatMinutes(LIMIT_MAX)}", color = AppTheme.current.textSecondary, fontSize = 10.sp)
        }
    }
}

// ── App picker sheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(
    apps: List<TimerAppInfo>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = if (searchQuery.isBlank()) apps
    else apps.filter { it.label.contains(searchQuery, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.current.surface,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "Add App Limit",
                    fontFamily = GildaDisplay,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.current.textPrimary
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps…", color = AppTheme.current.textSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.current.accent,
                        unfocusedBorderColor = AppTheme.current.textSecondary.copy(alpha = 0.4f),
                        focusedTextColor = AppTheme.current.textPrimary,
                        unfocusedTextColor = AppTheme.current.textPrimary,
                        cursorColor = AppTheme.current.accent
                    )
                )
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppTheme.current.bg)
                            .clickable { onPick(app.packageName) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bitmap = remember(app.icon) { app.icon.toBitmapCompat() }
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(app.label, color = AppTheme.current.textPrimary, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}