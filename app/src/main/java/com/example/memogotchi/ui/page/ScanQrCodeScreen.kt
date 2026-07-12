package com.example.memogotchi.ui.page

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.api.ApiClient
import com.example.memogotchi.api.ResolveTokenRequest
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import com.example.memogotchi.ui.theme.LocalAppColors
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

/**
 * Scans a QR code (or accepts manual entry) and resolves it via the exact
 * same resolveNearbyToken() call BleScanner.kt already makes — same match
 * flow, same push notification downstream. This screen's job ends once the
 * token is resolved; onMatched hands off the matchId to the caller.
 */
@Composable
fun ScanQrCodeScreen(
    onBack: () -> Unit = {},
    onMatched: (matchId: String) -> Unit = {}
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()

    var manualToken by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isResolving by remember { mutableStateOf(false) }

    fun resolveToken(tokenHex: String) {
        if (isResolving || tokenHex.isBlank()) return
        isResolving = true
        statusMessage = null
        scope.launch {
            try {
                val response = ApiClient.service.resolveNearbyToken(ResolveTokenRequest(tokenHex))
                if (response.matched && response.matchId != null) {
                    statusMessage = "Matched! Connecting…"
                    onMatched(response.matchId)
                } else {
                    statusMessage = when (response.reason) {
                        "token_expired" -> "This code has expired — ask your buddy to show a fresh one."
                        "token_not_found" -> "That code doesn't look right. Double-check and try again."
                        "self_scan" -> "That's your own code — have your buddy show theirs instead."
                        "already_buddies" -> "You're already connected with this buddy."
                        "cooldown_active" -> "You two matched recently — try again in a bit."
                        else -> "Couldn't connect: ${response.reason ?: "unknown error"}"
                    }
                }
            } catch (e: Exception) {
                statusMessage = "Connection failed — check your network and try again."
            } finally {
                isResolving = false
            }
        }
    }

    // zxing-android-embedded's scan Activity requests CAMERA permission
    // itself if needed; CAMERA is already declared in this project's manifest.
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { resolveToken(it) }
    }

    fun launchScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan your buddy's code")
            setBeepEnabled(false)
            setOrientationLocked(true)
        }
        scanLauncher.launch(options)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.bg).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = "SCAN A CODE",
            fontSize = 22.sp,
            fontFamily = GildaDisplay,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Scan your buddy's code to connect instantly.",
            fontSize = 13.sp,
            fontFamily = Comfortaa,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { launchScanner() },
            enabled = !isResolving,
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Camera Scanner", fontFamily = Comfortaa, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "— or type the code manually —",
            fontSize = 12.sp,
            fontFamily = Comfortaa,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = manualToken,
            onValueChange = { manualToken = it.trim() },
            label = { Text("Buddy's code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { resolveToken(manualToken) },
            enabled = !isResolving && manualToken.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect", fontFamily = Comfortaa, fontWeight = FontWeight.Bold)
        }

        if (isResolving) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(color = colors.accent)
        }

        statusMessage?.let { message ->
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                fontFamily = Comfortaa,
                color = if (message.startsWith("Matched")) colors.accent else Color(0xFFE05252),
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.weight(1f))
        TextButton(onClick = onBack) {
            Text("< Back", color = colors.accent, fontFamily = GildaDisplay, fontSize = 16.sp)
        }
    }
}