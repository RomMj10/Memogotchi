package com.example.memogotchi.ui.page

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.InsertChart
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.memogotchi.R
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex
import com.example.memogotchi.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import java.util.Calendar

// ── Palette ───────────────────────────────────────────────────────────────────
private val AppTheme = LocalAppColors

// ════════════════════════════════════════════════════════════════════════════
//  ROOT
// ════════════════════════════════════════════════════════════════════════════
private const val POMODORO_TARGET_SECONDS = 1500L
private const val SPEECH_BUBBLE_DURATION_MS = 8000L
val bubbleEnterTransition = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
        scaleIn(
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioHighBouncy
            ),
            initialScale = 0.5f,

            )
val bubbleExitTransition =
    fadeOut(animationSpec = tween(durationMillis = 10000, easing = EaseOutBounce)) +
            scaleOut(
                targetScale = 0.0f
            )

@Preview
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PetScreen(
    today: DayData? = null,
    petState: PetState = PetState(),
    xpEarned: Int = 0,
    batteryLevel: Int = 0,
    elapsedSeconds: Long = 0L,
    timerRunning: Boolean = true,
    timerMode: TimerMode = TimerMode.STOPWATCH,
    activeTaskTitle: String? = null,
    activeTaskTargetSeconds: Int? = null,
    taskAnnouncement: String? = null,
    petName: String = "",
    onTimerToggle: () -> Unit = {},
    onModeChange: (TimerMode) -> Unit = {},
    onClose: () -> Unit = {},
    onReset: () -> Unit = {},
    onSettings: () -> Unit = {},
    previewTasks: List<AnalogTask> = emptyList(),
    onOpenTasks: () -> Unit = {},
    onOpenScreenTime: () -> Unit = {},
    onOpenWellness: () -> Unit = {},
    onOpenActivityTree: () -> Unit = {},
    onTaskAnnouncementConsumed: () -> Unit = {},
    personalityUnlocked: Boolean = false,
    personalityDirty: Boolean = false,
    onOpenPersonality: () -> Unit = {},
    yesterdayTotalMs: Long? = null,
    scheduledBlockedApps: List<AppUsageInfo> = emptyList(),
    onScheduledBlockClick: () -> Unit = {},
) {
    var hexMenuOpen by remember { mutableStateOf(false) }
    var showTaskPanel by remember { mutableStateOf(false) }
    var showWardrobe by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var equippedRoomId by remember { mutableStateOf(ShopStore.equippedItemId(context,
        ShopCategory.ROOM
    ))}
    val equippedRoomItem = remember(equippedRoomId) {
        shopCatalog.firstOrNull{it.id == equippedRoomId}
    }
    var equippedPetItemId by remember { mutableStateOf(ShopStore.equippedItemId(context,
        ShopCategory.PET
    ))}
    val equippedPetItem = remember(equippedPetItemId) {
        shopCatalog.firstOrNull{it.id == equippedPetItemId}
    }

    val hexItems =
        remember(previewTasks, onOpenTasks, onOpenScreenTime, onOpenWellness, onOpenPersonality) {
            listOf(
                HexMenuItem(Icons.Outlined.AccountTree, "Activity Tree") {
                    hexMenuOpen = false
                    onOpenActivityTree()
                },
                HexMenuItem(Icons.Outlined.InsertChart, "Screen Time") {
                    hexMenuOpen = false
                    onOpenScreenTime()
                },
                HexMenuItem(Icons.Outlined.BatteryStd, "Wellness") {
                    hexMenuOpen = false
                    onOpenWellness()
                },
                HexMenuItem(Icons.Outlined.Checklist, "Tasks") {
                    hexMenuOpen = false
                    showTaskPanel = true
                },
                HexMenuItem(Icons.Outlined.Psychology, "Personality") {
                    hexMenuOpen = false
                    onOpenPersonality()
                },
                HexMenuItem(Icons.Outlined.Checkroom, "Wardrobe") {
                    hexMenuOpen = false
                    showWardrobe = true
                }
            )
        }

    val totalHours = remember(today) { (today?.totalMs ?: 0L) / 3_600_000.0 }
    val dailyLabel = remember(totalHours) { formatDailyTotal(totalHours) }

    var showSpeechBubble by remember { mutableStateOf(false) }
    var bubbleText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(taskAnnouncement) {
        if (!taskAnnouncement.isNullOrBlank()) {
            bubbleText = taskAnnouncement
            showSpeechBubble = true
            delay(SPEECH_BUBBLE_DURATION_MS)
            showSpeechBubble = false
            onTaskAnnouncementConsumed()
        }
    }
    LaunchedEffect(petState.speechBubble, taskAnnouncement) {
        if (taskAnnouncement.isNullOrBlank()) {
            if (!petState.speechBubble.isNullOrBlank()) {
                bubbleText = petState.speechBubble
                showSpeechBubble = true
                delay(SPEECH_BUBBLE_DURATION_MS)
                showSpeechBubble = false
            } else {
                showSpeechBubble = false
            }
        }
    }

    val progress = if (activeTaskTargetSeconds != null && activeTaskTargetSeconds > 0) {
        (elapsedSeconds.toFloat() / activeTaskTargetSeconds).coerceIn(0f, 1f)
    } else when (timerMode) {
        TimerMode.POMODORO -> (elapsedSeconds.toFloat() / POMODORO_TARGET_SECONDS).coerceIn(0f, 1f)
        TimerMode.STOPWATCH -> (elapsedSeconds.toFloat() / 1800f).coerceIn(0f, 1f)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.current.bg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {


            // ── Top bar ───────────────────────────────────────────────────
            Text(
                text = petName,
                fontSize = 24.sp,
                fontFamily = GildaDisplay,
                fontWeight = FontWeight.Bold,
                color = AppTheme.current.textPrimary,
                modifier = Modifier.zIndex(9.0f).offset(y = 24.dp)
            )
            TopBar(
                onClose = onClose,
                onSettings = onSettings,
                timerMode = timerMode,
                onModeChange = onModeChange,
                locked = activeTaskTitle != null,
                onOpenPersonality = onOpenPersonality
            )


            // ── Pet + speech bubble layered ───────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center,
            ) {
                equippedRoomItem?.let {room ->
                    Image(
                        painter = painterResource(room.assetRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth()
                            .height(290.dp)
                            .offset(y = -90.dp)
                            .scale(1.7f)
                            .zIndex(-999.9f)
                            .clip(RoundedCornerShape(256.dp)),
                    )
                }
                // Pet overflows upward beyond the Box bounds
                PetCard(
                    petState = petState,
                    accessory = equippedPetItem,
                    modifier = Modifier
                        .size(220.dp)
                        .align(Alignment.TopCenter)
                        .clickable { hexMenuOpen = !hexMenuOpen }
                )

                // Speech bubble sits at the bottom of the Box, overlapping pet
                this@Column.AnimatedVisibility(
                    visible = showSpeechBubble,
                    enter = bubbleEnterTransition,
                    exit = bubbleExitTransition
                ) {
                    SpeechBubble(
                        text = bubbleText ?: "",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .offset(y = (-100).dp)

                    )
                }
                PetHexFabMenu(
                    expanded = hexMenuOpen,
                    items = hexItems,
                    modifier = Modifier
                        .matchParentSize()
                        .align(Alignment.TopCenter)
                        .offset(y = (-10).dp)
                        .zIndex(199.9f)
                )
            }

            // ── Giant timer display ───────────────────────────────────────
            TimerDisplay(
                elapsedSeconds = elapsedSeconds,
                progress = progress,
                timerMode = timerMode,
                subtitleOverride = activeTaskTitle,
                showReset = elapsedSeconds > 0L,
                onTap = { onTimerToggle() },
                onReset = onReset,
            )

            Spacer(Modifier.height(18.dp))

            // ── Screen time summary cards (below pet, above stats bar) ────
            ScreenTimeSummaryRow(
                today = today,
                yesterdayTotalMs = yesterdayTotalMs,
                scheduledBlockedApps = scheduledBlockedApps,
                onScheduledBlockClick = onScheduledBlockClick,
                onOpenScreenTime = onOpenScreenTime
            )

            Spacer(Modifier.height(24.dp))

            StatsBar(
                xpEarned = xpEarned,
                dailyTotal = dailyLabel,
                batterylvl = batteryLevel
            )

            Spacer(Modifier.height(56.dp))
        }
        if (showTaskPanel) {
            MiniTaskPanel(
                tasks = previewTasks,
                onDismiss = { showTaskPanel = false },
                onViewAll = {
                    showTaskPanel = false
                    onOpenTasks()
                }
            )
        }
    }
    if (showWardrobe) {
        WardrobeMenu(
            onDismiss = { showWardrobe = false},
            onEquippedChanged = {
                equippedRoomId = ShopStore.equippedItemId(context, ShopCategory.ROOM)
                equippedPetItemId = ShopStore.equippedItemId(context, ShopCategory.PET)
            }
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  TOP BAR
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun TopBar(
    onClose: () -> Unit,
    onSettings: () -> Unit,
    timerMode: TimerMode,
    onModeChange: (TimerMode) -> Unit,
    locked: Boolean = false,
    personalityUnlocked: Boolean = false,
    personalityDirty: Boolean = false,
    onOpenPersonality: () -> Unit = {},

    ) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 80.dp)
            .zIndex(99.9f),
    ) {
        // Personality icon — top-left
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(36.dp)
                .clip(CircleShape)
                .background(AppTheme.current.surface)
                .clickable { onOpenPersonality() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = "Memo's personality",
                tint = if (personalityUnlocked) AppTheme.current.accent else Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
            if (personalityUnlocked && personalityDirty) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AppTheme.current.accent)
                )

            }
        }

        // Title pill — centered
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(RoundedCornerShape(50.dp))
                .border(1.5.dp, AppTheme.current.accentDark, RoundedCornerShape(50.dp)),
            contentAlignment = Alignment.Center
        ) {
            TimerModeSelector(currentMode = timerMode, onSelect = onModeChange, enabled = !locked)
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  PET CARD
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun PetCard(petState: PetState,accessory: ShopItem? = null, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    val rawRes = when (petState.mood) {
        PetMood.IDLE -> R.raw.pet_idle
        PetMood.HAPPY -> R.raw.pet_idle//pet_happy
        PetMood.CONCERNED -> R.raw.pet_concerned
        PetMood.TIRED -> R.raw.pet_concerned
        PetMood.ALARMED -> R.raw.pet_concerned
    }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )
    val clippedPetSize = 200.dp
    val clippedPetShape = RectangleShape
    val lottieRenderSize = 400.dp

    Box(
        modifier = modifier
            .offset(y = offsetY.dp)
            .size(clippedPetSize)
            .clip(clippedPetShape),
        contentAlignment = Alignment.Center,
    ) {
        // Soft radial glow behind the pet
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            AppTheme.current.accentLight.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .size(lottieRenderSize)
                .scale(1.6f)

        )
        accessory?.let { item ->
            Image(
                painter = painterResource(item.assetRes),
                contentDescription = item.name,
                modifier = Modifier
                    .offset(y = -10.dp)
                    .height(220.dp)
                    .scale(1.0f)
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  SPEECH BUBBLE
// ════════════════════════════════════════════════════════════════════════════


@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier) {

    Card(
        modifier = modifier
            .widthIn(min = 50.dp, max = 390.dp)
            .wrapContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontFamily = Comfortaa,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF232222),
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun TimerModeSelector(
    currentMode: TimerMode,
    onSelect: (TimerMode) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.current.surface)
            .padding(4.dp),
    ) {
        TimerMode.entries.forEach { mode ->
            val selected = mode == currentMode
            val label = if (mode == TimerMode.STOPWATCH) "Stopwatch" else "Pomodoro"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (selected) AppTheme.current.accent else Color.Transparent)
                    .clickable(enabled = !selected && enabled) { onSelect(mode) }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontFamily = GildaDisplay,
                    fontSize = 16.sp,
                    color = if (selected) AppTheme.current.bg else AppTheme.current.accentDark.copy(alpha = if (enabled) 1f else 0.4f),
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  TIMER DISPLAY
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun TimerDisplay(
    elapsedSeconds: Long,
    progress: Float,
    subtitleOverride: String? = null,
    timerMode: TimerMode,
    showReset: Boolean,
    onTap: () -> Unit,
    onReset: () -> Unit,
) {
    val displaySeconds = when (timerMode) {
        TimerMode.POMODORO -> (POMODORO_TARGET_SECONDS - elapsedSeconds).coerceAtLeast(0L)
        TimerMode.STOPWATCH -> (elapsedSeconds)
    }
    val minutes = displaySeconds / 60
    val seconds = displaySeconds % 60
    val timeStr = String.format("%02d:%02d", minutes, seconds)
    val subtitle = subtitleOverride?.let { "TASK: ${it.uppercase()}" } ?: when (timerMode) {
        TimerMode.POMODORO -> "FOCUS SESSION"
        TimerMode.STOPWATCH -> "STAY OFF YOUR PHONE"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(horizontal = 24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = timeStr,
                fontSize = 56.sp,
                fontFamily = GildaDisplay,
                fontWeight = FontWeight.Black,
                color = AppTheme.current.textPrimary,
                letterSpacing = (-2).sp,
                lineHeight = 56.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTap() }
                    .padding(vertical = 4.dp),
            )

            this@Column.AnimatedVisibility(
                visible = showReset,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AppTheme.current.surface)
                        .clickable { onReset() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Reset timer",
                        tint = AppTheme.current.textSecondary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }


        Spacer(Modifier.height(6.dp))

        Text(
            text = subtitle,
            fontFamily = GildaDisplay,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.current.accentDark,
            letterSpacing = 3.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AppTheme.current.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(AppTheme.current.accent, Color(0xFF263630))
                        )
                    )
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  SCREEN TIME SUMMARY ROW
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun ScreenTimeSummaryRow(
    today: DayData?,
    yesterdayTotalMs: Long? = null,
    onOpenScreenTime: () -> Unit,
    scheduledBlockedApps: List<AppUsageInfo> = emptyList(),
    onScheduledBlockClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val totalMs = today?.totalMs ?: 0L
    val hours = totalMs / 3_600_000
    val minutes = (totalMs % 3_600_000) / 60_000
    val topApp = today?.apps?.firstOrNull()

    // true = higher than yesterday (up arrow), false = lower (down arrow), null = no change/no data
    val trendUp: Boolean? = yesterdayTotalMs?.let {
        when {
            totalMs > it -> true
            totalMs < it -> false
            else -> null
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            containerColor = AppTheme.current.accent,
            labelColor = AppTheme.current.bg.copy(alpha = 0.7f),
            label = "SCREEN TIME",
            onClick = onOpenScreenTime
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${hours}h ${minutes}m",
                    fontFamily = GildaDisplay,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.current.bg,
                )
                if (trendUp != null) {
                    Spacer(Modifier.width(5.dp))
                    TrendTriangle(
                        pointingUp = trendUp,
                        color = AppTheme.current.bg,
                        modifier = Modifier.size(9.dp),
                    )
                }
            }
        }

        SummaryCard(
            modifier = Modifier.weight(1f),
            containerColor = AppTheme.current.surface,
            labelColor = AppTheme.current.textPrimary,
            label = "MOST USED",
        ) {
            if (topApp != null) {
                AppIcon(topApp.icon, 20)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = topApp.appName,
                    fontFamily = Comfortaa,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatMs(topApp.totalTimeMs),
                    fontFamily = Comfortaa,
                    fontSize = 9.sp,
                    color = AppTheme.current.textSecondary,
                )
            } else {
                Text(
                    text = "No data yet",
                    fontFamily = Comfortaa,
                    fontSize = 11.sp,
                    color = AppTheme.current.textSecondary,
                )
            }
        }

        SummaryCard(
            modifier = Modifier.weight(1f),
            containerColor = AppTheme.current.surface,
            labelColor = AppTheme.current.textPrimary,
            label = "SCHEDULED BLOCK",
            onClick = onScheduledBlockClick, // TODO(user): wire to open ScheduleScreen
        ) {
            if (scheduledBlockedApps.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    scheduledBlockedApps.take(3).forEach { app ->
                        AppIcon(app.icon, 20)
                    }
                    val overflow = scheduledBlockedApps.size - 3
                    if (overflow > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2C2E34)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("+$overflow", fontSize = 8.sp, color = Color.White, fontFamily = Comfortaa)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${scheduledBlockedApps.size} app${if (scheduledBlockedApps.size == 1) "" else "s"}",
                    fontFamily = Comfortaa,
                    fontSize = 9.sp,
                    color = AppTheme.current.textSecondary,
                )
            } else {
                Text(
                    text = "None set",
                    fontFamily = Comfortaa,
                    fontSize = 11.sp,
                    color = AppTheme.current.textSecondary,
                )
            }
        }
    }
}

/** Simple filled triangle used as an up/down trend indicator. */
@Composable
fun TrendTriangle(pointingUp: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            if (pointingUp) {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            } else {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
            }
            close()
        }
        drawPath(path, color = color)
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    containerColor: Color,
    labelColor: Color,
    label: String,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val innerContent: @Composable ColumnScope.() -> Unit = {
        content()
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            fontFamily = Comfortaa,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor,
            letterSpacing = 1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.height(86.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement = Arrangement.Center,
                content = innerContent,
            )
        }
    } else {
        Card(
            modifier = modifier.height(86.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement = Arrangement.Center,
                content = innerContent,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  STATS BAR
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun StatsBar(
    xpEarned: Int,
    dailyTotal: String,
    batterylvl: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        StatItem(label = "BATTERY", value = "$batterylvl%", accent = false)
        StatItem(label = "XP EARNED", value = "+$xpEarned", accent = true)
        StatItem(label = "DAILY TOTAL", value = dailyTotal, accent = false)
    }
}

@Composable
fun StatItem(label: String, value: String, accent: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = GildaDisplay,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (accent) AppTheme.current.accent else AppTheme.current.accentDark,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontFamily = Comfortaa,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = AppTheme.current.textPrimary,
            letterSpacing = 1.5.sp,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatDailyTotal(hours: Double): String {
    val totalMin = (hours * 60).toInt()
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}