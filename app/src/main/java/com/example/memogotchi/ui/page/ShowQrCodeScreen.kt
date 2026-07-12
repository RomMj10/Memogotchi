package com.example.memogotchi.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.ble.NearbyTokenManager
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import com.example.memogotchi.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

/**
 * Displays the current nearby token — the same one BleAdvertiser would
 * broadcast — as a scannable QR code. Reuses NearbyTokenManager.ensureFreshToken()
 * as-is; QR is just an alternate delivery mechanism for the same token
 * resolveNearbyToken() already accepts. No new backend call.
 */
@Composable
fun ShowQrCodeScreen(onBack: () -> Unit = {}) {
    val colors = LocalAppColors.current
    var tokenHex by remember { mutableStateOf<String?>(null) }

    // Poll every 30s so the displayed QR refreshes if the token rotates
    // (~20 min TTL) without the user needing to do anything.
    LaunchedEffect(Unit) {
        while (true) {
            tokenHex = NearbyTokenManager.ensureFreshToken()
            delay(30_000L)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.bg).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = "MY CODE",
            fontSize = 22.sp,
            fontFamily = GildaDisplay,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Have your buddy scan this to connect instantly.",
            fontSize = 13.sp,
            fontFamily = Comfortaa,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        val currentToken = tokenHex
        Box(
            modifier = Modifier.size(260.dp).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (currentToken == null) {
                CircularProgressIndicator(color = colors.accent)
            } else {
                val bitmap = remember(currentToken) { generateQrBitmap(currentToken, 512) }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Scan this code to connect",
                    modifier = Modifier.size(240.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "This code refreshes automatically — no need to keep this screen open for long.",
            fontSize = 11.sp,
            fontFamily = Comfortaa,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.weight(1f))
        TextButton(onClick = onBack) {
            Text("< Back", color = colors.accent, fontFamily = GildaDisplay, fontSize = 16.sp)
        }
    }
}