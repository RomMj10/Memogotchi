package com.example.memogotchi.focusguard

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Checks whether FocusGuardAccessibilityService is currently enabled by the
 * user in Android Settings. Unlike runtime permissions (which have a clean
 * ActivityResultContract), accessibility service grant status has no
 * dedicated API — the standard approach is reading the
 * ENABLED_ACCESSIBILITY_SERVICES secure setting string and checking whether
 * this service's component name appears in it.
 *
 * Call this from AppBlockerScreen before letting the user toggle hard block
 * "on" for an app — if false, route them to HardBlockSetupScreen instead of
 * silently saving a hard-block rule that won't actually enforce anything
 * (see FocusGuardAccessibilityService's class doc on the silent fallback).
 */
fun Context.isFocusGuardAccessibilityServiceEnabled(): Boolean {
    val expectedComponentName = "$packageName/${FocusGuardAccessibilityService::class.java.name}"
    val enabledServicesSetting = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServicesSetting)
    while (splitter.hasNext()) {
        if (splitter.next().equals(expectedComponentName, ignoreCase = true)) {
            return true
        }
    }
    return false
}

private val BgColor       = Color(0xFF16171C)
private val SurfaceColor  = Color(0xFF1F2125)
private val AccentGreen   = Color(0xFF77C59D)
private val TextPrimary   = Color(0xFFF2F2F2)
private val TextSecondary = Color(0xFF888888)

/**
 * Walks the user to Android's Accessibility settings so they can manually
 * enable FocusGuardAccessibilityService. This is required because hard
 * block mode does nothing without it — there is no in-app way to grant this
 * permission programmatically, the system requires the user to do it
 * through Settings directly (a deliberate Android security restriction on
 * accessibility services, since they're a powerful permission category).
 *
 * onContinue should re-check isFocusGuardAccessibilityServiceEnabled() —
 * since this screen can't know synchronously when the user finishes in
 * Settings and returns, the caller (likely AppBlockerScreen) should re-check
 * in onResume rather than relying on a callback the OS doesn't provide.
 */
@Composable
fun HardBlockSetupScreen(
    appLabelBeingBlocked: String,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "One more step for strict blocking",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "To strictly block $appLabelBeingBlocked, Memogotchi needs " +
                        "Accessibility access. This lets it actually close the app " +
                        "when you try to open it, instead of just asking nicely.",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 28.dp)
            )

            Box(
                modifier = Modifier
                    .background(SurfaceColor, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Settings → Accessibility → Memogotchi → Focus Guard → On",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Open Accessibility Settings", color = BgColor, fontSize = 15.sp)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Not now, use soft block instead", color = TextSecondary, fontSize = 14.sp)
            }
        }
    }
}