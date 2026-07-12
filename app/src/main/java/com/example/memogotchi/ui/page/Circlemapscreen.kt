package com.example.memogotchi.ui.page

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.api.ApiClient
import com.example.memogotchi.api.BuddyRequestRequest
import com.example.memogotchi.ble.NearbyRssiRepository
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import com.example.memogotchi.ui.theme.LocalAppColors
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlin.math.min
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.geometry.Offset

// RSSI range we normalize against for placement. Raw RSSI is noisy, so
// this is intentionally a loose approximation, not a precise distance.
private const val RSSI_CLOSE = -40
private const val RSSI_FAR = -90

private fun rssiToRadiusFraction(rssi: Int): Float {
    val clamped = rssi.coerceIn(RSSI_FAR, RSSI_CLOSE)
    // Closer to RSSI_CLOSE (e.g. -40) -> fraction near 0 (center).
    // Closer to RSSI_FAR (e.g. -90) -> fraction near 1 (edge).
    return 1f - ((clamped - RSSI_FAR).toFloat() / (RSSI_CLOSE - RSSI_FAR).toFloat())
}

@Composable
fun CircleMapScreen(
    otherUserId: String,
    onConnected: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()

    var otherToken by remember { mutableStateOf<String?>(null) }
    val rssiMap by NearbyRssiRepository.rssiByToken.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }
    var requestStatusText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(otherUserId) {
        Firebase.firestore.collection("nearby_presence").document(otherUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                otherToken = snapshot.getString("token")
            }
    }

    val rssi = otherToken?.let { rssiMap[it] }
    // Default to "far" if we haven't heard a scan result yet, rather than
    // snapping the icon to the center on no data.
    val radiusFraction = rssiToRadiusFraction(rssi ?: RSSI_FAR)

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "CIRCLE",
                fontSize = 22.sp,
                fontFamily = GildaDisplay,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(top = 48.dp, start = 24.dp, bottom = 4.dp)
            )
            Text(
                text = "Tap their memo to say hi",
                fontFamily = Comfortaa,
                fontSize = 12.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
            )

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                var tapTargetCenter by remember { mutableStateOf(Offset.Zero) }
                var tapTargetRadiusPx by remember { mutableStateOf(0f) }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .pointerInput(tapTargetCenter, tapTargetRadiusPx) {
                            detectCircleTap(tapTargetCenter, tapTargetRadiusPx) {
                                showConfirmDialog = true
                            }
                        }
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val maxRadius = min(size.width, size.height) / 2 * 0.9f

                    // Concentric radar rings.
                    for (i in 1..3) {
                        drawCircle(
                            color = colors.accent.copy(alpha = 0.15f),
                            radius = maxRadius * (i / 3f),
                            center = center,
                            style = Stroke(width = 2f)
                        )
                    }

                    // "You" marker at the center.
                    drawCircle(color = colors.accent, radius = 18f, center = center)

                    // Other user's memo, placed by RSSI-derived distance.
                    // Fixed angle (top) since we don't have real bearing
                    // data from BLE RSSI alone — position conveys distance
                    // only, not direction.
                    val otherDistance = maxRadius * radiusFraction
                    val otherCenter = Offset(center.x, center.y - otherDistance)
                    tapTargetCenter = otherCenter
                    tapTargetRadiusPx = 24f

                    drawCircle(color = colors.accentLight, radius = 24f, center = otherCenter)
                }

                Text(
                    text = "You",
                    fontFamily = Comfortaa,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.align(Alignment.Center).padding(top = 28.dp)
                )
            }

            requestStatusText?.let {
                Text(
                    text = it,
                    fontFamily = Comfortaa,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            TextButton(onClick = onBack, modifier = Modifier.padding(start = 20.dp, bottom = 16.dp)) {
                Text("< Back", color = colors.accent, fontFamily = GildaDisplay, fontSize = 16.sp)
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = colors.surface,
            title = { Text("I found you!", color = colors.textPrimary, fontFamily = GildaDisplay, fontWeight = FontWeight.Bold) },
            text = { Text("Wanna be friends?", color = colors.textSecondary, fontFamily = Comfortaa) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    scope.launch {
                        try {
                            val response = ApiClient.service.createGoalBuddyRequest(
                                BuddyRequestRequest(targetUserId = otherUserId)
                            )
                            requestStatusText = when (response.status) {
                                "active" -> "You're Goal Buddies now! 🎉"
                                else -> "Request sent — waiting for them to accept."
                            }
                            onConnected()
                        } catch (e: Exception) {
                            requestStatusText = "Couldn't send the request. Try again."
                        }
                    }
                }) {
                    Text("Yes!", color = colors.accent, fontFamily = Comfortaa, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Not now", color = colors.textSecondary, fontFamily = Comfortaa)
                }
            }
        )
    }
}

/**
 * Minimal tap-target hit-test for the Canvas above — Compose's Canvas
 * doesn't have built-in per-shape click handling, so this checks whether
 * a tap falls within a circular radius of a given center point.
 */
private suspend fun PointerInputScope.detectCircleTap(
    center: Offset,
    radiusPx: Float,
    onHit: () -> Unit
) {
    detectTapGestures { tapOffset ->
        val dx = tapOffset.x - center.x
        val dy = tapOffset.y - center.y
        val distanceSquared = dx * dx + dy * dy
        if (distanceSquared <= radiusPx * radiusPx * 4) { // generous hit area, 2x visual radius
            onHit()
        }
    }
}