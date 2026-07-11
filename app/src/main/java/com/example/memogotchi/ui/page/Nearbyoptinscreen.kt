package com.example.memogotchi.ui.page

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.R
import com.example.memogotchi.ble.NearbyModeController
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import com.example.memogotchi.ui.theme.LocalAppColors

/**
 * Returns the exact permission set this app needs to request based on API level.
 * On API 31+ (S), BLE scanning/advertising uses the dedicated Bluetooth runtime
 * permissions (with neverForLocation on the scan permission, since we only use
 * RSSI for rough proximity — not actual device location).
 * Below API 31, BLE scanning requires ACCESS_FINE_LOCATION as an OS quirk,
 * unrelated to any real location use by this app.
 */
private fun requiredNearbyPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

@Composable
fun NearbyOptInScreen(onBack: () -> Unit = {}) {
    val colors = LocalAppColors.current
    val context = LocalContext.current

    var mode by remember { mutableStateOf(NearbyStore.loadMode(context)) }
    var showPermissionDeniedMessage by remember { mutableStateOf(false) }

    // After a toggle/button press grants permission, this remembers which
    // mode to actually apply (PASSIVE for the toggle, ACTIVE for "Find Now").
    var pendingModeAfterPermission by remember { mutableStateOf(NearbyMode.PASSIVE) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            showPermissionDeniedMessage = false
            mode = pendingModeAfterPermission
            NearbyStore.saveMode(context, pendingModeAfterPermission)
            NearbyModeController.applyMode(context, pendingModeAfterPermission)
        } else {
            showPermissionDeniedMessage = true
            mode = NearbyMode.OFF
            NearbyStore.saveMode(context, NearbyMode.OFF)
            NearbyModeController.applyMode(context, NearbyMode.OFF)
        }
    }

    fun requestModeChange(target: NearbyMode) {
        pendingModeAfterPermission = target
        permissionLauncher.launch(requiredNearbyPermissions())
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bg).padding(top = 48.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            Text(
                text = "GOAL BUDDY",
                fontSize = 24.sp,
                fontFamily = GildaDisplay,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                letterSpacing = 3.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp, start = 24.dp),
                textAlign = TextAlign.Start,
            )
        }

        item {
            Text(
                text = "Goal Buddy uses Bluetooth to sense nearby Memogotchi users. " +
                        "Nothing is shared until you both confirm a connection.",
                fontSize = 13.sp,
                fontFamily = Comfortaa,
                color = colors.textSecondary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        item { SectionLabel("NEARBY DETECTION") }
        item {
            SettingsGroup {
                SettingsRow(
                    icon = R.drawable.outline_notification_important_icon,
                    title = "Notify me when a memo is nearby",
                    subtitle = when (mode) {
                        NearbyMode.OFF -> "Off"
                        NearbyMode.PASSIVE -> "On — checking periodically"
                        NearbyMode.ACTIVE -> "On — actively scanning right now"
                    },
                    trailing = {
                        SettingsSwitch(
                            checked = mode != NearbyMode.OFF,
                            onCheckedChange = { turnedOn ->
                                if (turnedOn) {
                                    requestModeChange(NearbyMode.PASSIVE)
                                } else {
                                    mode = NearbyMode.OFF
                                    NearbyStore.saveMode(context, NearbyMode.OFF)
                                    NearbyModeController.applyMode(context, NearbyMode.OFF)
                                }
                            }
                        )
                    }
                )
            }
        }

        if (mode != NearbyMode.OFF) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Button(
                        onClick = { requestModeChange(NearbyMode.ACTIVE) },
                        enabled = mode != NearbyMode.ACTIVE,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (mode == NearbyMode.ACTIVE) "Actively Scanning…" else "Find a Buddy Now",
                            fontFamily = Comfortaa,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (mode == NearbyMode.ACTIVE) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            mode = NearbyMode.PASSIVE
                            NearbyStore.saveMode(context, NearbyMode.PASSIVE)
                            NearbyModeController.applyMode(context, NearbyMode.PASSIVE)
                        }) {
                            Text("Stop Active Scan", color = colors.textSecondary, fontFamily = Comfortaa)
                        }
                    }
                }
            }
        }

        if (showPermissionDeniedMessage) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Text(
                        text = "Goal Buddy needs Bluetooth permissions to sense nearby users. " +
                                "You can grant them from app settings.",
                        fontSize = 12.sp,
                        fontFamily = Comfortaa,
                        color = Color(0xFFE05252)
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }) {
                        Text("Open App Settings", color = colors.accent, fontFamily = Comfortaa, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            TextButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp)
            ) {
                Text("< Back", color = colors.accent, fontFamily = GildaDisplay, fontSize = 16.sp)
            }
        }
    }
}