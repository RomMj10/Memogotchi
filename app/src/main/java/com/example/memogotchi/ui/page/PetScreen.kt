package com.example.memogotchi.ui.page

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PetScreen(
    today: DayData?     = null,
    petState: PetState  = PetState(),
    xpEarned: Int       = 0,
    batteryLevel: Int   = 0,
    onClose: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val totalHours  = remember(today) { (today?.totalMs ?: 0L) / 3_600_000.0 }
    val dailyLabel  = remember(totalHours) { formatDailyTotal(totalHours) }
    val hourNow     = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    // Timer state — counts up seconds while screen is open
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var timerRunning   by remember { mutableStateOf(true) }

    LaunchedEffect(timerRunning) {
        while (timerRunning) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    // Target: goal is to stay off phone for 30 min = 1800s (adjustable)
    val targetSeconds = 1800L
    val progress      = (elapsedSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f)

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
            TopBar(onClose = onClose, onSettings = onSettings)

            // ── Pet + speech bubble layered ───────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                // Pet overflows upward beyond the Box bounds
                PetCard(
                    petState = petState,
                    modifier = Modifier
                        .size(420.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (-50).dp)
                )

                // Speech bubble sits at the bottom of the Box, overlapping pet
                SpeechBubbleRow(
                    petState = petState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            // ── Giant timer display ───────────────────────────────────────
            TimerDisplay(
                elapsedSeconds = elapsedSeconds,
                progress       = progress,
                onTap          = { timerRunning = !timerRunning },
            )

            Spacer(Modifier.weight(1f))

            // ── Stats bar ─────────────────────────────────────────────────
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
fun TopBar(onClose: () -> Unit, onSettings: () -> Unit) {
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
                .border(1.5.dp, AccentDark, RoundedCornerShape(50.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = "DEEP FOCUS",
                fontFamily = GildaDisplay,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = AccentDark,
                letterSpacing = 2.sp,
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

    Box(
        modifier         = modifier.offset(y = offsetY.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Soft radial glow behind the pet
        Box(
            modifier = Modifier
                .size(260.dp)
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
            modifier    = Modifier.fillMaxSize(),
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  SPEECH BUBBLE
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun SpeechBubbleRow(petState: PetState, modifier: Modifier = Modifier) {
    val message = petState.speechBubble ?: "Shhh... Pixel is resting"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .border(1.5.dp, Color(0xFFFFFFFF), RoundedCornerShape(50.dp))
            .background(Color(0xFFFFFFFF))
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = message,
            fontSize   = 14.sp,
            fontFamily = Comfortaa,
            fontWeight = FontWeight.Medium,
            color      = BgColor,
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  TIMER DISPLAY
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun TimerDisplay(
    elapsedSeconds: Long,
    progress: Float,
    onTap: () -> Unit,
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timeStr = String.format("%02d:%02d", minutes, seconds)

    // Split into chars for the big blocky display
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(horizontal = 24.dp)
    ) {
        // Giant timer text — two rows like the mockup
        Text(
            text       = timeStr,
            fontSize   = 56.sp,
            fontFamily = GildaDisplay,
            fontWeight = FontWeight.Black,
            color      = TimerColor,
            letterSpacing = (-2).sp,
            lineHeight = 56.sp,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text          = "STAY OFF YOUR PHONE",
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
        verticalAlignment     = Alignment.Bottom,
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