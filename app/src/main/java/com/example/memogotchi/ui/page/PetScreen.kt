package com.example.memogotchi.ui.page

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.memogotchi.R
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import kotlinx.coroutines.delay
import java.util.Calendar

// ── Palette ───────────────────────────────────────────────────────────────────
private val BgColor       = Color(0xFF16171C)   // warm off-white
private val SurfaceColor  = Color(0xFF1F2125)
private val AccentDark    = Color(0xFFAEB9B3)
private val Accent  = Color(0xFF77C59D)
private val TextLight     = Color(0xFF708A7A)
private val PetBorderClr  = Color(0xFF77C59D)
private val TimerColor    = Color(0xFFE6FCFF)

// ════════════════════════════════════════════════════════════════════════════
//  ROOT
// ════════════════════════════════════════════════════════════════════════════
private const val POMODORO_TARGET_SECONDS = 1500L

@Preview
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PetScreen(
today: DayData?          = null,
petState: PetState       = PetState(),
xpEarned: Int            = 0,
batteryLevel: Int        = 0,
elapsedSeconds: Long     = 0L,
timerRunning: Boolean    = true,
timerMode: TimerMode = TimerMode.STOPWATCH,
onTimerToggle: () -> Unit = {},
onModeChange: (TimerMode) -> Unit = {},
onClose: () -> Unit      = {},
onReset: () -> Unit = {},
onSettings: () -> Unit   = {},
) {
    val totalHours = remember(today) { (today?.totalMs ?: 0L) / 3_600_000.0 }
    val dailyLabel = remember(totalHours) { formatDailyTotal(totalHours) }

    var isPomodoro by remember { mutableStateOf(false)}

    // Target: goal is to stay off phone for 30 min = 1800s (adjustable)
    val targetSeconds = 1800L
    val progress      = when(timerMode) {
        TimerMode.POMODORO ->
            (elapsedSeconds.toFloat() / POMODORO_TARGET_SECONDS).coerceIn(0f, 1f)
        TimerMode.STOPWATCH ->
            (elapsedSeconds.toFloat() / 1800f).coerceIn(0f, 1f)
    }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgColor)
                ) {
            Column(
                modifier            = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                // ── Top bar ───────────────────────────────────────────────────
                TopBar(onClose = onClose, onSettings = onSettings, timerMode= timerMode, onModeChange = onModeChange)

                // ── Pet + speech bubble layered ───────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Pet overflows upward beyond the Box bounds
                    PetCard(
                        petState = petState,
                        modifier = Modifier
                            .size(220.dp)
                            .align(Alignment.TopCenter)
                    )

                    // Speech bubble sits at the bottom of the Box, overlapping pet
                    this@Column.AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        SpeechBubbleRow(
                            petState = petState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                                .offset(y = (-100).dp)

                        )
                    }
                }


                // ── Giant timer display ───────────────────────────────────────
                TimerDisplay(
                    elapsedSeconds = elapsedSeconds,
                    progress       = progress,
                    timerMode = timerMode,
                    showReset = elapsedSeconds > 0L,
                    onTap = { onTimerToggle() },
                    onReset = onReset,
                )

                Spacer(Modifier.weight(1f))

                StatsBar(
                    xpEarned      = xpEarned,
                    dailyTotal    = dailyLabel,
                    batterylvl = batteryLevel
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }

// ════════════════════════════════════════════════════════════════════════════
//  TOP BAR
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun TopBar(onClose: () -> Unit, onSettings: () -> Unit, timerMode: TimerMode, onModeChange: (TimerMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 80.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        // Title pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .border(1.5.dp, AccentDark, RoundedCornerShape(50.dp)),
            contentAlignment = Alignment.Center
        ) {
            TimerModeSelector(
                currentMode = timerMode,
                onSelect = onModeChange,
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  PET CARD
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun PetCard(petState: PetState, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue   = 0f,
        targetValue    = -10f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    val rawRes = when (petState.mood) {
        PetMood.IDLE      -> R.raw.pet_idle
        PetMood.HAPPY     -> R.raw.pet_happy
        PetMood.CONCERNED -> R.raw.pet_concerned
        PetMood.TIRED     -> R.raw.pet_concerned
        PetMood.ALARMED   -> R.raw.pet_concerned
    }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))
    val progress    by animateLottieCompositionAsState(
        composition = composition,
        iterations  = LottieConstants.IterateForever,
    )
    val clippedPetSize = 200.dp
    val clippedPetShape = CircleShape
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
                            PetBorderClr.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        LottieAnimation(
            composition = composition,
            progress    = { progress },
            modifier    = Modifier.size(lottieRenderSize)
                .clip(clippedPetShape)
                .scale(2f)

        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  SPEECH BUBBLE
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun SpeechBubbleRow(petState: PetState, modifier: Modifier = Modifier) {
    val message = petState.speechBubble ?: "Shhh... Pixel is resting"

     Card(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .widthIn(min = 50.dp, max = 390.dp)
            .wrapContentSize(),
         colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text       = message,
            fontSize   = 12.sp,
            fontFamily = Comfortaa,
            fontWeight = FontWeight.Medium,
            color      = BgColor,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun TimerModeSelector(
    currentMode: TimerMode,
    onSelect: (TimerMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceColor)
            .padding(4.dp),
    ) {
        TimerMode.entries.forEach { mode ->
            val selected = mode == currentMode
            val label = if (mode == TimerMode.STOPWATCH) "Stopwatch" else "Pomodoro"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (selected) Accent else Color.Transparent)
                    .clickable(enabled = !selected) { onSelect(mode) }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = label,
                    fontFamily = GildaDisplay,
                    fontSize   = 16.sp,
                    color      = if (selected) BgColor else TextLight,
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
    timerMode: TimerMode,
    showReset: Boolean,
    onTap: () -> Unit,
    onReset: () -> Unit,
) {
    val displaySeconds = when(timerMode) {
        TimerMode.POMODORO -> (POMODORO_TARGET_SECONDS - elapsedSeconds).coerceAtLeast(0L)
        TimerMode.STOPWATCH -> (elapsedSeconds)
    }
    val minutes = displaySeconds / 60
    val seconds = displaySeconds % 60
    val timeStr = String.format("%02d:%02d", minutes, seconds)
    val subtitle = when(timerMode) {
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
                color = TimerColor,
                letterSpacing = (-2).sp,
                lineHeight = 56.sp,
                textAlign = TextAlign.Center,
                modifier   = Modifier
                    .fillMaxWidth()
                    .clickable { onTap() }
                    .padding(vertical = 4.dp),
            )

            this@Column.AnimatedVisibility(
                visible = showReset,
                enter   = fadeIn() + scaleIn(),
                exit    = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceColor)
                        .clickable { onReset() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Reset timer",
                        tint = TextLight,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }


        Spacer(Modifier.height(6.dp))

        Text(
            text          = subtitle,
            fontFamily = GildaDisplay,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = TextLight,
            letterSpacing = 3.sp,
            textAlign     = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(SurfaceColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Accent, Color(0xFF263630))
                        )
                    )
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
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top,
    ) {
        StatItem(label = "BATTERY",      value = "$batterylvl%",          accent = false)
        StatItem(label = "XP EARNED",   value = "+$xpEarned",   accent = true)
        StatItem(label = "DAILY TOTAL", value = dailyTotal,     accent = false)
    }
}

@Composable
fun StatItem(label: String, value: String, accent: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            fontFamily = GildaDisplay,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = if (accent) Accent else AccentDark,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text          = label,
            fontFamily = Comfortaa,
            fontSize      = 9.sp,
            fontWeight    = FontWeight.Medium,
            color         = TextLight,
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