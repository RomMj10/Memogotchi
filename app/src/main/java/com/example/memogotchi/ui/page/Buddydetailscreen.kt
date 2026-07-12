package com.example.memogotchi.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.api.ApiClient
import com.example.memogotchi.api.BuddyIdRequest
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import com.example.memogotchi.ui.theme.LocalAppColors
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

data class BuddyEventSummary(
    val id: String,
    val status: String,
    val metricType: String,
    val targetApp: String?,
    val limitMinutes: Int?
)

@Composable
fun BuddyDetailScreen(
    buddyId: String,
    otherUserId: String,
    onCreateEvent: (buddyId: String) -> Unit = {},
    onOpenEvent: (eventId: String) -> Unit = {},
    onConnectionEnded: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()

    var displayName by remember { mutableStateOf("...") }
    var events by remember { mutableStateOf<List<BuddyEventSummary>>(emptyList()) }
    var showEndConfirm by remember { mutableStateOf(false) }
    var isEnding by remember { mutableStateOf(false) }
    var endErrorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(otherUserId) {
        displayName = UserProfileCache.getDisplayName(otherUserId)
    }

    DisposableEffect(buddyId) {
        val registration: ListenerRegistration = Firebase.firestore.collection("buddy_events")
            .whereEqualTo("buddyConnectionId", buddyId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("BuddyDetailScreen", "buddy_events listener failed", error)
                }
                events = snapshot?.documents?.mapNotNull { doc ->
                    val goalConfig = doc.get("goalConfig") as? Map<*, *> ?: return@mapNotNull null
                    BuddyEventSummary(
                        id = doc.id,
                        status = doc.getString("status") ?: "pending",
                        metricType = goalConfig["metricType"] as? String ?: "",
                        targetApp = goalConfig["targetApp"] as? String,
                        limitMinutes = (goalConfig["limitMinutes"] as? Long)?.toInt()
                    )
                } ?: emptyList()
            }
        onDispose { registration.remove() }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg).padding(top = 48.dp)) {
        Text(
            text = displayName,
            fontSize = 22.sp,
            fontFamily = GildaDisplay,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.padding(start = 24.dp, bottom = 2.dp)
        )
        Text(
            text = "Goal Buddy",
            fontFamily = Comfortaa,
            fontSize = 12.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
        )

        Button(
            onClick = { onCreateEvent(buddyId) },
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()
        ) {
            Text("Create Shared Goal", fontFamily = Comfortaa, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "SHARED GOALS",
            fontFamily = GildaDisplay,
            fontSize = 13.sp,
            color = colors.textSecondary,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        if (events.isEmpty()) {
            Text(
                text = "No shared goals yet — zero/blank state is expected here.",
                fontFamily = Comfortaa,
                fontSize = 13.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 24.dp)) {
                items(events, key = { it.id }) { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(colors.surface, RoundedCornerShape(12.dp))
                            .clickable { onOpenEvent(event.id) }
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = event.targetApp?.let { "$it · ${event.limitMinutes ?: "?"} min" }
                                    ?: event.metricType,
                                fontFamily = Comfortaa,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = event.status.replaceFirstChar { it.uppercase() },
                                fontFamily = Comfortaa,
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        endErrorText?.let {
            Text(
                text = it,
                fontFamily = Comfortaa,
                fontSize = 12.sp,
                color = Color(0xFFE05252),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        TextButton(
            onClick = { showEndConfirm = true },
            modifier = Modifier.padding(start = 20.dp)
        ) {
            Text("End Connection", color = Color(0xFFE05252), fontFamily = Comfortaa)
        }

        TextButton(onClick = onBack, modifier = Modifier.padding(start = 20.dp, bottom = 16.dp)) {
            Text("< Back", color = colors.accent, fontFamily = GildaDisplay, fontSize = 16.sp)
        }
    }

    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isEnding) showEndConfirm = false },
            containerColor = colors.surface,
            title = {
                Text("End this connection?", color = colors.textPrimary, fontFamily = GildaDisplay, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This also stops any progress sharing between you and $displayName.",
                    color = colors.textSecondary,
                    fontFamily = Comfortaa
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isEnding,
                    onClick = {
                        isEnding = true
                        endErrorText = null
                        scope.launch {
                            try {
                                ApiClient.service.endGoalBuddyConnection(BuddyIdRequest(buddyId))
                                showEndConfirm = false
                                onConnectionEnded()
                            } catch (e: Exception) {
                                endErrorText = "Couldn't end the connection. Try again."
                            } finally {
                                isEnding = false
                            }
                        }
                    }
                ) {
                    Text("End Connection", color = Color(0xFFE05252), fontFamily = Comfortaa, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(enabled = !isEnding, onClick = { showEndConfirm = false }) {
                    Text("Cancel", color = colors.textSecondary, fontFamily = Comfortaa)
                }
            }
        )
    }
}