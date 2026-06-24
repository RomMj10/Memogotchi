package com.example.memogotchi.focusguard

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.memogotchi.ui.theme.GildaDisplay
import kotlinx.coroutines.launch

// ── Theme tokens — reused exactly from the rest of the app, not invented here ──
private val BgColor       = Color(0xFF16171C)
private val SurfaceColor  = Color(0xFF1F2125)
private val AccentGreen   = Color(0xFF77C59D)
private val TextSecondary = Color(0xFF888888)
private val TextPrimary   = Color(0xFFF2F2F2)

/**
 * One row's worth of UI-friendly app info, resolved once from
 * PackageManager and cached in state rather than re-queried on every
 * recomposition.
 */
private data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable
)

@Composable
fun AppBlockerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var blockedApps by remember { mutableStateOf<List<BlockedApp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var accessibilityEnabled by remember { mutableStateOf(context.isFocusGuardAccessibilityServiceEnabled()) }

    // Tracks which package is currently being routed through the
    // Accessibility setup flow, so we know which row to revert to soft
    // block if the user cancels instead of granting access.
    var pendingHardBlockPackage by remember { mutableStateOf<String?>(null) }

    // Re-check accessibility grant status whenever this screen resumes —
    // HardBlockSetupScreen sends the user to system Settings and there's
    // no callback for when they return, so ON_RESUME is the only reliable
    // signal that they might have just granted (or not granted) access.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityEnabled = context.isFocusGuardAccessibilityServiceEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    suspend fun reloadBlockedApps() {
        blockedApps = FocusGuardStore.loadConfig(context).blockedAppsList
    }

    LaunchedEffect(Unit) {
        installedApps = loadInstalledApps(context)
        reloadBlockedApps()
        isLoading = false
    }

    // If the user just granted accessibility access while a hard-block
    // toggle was pending, finish that toggle now instead of leaving it
    // soft despite the user having done what was asked.
    LaunchedEffect(accessibilityEnabled, pendingHardBlockPackage) {
        val pkg = pendingHardBlockPackage
        if (pkg != null && accessibilityEnabled) {
            val existing = blockedApps.firstOrNull { it.packageName == pkg }
            if (existing != null) {
                FocusGuardStore.upsertBlockedApp(
                    context,
                    existing.toBuilder().setIsHardBlock(true).build()
                )
                reloadBlockedApps()
            }
            pendingHardBlockPackage = null
        }
    }

    if (pendingHardBlockPackage != null && !accessibilityEnabled) {
        val pkg = pendingHardBlockPackage!!
        val label = installedApps.firstOrNull { it.packageName == pkg }?.label ?: pkg
        HardBlockSetupScreen(
            appLabelBeingBlocked = label,
            onCancel = {
                // User backed out — leave the rule as soft block, don't
                // silently force hard=true with no enforcement behind it.
                pendingHardBlockPackage = null
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(onBack = onBack)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGreen)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(installedApps, key = { it.packageName }) { app ->
                        val rule = blockedApps.firstOrNull { it.packageName == app.packageName }
                        AppBlockerRow(
                            app = app,
                            rule = rule,
                            onToggleBlocked = { isBlocked ->
                                coroutineScope.launch {
                                    if (isBlocked) {
                                        val newRule = rule?.toBuilder()?.setIsActive(true)?.build()
                                            ?: BlockedApp.newBuilder()
                                                .setPackageName(app.packageName)
                                                .setAppLabel(app.label)
                                                .setIsHardBlock(false)
                                                .setIsActive(true)
                                                .setUnblockMethod(UnblockMethod.PHRASE)
                                                .setPhraseConfig(
                                                    PhraseConfig.newBuilder()
                                                        .setTone(PhraseTone.DEFAULT)
                                                        .build()
                                                )
                                                .build()
                                        FocusGuardStore.upsertBlockedApp(context, newRule)
                                    } else if (rule != null) {
                                        // Use setBlockedAppActive rather than
                                        // removeBlockedApp here: turning the
                                        // toggle off should pause enforcement,
                                        // not throw away the user's configured
                                        // hard/soft mode and unblock method.
                                        // Switching it back on later (the `if`
                                        // branch above) reuses rule's existing
                                        // config via toBuilder() rather than
                                        // rebuilding from scratch with PHRASE/
                                        // DEFAULT every time.
                                        FocusGuardStore.setBlockedAppActive(
                                            context,
                                            app.packageName,
                                            false
                                        )
                                    } else {
                                        // No existing rule and toggled off —
                                        // nothing to do, this shouldn't
                                        // normally be reachable since the
                                        // switch only shows "off" when rule
                                        // is null or inactive already.
                                    }
                                    reloadBlockedApps()
                                }
                            },
                            onSetHardBlock = { wantsHard ->
                                if (wantsHard && !accessibilityEnabled) {
                                    // Don't save isHardBlock=true silently —
                                    // route through setup first.
                                    pendingHardBlockPackage = app.packageName
                                } else {
                                    coroutineScope.launch {
                                        rule?.let {
                                            FocusGuardStore.upsertBlockedApp(
                                                context,
                                                it.toBuilder().setIsHardBlock(wantsHard).build()
                                            )
                                            reloadBlockedApps()
                                        }
                                    }
                                }
                            },
                            onUnblockMethodChange = { method ->
                                coroutineScope.launch {
                                    rule?.let {
                                        FocusGuardStore.upsertBlockedApp(
                                            context,
                                            it.toBuilder().setUnblockMethod(method).build()
                                        )
                                        reloadBlockedApps()
                                    }
                                }
                            },
                            onPhraseToneChange = { tone ->
                                coroutineScope.launch {
                                    rule?.let {
                                        FocusGuardStore.upsertBlockedApp(
                                            context,
                                            it.toBuilder()
                                                .setPhraseConfig(PhraseConfig.newBuilder().setTone(tone).build())
                                                .build()
                                        )
                                        reloadBlockedApps()
                                    }
                                }
                            },
                            onDelaySecondsChange = { seconds ->
                                coroutineScope.launch {
                                    rule?.let {
                                        FocusGuardStore.upsertBlockedApp(
                                            context,
                                            it.toBuilder()
                                                .setDelayConfig(DelayConfig.newBuilder().setDelaySeconds(seconds).build())
                                                .build()
                                        )
                                        reloadBlockedApps()
                                    }
                                }
                            },
                            onMathDifficultyChange = { difficulty ->
                                coroutineScope.launch {
                                    rule?.let {
                                        FocusGuardStore.upsertBlockedApp(
                                            context,
                                            it.toBuilder()
                                                .setMathConfig(MathConfig.newBuilder().setDifficulty(difficulty).build())
                                                .build()
                                        )
                                        reloadBlockedApps()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader(onBack: () -> Unit) {
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
            text = "Block Apps",
            fontFamily = GildaDisplay,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(64.dp)) // balances the back button's width roughly
    }
}

@Composable
private fun AppBlockerRow(
    app: InstalledAppInfo,
    rule: BlockedApp?,
    onToggleBlocked: (Boolean) -> Unit,
    onSetHardBlock: (Boolean) -> Unit,
    onUnblockMethodChange: (UnblockMethod) -> Unit,
    onPhraseToneChange: (PhraseTone) -> Unit,
    onDelaySecondsChange: (Int) -> Unit,
    onMathDifficultyChange: (MathDifficulty) -> Unit
) {
    val isBlocked = rule?.isActive == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceColor)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            AppIcon(app.icon)
            Spacer(Modifier.width(12.dp))
            Text(
                text = app.label,
                color = TextPrimary,
                fontFamily = GildaDisplay,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isBlocked,
                onCheckedChange = onToggleBlocked,
                colors = SwitchDefaults.colors(checkedTrackColor = AccentGreen)
            )
        }

        if (isBlocked && rule != null) {
            Spacer(Modifier.height(12.dp))
            HardSoftPicker(
                isHardBlock = rule.isHardBlock,
                onChange = onSetHardBlock
            )

            Spacer(Modifier.height(12.dp))
            UnblockMethodPicker(
                selected = rule.unblockMethod,
                onChange = onUnblockMethodChange
            )

            Spacer(Modifier.height(10.dp))
            when (rule.unblockMethod) {
                UnblockMethod.PHRASE -> PhraseToneSubConfig(
                    tone = rule.phraseConfig.tone,
                    onChange = onPhraseToneChange
                )
                UnblockMethod.DELAY -> DelaySecondsSubConfig(
                    seconds = rule.delayConfig.delaySeconds,
                    onChange = onDelaySecondsChange
                )
                UnblockMethod.MATH -> MathDifficultySubConfig(
                    difficulty = rule.mathConfig.difficulty,
                    onChange = onMathDifficultyChange
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun AppIcon(drawable: Drawable) {
    val bitmap = remember(drawable) { drawable.toBitmapCompat() }
    androidx.compose.foundation.Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
    )
}

@Composable
private fun HardSoftPicker(isHardBlock: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(BgColor),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SegmentChip(
            label = "Soft",
            selected = !isHardBlock,
            onClick = { onChange(false) },
            modifier = Modifier.weight(1f)
        )
        SegmentChip(
            label = "Hard",
            selected = isHardBlock,
            onClick = { onChange(true) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UnblockMethodPicker(selected: UnblockMethod, onChange: (UnblockMethod) -> Unit) {
    Column {
        Text(
            text = "Unblock method",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BgColor),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SegmentChip("Phrase", selected == UnblockMethod.PHRASE, { onChange(UnblockMethod.PHRASE) }, Modifier.weight(1f))
            SegmentChip("Delay", selected == UnblockMethod.DELAY, { onChange(UnblockMethod.DELAY) }, Modifier.weight(1f))
            SegmentChip("Math", selected == UnblockMethod.MATH, { onChange(UnblockMethod.MATH) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SegmentChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AccentGreen else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) BgColor else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun PhraseToneSubConfig(tone: PhraseTone, onChange: (PhraseTone) -> Unit) {
    // Same issue as the delay/math defaults above: a freshly-created
    // PHRASE rule normally sets DEFAULT explicitly at creation time (see
    // onToggleBlocked's new-rule branch), but if tone is ever
    // UNSPECIFIED for any other reason, persist DEFAULT explicitly rather
    // than relying on "!= HARSH" as an implicit default everywhere this
    // value is read.
    LaunchedEffect(tone) {
        if (tone == PhraseTone.PHRASE_TONE_UNSPECIFIED || tone == PhraseTone.UNRECOGNIZED) {
            onChange(PhraseTone.DEFAULT)
        }
    }

    Column {
        Text(
            text = "Phrase tone",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BgColor),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SegmentChip("Default", tone != PhraseTone.HARSH, { onChange(PhraseTone.DEFAULT) }, Modifier.weight(1f))
            SegmentChip("Harsh", tone == PhraseTone.HARSH, { onChange(PhraseTone.HARSH) }, Modifier.weight(1f))
        }
        // NOTE: Harsh-tone phrases aren't drafted yet anywhere in the
        // codebase — selecting this only stores the preference. Whatever
        // screen actually displays unblock phrases (UnblockFlowSheet, not
        // built as of this file) currently has no distinct Harsh content
        // and will need to fall back to the Default pool until that copy
        // exists.
        Text(
            text = "Harsh-tone phrasing isn't written yet — falls back to Default for now.",
            color = TextSecondary,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun DelaySecondsSubConfig(seconds: Int, onChange: (Int) -> Unit) {
    // No fixed range was specified for delay seconds — only that it's the
    // user's free choice. A floor of 1 is enforced here purely so the
    // value can never be saved as zero or negative, which would make the
    // delay challenge meaningless (an instant unblock). 600s (10 min) is
    // a suggested practical ceiling shown as hint text, not a hard cap.
    //
    // If this is a freshly-created rule, delay_seconds is still the proto
    // default of 0 (unset). Rather than just DISPLAYING a fallback of 30s
    // while leaving the actual stored value at 0 — which would mean
    // anything reading delay_seconds directly (e.g. the future
    // UnblockFlowSheet) sees 0, not what's shown here — persist the
    // default immediately so what's displayed and what's saved never
    // diverge.
    LaunchedEffect(seconds) {
        if (seconds <= 0) {
            onChange(30)
        }
    }

    val displaySeconds = if (seconds <= 0) 30 else seconds

    Column {
        Text(
            text = "Delay: $displaySeconds seconds",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Slider(
            value = displaySeconds.toFloat(),
            onValueChange = { onChange(it.toInt().coerceAtLeast(1)) },
            valueRange = 1f..600f,
            colors = SliderDefaults.colors(thumbColor = AccentGreen, activeTrackColor = AccentGreen)
        )
        Text(
            text = "Suggested range: 15s – 600s (10 min). You can go higher if you want stricter friction.",
            color = TextSecondary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun MathDifficultySubConfig(difficulty: MathDifficulty, onChange: (MathDifficulty) -> Unit) {
    // Same issue as DelaySecondsSubConfig above: a freshly-selected MATH
    // method leaves math_config.difficulty at the proto's UNSPECIFIED zero
    // value until the user taps a chip. Persist a reasonable default
    // (MEDIUM) immediately so nothing downstream (e.g. the future
    // UnblockFlowSheet) ever has to handle an UNSPECIFIED difficulty, and
    // so one of the three chips always shows as selected rather than none.
    LaunchedEffect(difficulty) {
        if (difficulty == MathDifficulty.MATH_DIFFICULTY_UNSPECIFIED || difficulty == MathDifficulty.UNRECOGNIZED) {
            onChange(MathDifficulty.MEDIUM)
        }
    }

    Column {
        Text(
            text = "Math difficulty",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BgColor),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SegmentChip("Easy", difficulty == MathDifficulty.EASY, { onChange(MathDifficulty.EASY) }, Modifier.weight(1f))
            SegmentChip("Medium", difficulty == MathDifficulty.MEDIUM, { onChange(MathDifficulty.MEDIUM) }, Modifier.weight(1f))
            SegmentChip("Hard", difficulty == MathDifficulty.HARD, { onChange(MathDifficulty.HARD) }, Modifier.weight(1f))
        }
    }
}

/**
 * Queries installed, launchable apps via PackageManager. Excludes this
 * app's own package so users can't accidentally block Memogotchi itself.
 * Runs on the calling coroutine — caller is responsible for invoking this
 * from a LaunchedEffect or similar, not the main thread directly, since
 * querying all installed packages can be slow on devices with many apps.
 */
private fun loadInstalledApps(context: android.content.Context): List<InstalledAppInfo> {
    val packageManager = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    @Suppress("DEPRECATION") // queryIntentActivities flags param deprecated in
    // favor of queryIntentActivities(Intent, PackageManager.ResolveInfoFlags)
    // on API 33+, but the simpler overload still works across all supported
    // API levels (minSdk 24) and avoids branching on SDK version here.
    val resolvedApps = packageManager.queryIntentActivities(launcherIntent, 0)

    return resolvedApps
        .map { it.activityInfo.applicationInfo }
        .filterNot { it.packageName == context.packageName }
        .distinctBy { it.packageName }
        .mapNotNull { appInfo: ApplicationInfo ->
            runCatching {
                InstalledAppInfo(
                    packageName = appInfo.packageName,
                    label = packageManager.getApplicationLabel(appInfo).toString(),
                    icon = packageManager.getApplicationIcon(appInfo)
                )
            }.getOrNull()
        }
        .sortedBy { it.label.lowercase() }
}

/**
 * Drawable -> Bitmap conversion, since Compose's Image composable needs an
 * ImageBitmap, not a raw Drawable. Handles both BitmapDrawable (the common
 * case) and other Drawable subtypes (e.g. AdaptiveIconDrawable) by drawing
 * onto a fresh canvas.
 */
private fun Drawable.toBitmapCompat(): android.graphics.Bitmap {
    if (this is android.graphics.drawable.BitmapDrawable) {
        return this.bitmap
    }
    val width = intrinsicWidth.takeIf { it > 0 } ?: 96
    val height = intrinsicHeight.takeIf { it > 0 } ?: 96
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}