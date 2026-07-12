package com.example.memogotchi.ui.page

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.api.ApiClient
import com.example.memogotchi.api.CreateEventRequest
import com.example.memogotchi.api.GoalConfig
import com.example.memogotchi.api.JoinEventRequest
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import com.example.memogotchi.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

private data class PickableApp(val packageName: String, val label: String)

/**
 * NOTE: this builds its own minimal app picker via PackageManager rather
 * than reusing FocusGuard's AppBlockerScreen/AppTimerScreen picker, since
 * those files weren't available to reference when this was built. If
 * FocusGuard already has a nicer/more complete app-picker component,
 * consider swapping this out for that instead — functionally this is a
 * duplicate of logic that likely already exists elsewhere in the app.
 */
private fun loadLaunchableApps(context: android.content.Context): List<PickableApp> {
    val pm = context.packageManager
    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
    }
    return pm.queryIntentActivities(intent, 0)
        .map { resolveInfo ->
            PickableApp(
                packageName = resolveInfo.activityInfo.packageName,
                label = resolveInfo.loadLabel(pm).toString()
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

private enum class GoalDuration(val apiValue: String, val label: String) {
    TODAY("today", "Today"),
    THIS_WEEK("this_week", "This Week")
}

@Composable
fun CreateEventScreen(
    buddyConnectionId: String,
    onCreated: (eventId: String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAppPicker by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<PickableApp?>(null) }
    var limitMinutesText by remember { mutableStateOf("120") }
    var duration by remember { mutableStateOf(GoalDuration.TODAY) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Post-creation consent step
    var createdEventId by remember { mutableStateOf<String?>(null) }
    var shareConsent by remember { mutableStateOf(false) }
    var isSavingConsent by remember { mutableStateOf(false) }

    val apps = remember { loadLaunchableApps(context) }

    if (createdEventId != null) {
        // Step 2: ask the creator whether THEY want to share progress too —
        // consent is separate from creating/joining the event itself.
        Column(modifier = Modifier.fillMaxSize().background(colors.bg).padding(top = 48.dp, start = 24.dp, end = 24.dp)) {
            Text(
                text = "Goal Created! 🎉",
                fontSize = 22.sp,
                fontFamily = GildaDisplay,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = "Share your progress with your buddy for this goal?",
                fontFamily = Comfortaa,
                fontSize = 14.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsSwitch(checked = shareConsent, onCheckedChange = { shareConsent = it })
                Spacer(Modifier.width(12.dp))
                Text("Sharing your progress: ${if (shareConsent) "ON" else "OFF"}", fontFamily = Comfortaa, color = colors.textPrimary)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                enabled = !isSavingConsent,
                onClick = {
                    isSavingConsent = true
                    scope.launch {
                        try {
                            ApiClient.service.joinBuddyEvent(
                                JoinEventRequest(eventId = createdEventId!!, consentedToShare = shareConsent)
                            )
                        } catch (e: Exception) {
                            // Non-fatal — event already exists; consent can be changed later
                            // from the shared goal screen (Step 10).
                        } finally {
                            isSavingConsent = false
                            onCreated(createdEventId!!)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done", fontFamily = Comfortaa, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg).padding(top = 48.dp)) {
        Text(
            text = "NEW SHARED GOAL",
            fontSize = 20.sp,
            fontFamily = GildaDisplay,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(
                start = 24.dp,
                top = 0.dp,
                end = 24.dp,
                bottom = 20.dp
            )
        )

        Column(modifier = Modifier.weight(1f).padding(horizontal = 24.dp)) {
            Text("Goal type", fontFamily = Comfortaa, fontSize = 12.sp, color = colors.textSecondary)
            Text(
                "Screen time limit",
                fontFamily = Comfortaa,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Only goal type for MVP — additional types are a Future Consideration
            // per the original feature plan, not built here.

            Text("Target app", fontFamily = Comfortaa, fontSize = 12.sp, color = colors.textSecondary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp)
                    .background(colors.surface, RoundedCornerShape(12.dp))
                    .clickable { showAppPicker = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    selectedApp?.label ?: "Choose an app…",
                    fontFamily = Comfortaa,
                    color = if (selectedApp != null) colors.textPrimary else colors.textSecondary
                )
                Text(">", color = colors.textSecondary, fontFamily = Comfortaa)
            }

            Text("Limit (minutes)", fontFamily = Comfortaa, fontSize = 12.sp, color = colors.textSecondary)
            OutlinedTextField(
                value = limitMinutesText,
                onValueChange = { new -> if (new.all { it.isDigit() } && new.length <= 4) limitMinutesText = new },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
                singleLine = true
            )

            Text("Duration", fontFamily = Comfortaa, fontSize = 12.sp, color = colors.textSecondary)
            Row(modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)) {
                GoalDuration.values().forEach { option ->
                    val selected = duration == option
                    TextButton(
                        onClick = { duration = option },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (selected) colors.accent else colors.textSecondary
                        )
                    ) {
                        Text(option.label, fontFamily = Comfortaa, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            errorText?.let {
                Text(it, color = Color(0xFFE05252), fontFamily = Comfortaa, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            Button(
                enabled = !isSubmitting && selectedApp != null && limitMinutesText.toIntOrNull() != null,
                onClick = {
                    val limit = limitMinutesText.toIntOrNull()
                    val app = selectedApp
                    if (limit == null || app == null) return@Button

                    isSubmitting = true
                    errorText = null
                    scope.launch {
                        try {
                            val response = ApiClient.service.createBuddyEvent(
                                CreateEventRequest(
                                    buddyConnectionId = buddyConnectionId,
                                    goalConfig = GoalConfig(
                                        targetApp = app.packageName,
                                        limitMinutes = limit,
                                        metricType = "screen_time_limit",
                                        duration = duration.apiValue
                                    )
                                )
                            )
                            createdEventId = response.eventId
                        } catch (e: Exception) {
                            errorText = "Couldn't create the goal. Try again."
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSubmitting) "Creating…" else "Create Goal", fontFamily = Comfortaa, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
                Text("< Back", color = colors.accent, fontFamily = GildaDisplay, fontSize = 16.sp)
            }
        }
    }

    if (showAppPicker) {
        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            containerColor = colors.surface,
            title = { Text("Choose an app", color = colors.textPrimary, fontFamily = GildaDisplay, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(apps, key = { it.packageName }) { app ->
                        Text(
                            text = app.label,
                            fontFamily = Comfortaa,
                            color = colors.textPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedApp = app
                                    showAppPicker = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppPicker = false }) {
                    Text("Close", color = colors.textSecondary, fontFamily = Comfortaa)
                }
            }
        )
    }
}