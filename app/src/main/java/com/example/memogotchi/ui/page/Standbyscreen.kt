package com.example.memogotchi.ui.page

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.api.ApiClient
import com.example.memogotchi.api.CheckNearbyReadyRequest
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import com.example.memogotchi.ui.theme.LocalAppColors
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

/**
 * Waits for both the current user and the matched user to reach standby.
 * Polls checkNearbyReady() rather than listening to nearby_presence
 * directly — Firestore rules only allow a user to read their own presence
 * doc, so the other user's status must be checked server-side.
 */
@Composable
fun StandbyScreen(
    matchId: String,
    onBothReady: (otherUserId: String) -> Unit,
    onCancel: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val myUid = FirebaseAuth.getInstance().currentUser?.uid

    var statusText by remember { mutableStateOf("Reaching out…") }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(matchId) {
        if (myUid == null) {
            errorText = "Not signed in."
            return@LaunchedEffect
        }

        try {
            ApiClient.service.setStandbyStatus()
        } catch (e: Exception) {
            errorText = "Couldn't reach the server. Check your connection."
            return@LaunchedEffect
        }

        statusText = "Waiting for the other memo…"

        while (true) {
            try {
                val response = ApiClient.service.checkNearbyReady(CheckNearbyReadyRequest(matchId))
                if (response.bothReady && response.otherUserId != null) {
                    onBothReady(response.otherUserId)
                    break
                }
            } catch (e: Exception) {
                // Transient network blip — keep polling instead of
                // surfacing an error for one failed check.
            }
            delay(3_000L)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "standby-pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse-scale"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val radius = size.minDimension / 2 * pulseScale
                drawCircle(
                    color = colors.accent.copy(alpha = 0.3f),
                    radius = radius,
                    center = Offset(size.width / 2, size.height / 2)
                )
                drawCircle(
                    color = colors.accent,
                    radius = size.minDimension / 4,
                    center = Offset(size.width / 2, size.height / 2)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = errorText ?: statusText,
                fontFamily = GildaDisplay,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (errorText != null) androidx.compose.ui.graphics.Color(0xFFE05252) else colors.textPrimary
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "I sense a memo nearby…",
                fontFamily = Comfortaa,
                fontSize = 12.sp,
                color = colors.textSecondary
            )

            Spacer(Modifier.height(32.dp))

            TextButton(onClick = onCancel) {
                Text("Cancel", color = colors.textSecondary, fontFamily = Comfortaa)
            }
        }
    }
}