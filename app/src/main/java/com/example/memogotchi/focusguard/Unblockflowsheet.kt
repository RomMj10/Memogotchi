package com.example.memogotchi.focusguard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.ui.theme.GildaDisplay
import kotlinx.coroutines.delay

// ── Reused exactly from BlockInterceptOverlay / AppBlockerScreen ──────────────
private val BgColor       = Color(0xFF16171C)
private val SurfaceColor  = Color(0xFF1F2125)
private val AccentGreen   = Color(0xFF77C59D)
private val WarnRed       = Color(0xFFD4537E)
private val TextPrimary   = Color(0xFFF2F2F2)
private val TextSecondary = Color(0xFF888888)

// ── Phrase pool ───────────────────────────────────────────────────────────────
// DEFAULT tone only — HARSH tone falls back to this same pool.
// TODO: Draft HARSH-specific phrases and swap them in here when
//       PhraseTone.HARSH is selected (no distinct copy exists yet).
private val DEFAULT_PHRASES = listOf(
    "I want to use {appname} for a bit",
    "I'm choosing to open {appname} right now",
    "I'm okay with using {appname} for now",
    "I'm unblocking {appname} on purpose",
    "Yes, I really want {appname} right now",
    "I'm aware this breaks my {appname} block",
    "I'm overriding my own rule for {appname}"
)

// ── Math problem generation ───────────────────────────────────────────────────
private data class MathProblem(val display: String, val answer: Int)

private fun generateMathProblem(difficulty: MathDifficulty): MathProblem {
    return when (difficulty) {
        MathDifficulty.EASY -> {
            // Single-digit addition or subtraction
            val a = (1..9).random()
            val b = (1..9).random()
            if ((0..1).random() == 0) {
                MathProblem("$a + $b = ?", a + b)
            } else {
                val (big, small) = if (a >= b) Pair(a, b) else Pair(b, a)
                MathProblem("$big − $small = ?", big - small)
            }
        }
        MathDifficulty.HARD -> {
            // Two-digit multiplication OR a multi-step expression
            if ((0..1).random() == 0) {
                val a = (10..19).random()
                val b = (2..9).random()
                MathProblem("$a × $b = ?", a * b)
            } else {
                val a = (2..9).random()
                val b = (2..9).random()
                val c = (2..9).random()
                MathProblem("($a + $b) × $c = ?", (a + b) * c)
            }
        }
        else -> {
            // MEDIUM (also covers UNSPECIFIED as a safe fallback)
            // Two-digit addition or subtraction
            val a = (10..49).random()
            val b = (10..49).random()
            if ((0..1).random() == 0) {
                MathProblem("$a + $b = ?", a + b)
            } else {
                val (big, small) = if (a >= b) Pair(a, b) else Pair(b, a)
                MathProblem("$big − $small = ?", big - small)
            }
        }
    }
}

// ── Public composable ─────────────────────────────────────────────────────────

/**
 * Bottom sheet that presents an unblock challenge before allowing the user
 * back into a blocked app. Called from BlockInterceptOverlay when the user
 * taps "Unblock −1".
 *
 * @param appLabel        Display name of the app being unblocked.
 * @param unblockMethod   Which challenge type to show.
 * @param phraseConfig    Required when [unblockMethod] == PHRASE; null otherwise.
 * @param delayConfig     Required when [unblockMethod] == DELAY; null otherwise.
 * @param mathConfig      Required when [unblockMethod] == MATH; null otherwise.
 * @param onSuccess       Called when the challenge is completed — caller should
 *                        call finish() to let the user into the app.
 * @param onCancel        Called when the user dismisses the sheet without
 *                        completing the challenge — caller should NOT finish(),
 *                        so the intercept screen remains active.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnblockFlowSheet(
    appLabel: String,
    unblockMethod: UnblockMethod,
    phraseConfig: PhraseConfig?,
    delayConfig: DelayConfig?,
    mathConfig: MathConfig?,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    // Proceed-ready state is owned here; each challenge sub-composable flips it.
    var proceedEnabled by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        containerColor = SurfaceColor,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Text(
                text = "Unblock $appLabel",
                fontFamily = GildaDisplay,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = when (unblockMethod) {
                    UnblockMethod.PHRASE -> "Type the phrase below exactly to continue."
                    UnblockMethod.DELAY  -> "Wait for the countdown to finish."
                    UnblockMethod.MATH   -> "Solve the problem to continue."
                    else                 -> "Complete the challenge to continue."
                },
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // ── Challenge area ────────────────────────────────────────────────
            when (unblockMethod) {
                UnblockMethod.PHRASE -> PhraseChallenge(
                    appLabel = appLabel,
                    tone = phraseConfig?.tone ?: PhraseTone.DEFAULT,
                    onProceedReady = { proceedEnabled = it }
                )
                UnblockMethod.DELAY -> DelayChallenge(
                    delaySeconds = delayConfig?.delaySeconds?.takeIf { it > 0 } ?: 30,
                    onProceedReady = { proceedEnabled = it }
                )
                UnblockMethod.MATH -> MathChallenge(
                    difficulty = mathConfig?.difficulty ?: MathDifficulty.MEDIUM,
                    onProceedReady = { proceedEnabled = it }
                )
                else -> {
                    // Fallback: shouldn't occur in practice
                    Text(
                        "Unknown challenge type.",
                        color = WarnRed,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LaunchedEffect(Unit) { proceedEnabled = true }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Proceed button ────────────────────────────────────────────────
            Button(
                onClick = onSuccess,
                enabled = proceedEnabled,
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
                    "Proceed",
                    fontFamily = GildaDisplay,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Cancel text button ────────────────────────────────────────────
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Cancel",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ── Challenge sub-composables ─────────────────────────────────────────────────

/**
 * Shows a randomly selected phrase (substituting {appname}) and a text field.
 * Proceed becomes available only on exact case-sensitive match.
 */
@Composable
private fun PhraseChallenge(
    appLabel: String,
    tone: PhraseTone,
    onProceedReady: (Boolean) -> Unit
) {
    // TODO: When HARSH-specific phrases are drafted, swap DEFAULT_PHRASES for
    //       a HARSH_PHRASES list when tone == PhraseTone.HARSH. For now both
    //       tones use the same pool.
    val pool = DEFAULT_PHRASES
    val targetPhrase = remember { pool.random().replace("{appname}", appLabel) }
    var typed by remember { mutableStateOf("") }

    LaunchedEffect(typed) {
        onProceedReady(typed == targetPhrase)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // The phrase to type
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgColor, RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            Text(
                text = targetPhrase,
                color = TextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = typed,
            onValueChange = { typed = it },
            placeholder = { Text("Type the phrase exactly…", color = TextSecondary, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 2,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentGreen,
                unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = AccentGreen
            )
        )
    }
}

/**
 * Counts down from [delaySeconds] to 0. Proceed becomes available when the
 * countdown completes. The countdown is not skippable.
 */
@Composable
private fun DelayChallenge(
    delaySeconds: Int,
    onProceedReady: (Boolean) -> Unit
) {
    var remaining by remember { mutableIntStateOf(delaySeconds) }

    LaunchedEffect(delaySeconds) {
        while (remaining > 0) {
            delay(1_000L)
            remaining--
        }
        onProceedReady(true)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgColor, RoundedCornerShape(10.dp))
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${remaining}s",
            fontFamily = GildaDisplay,
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            color = if (remaining > 0) TextPrimary else AccentGreen
        )
    }
}

/**
 * Generates one random arithmetic problem for [difficulty]. The Proceed button
 * is enabled only when the user's typed answer is correct. Wrong answers leave
 * the field editable; a subtle hint appears after an incorrect submission.
 */
@Composable
private fun MathChallenge(
    difficulty: MathDifficulty,
    onProceedReady: (Boolean) -> Unit
) {
    val problem = remember { generateMathProblem(difficulty) }
    var typed by remember { mutableStateOf("") }
    var showWrongHint by remember { mutableStateOf(false) }

    // Check correctness on each keystroke so Proceed lights up the moment the
    // right answer is present — but don't show the wrong-answer hint yet
    // (that only appears on an explicit submit attempt below).
    val isCorrect = typed.trim().toIntOrNull() == problem.answer

    LaunchedEffect(isCorrect) {
        onProceedReady(isCorrect)
        if (isCorrect) showWrongHint = false
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgColor, RoundedCornerShape(10.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = problem.display,
                fontFamily = GildaDisplay,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = typed,
            onValueChange = {
                typed = it
                showWrongHint = false // reset hint on any new input
            },
            placeholder = { Text("Your answer…", color = TextSecondary, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentGreen,
                unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = AccentGreen
            )
        )

        // Subtle wrong-answer hint — shown only after the user has typed
        // something non-empty that doesn't match, without revealing the answer.
        if (showWrongHint) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Not quite — try again.",
                color = WarnRed,
                fontSize = 12.sp
            )
        }

        // Surface a non-prominent "Check" button so users on a numeric
        // keyboard get an explicit submit gesture that triggers the hint,
        // without it being confused for "Proceed".
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = {
                if (typed.isNotBlank() && !isCorrect) {
                    showWrongHint = true
                }
            },
            enabled = typed.isNotBlank() && !isCorrect,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(
                "Check",
                color = if (typed.isNotBlank() && !isCorrect) AccentGreen else TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}