package com.example.memogotchi.focusguard

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch

/**
 * BlockInterceptOverlay — the friction screen shown when the user tries to
 * open a blocked app. This is the "Do you really want to use {appname}?"
 * screen from the original spec.
 *
 * LAUNCH CONTRACT
 * ------------------
 * Always started by FocusGuardService (see launchInterceptOverlay) with
 * three required extras: EXTRA_PACKAGE_NAME, EXTRA_APP_LABEL,
 * EXTRA_BLOCK_MODE. This Activity does not look anything up on its own —
 * FocusGuardService already resolved the rule before launching it.
 *
 * WHAT'S STUBBED HERE, DELIBERATELY
 * -------------------------------------
 * The "Unblock -1" flow (tap Unblock → shown a phrase/timer/math challenge
 * → on success, finish() and let the underlying app resume) is NOT wired up
 * yet. This file renders the question screen and both buttons, but the
 * Unblock button currently just finishes the Activity directly — it does
 * NOT yet route to the phrase/delay/math challenge screens discussed
 * earlier. That routing depends on FocusGuardStore existing (to know which
 * unblock method this specific app rule uses), which hasn't been built.
 * Wiring that in is the next step after this.
 *
 * "Nevermind" is fully real: it just finishes the Activity and returns the
 * user to their home screen (NOT back to the blocked app — finishing here
 * with no further action means the blocked app, which never finished
 * resuming, is left behind; the system will show whatever was foreground
 * before it, typically the launcher).
 */
class BlockInterceptOverlay : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "com.example.memogotchi.focusguard.EXTRA_PACKAGE_NAME"
        const val EXTRA_APP_LABEL = "com.example.memogotchi.focusguard.EXTRA_APP_LABEL"
        const val EXTRA_BLOCK_MODE = "com.example.memogotchi.focusguard.EXTRA_BLOCK_MODE"
    }

    private var launchedPackageName: String = ""

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        launchedPackageName = packageName
        val appLabel = intent.getStringExtra(EXTRA_APP_LABEL) ?: packageName
        val blockModeRaw = intent.getStringExtra(EXTRA_BLOCK_MODE) ?: BlockMode.SOFT.name
        val blockMode = runCatching { BlockMode.valueOf(blockModeRaw) }.getOrDefault(BlockMode.SOFT)


        setContent {
            var showUnblockSheet by remember { mutableStateOf(false) }
            var blockedAppRule by remember { mutableStateOf<BlockedApp?>(null) }
            val coroutineScope = rememberCoroutineScope()
            val context = this  // Activity is the Context here

            // Load the per-app rule once on launch
            LaunchedEffect(packageName) {
                val config = FocusGuardStore.loadConfig(context)
                blockedAppRule = config.blockedAppsList.firstOrNull {
                    it.packageName == packageName
                }
            }

            val rule = blockedAppRule
            if (showUnblockSheet && rule != null) {
                UnblockFlowSheet(
                    appLabel       = appLabel,
                    unblockMethod  = rule.unblockMethod,
                    phraseConfig   = rule.phraseConfig.takeIf { rule.hasPhraseConfig() },
                    delayConfig    = rule.delayConfig.takeIf  { rule.hasDelayConfig()  },
                    mathConfig     = rule.mathConfig.takeIf   { rule.hasMathConfig()   },
                    onSuccess = {
                        showUnblockSheet = false
                        FocusGuardService.grantUnblockGrace(packageName)
                        // Explicitly launch the now-unblocked app
                        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(launchIntent)
                        }
                        finish()
                    },
                    onCancel = {
                        showUnblockSheet = false
                        // intentionally NOT finish() — intercept stays up
                    }
                )
            }

            InterceptScreen(
                appLabel = appLabel,
                blockMode = blockMode,
                onUnblockClicked = {
                    if (rule != null) {
                        showUnblockSheet = true
                    } else {
                        finish() // no rule found, fall through
                    }
                },
                onNevermindClicked = {
                    // Go to the device home screen, not back into any app
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(homeIntent)
                    finish()
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Tell the service this intercept session is over regardless of
        // outcome (Nevermind, successful unblock, back gesture, swipe-away
        // from recents, process death). Without this the service only
        // re-checks on its next ~500ms tick, and if the foreground app at
        // that point is still this same blocked package, it skips
        // re-evaluating — leaving the blocked app visible and usable.
        if (launchedPackageName.isNotEmpty()) {
            FocusGuardService.clearInterceptedPackage(launchedPackageName)
        }
    }
}

private val ScrimColor    = Color(0xFF16171C)
private val WarnRed       = Color(0xFFD4537E)
private val TextPrimary   = Color(0xFFF2F2F2)
private val TextSecondary = Color(0xFF888888)

@Composable
private fun InterceptScreen(
    appLabel: String,
    blockMode: BlockMode,
    onUnblockClicked: () -> Unit,
    onNevermindClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrimColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text(
                text = "Do you really want to use $appLabel?",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (blockMode == BlockMode.HARD) {
                Text(
                    text = "This app is strictly blocked.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 28.dp)
                )
            } else {
                Spacer(Modifier.height(28.dp))
            }

            Button(
                onClick = onUnblockClicked,
                colors = ButtonDefaults.buttonColors(containerColor = WarnRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Unblock −1", color = TextPrimary, fontSize = 15.sp)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = onNevermindClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Nevermind", color = TextSecondary, fontSize = 15.sp)
            }
        }
    }
}