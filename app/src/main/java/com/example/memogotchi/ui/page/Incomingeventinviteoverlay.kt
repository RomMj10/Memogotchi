package com.example.memogotchi.ui.page

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.api.ApiClient
import com.example.memogotchi.api.JoinEventRequest
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import com.example.memogotchi.ui.theme.LocalAppColors
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class EventInvite(
    val eventId: String,
    val targetApp: String?,
    val limitMinutes: Int?,
    val duration: String?
)

/**
 * Mount this ONCE, high up in the tree (same pattern as
 * IncomingBuddyRequestOverlay from Step 7) — an event invite can arrive
 * at any time regardless of the current screen.
 *
 * Firestore has no "doesn't have a participant doc" query, so this:
 *  1. Tracks the user's active buddy connection ids (small listener pair,
 *     same OR-via-two-queries pattern as BuddyListScreen).
 *  2. Listens to buddy_events where status == "pending" and
 *     buddyConnectionId whereIn <those ids> (capped at 10 — Firestore's
 *     whereIn limit; fine for MVP, would need chunking past 10 active
 *     buddies with simultaneous pending invites, an unlikely edge case).
 *  3. For each candidate event, does a one-off check for whether the
 *     current user already has a buddy_event_participants doc — if not,
 *     it's a genuine incoming invite. This check is NOT live/reactive
 *     (single get(), refreshed only when the candidate event list
 *     changes) — acceptable for MVP since joining happens through this
 *     same overlay anyway.
 */
@Composable
fun IncomingEventInviteOverlay() {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val myUid = FirebaseAuth.getInstance().currentUser?.uid

    var activeBuddyIdsA by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeBuddyIdsB by remember { mutableStateOf<List<String>>(emptyList()) }
    var candidateEvents by remember { mutableStateOf<List<EventInvite>>(emptyList()) }
    var dismissedIds by remember { mutableStateOf(setOf<String>()) }
    var consentChoice by remember { mutableStateOf(false) }
    var isJoining by remember { mutableStateOf(false) }

    val activeBuddyIds = remember(activeBuddyIdsA, activeBuddyIdsB) {
        (activeBuddyIdsA + activeBuddyIdsB).distinct().take(10)
    }

    DisposableEffect(myUid) {
        if (myUid == null) return@DisposableEffect onDispose {}
        val db = Firebase.firestore
        val registrations = mutableListOf<ListenerRegistration>()

        registrations += db.collection("goal_buddies")
            .whereEqualTo("userAId", myUid)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snap, _ -> activeBuddyIdsA = snap?.documents?.map { it.id } ?: emptyList() }

        registrations += db.collection("goal_buddies")
            .whereEqualTo("userBId", myUid)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snap, _ -> activeBuddyIdsB = snap?.documents?.map { it.id } ?: emptyList() }

        onDispose { registrations.forEach { it.remove() } }
    }

    DisposableEffect(activeBuddyIds, myUid) {
        if (myUid == null || activeBuddyIds.isEmpty()) {
            candidateEvents = emptyList()
            return@DisposableEffect onDispose {}
        }
        val db = Firebase.firestore
        val registration = db.collection("buddy_events")
            .whereEqualTo("status", "pending")
            .whereIn("buddyConnectionId", activeBuddyIds)
            .addSnapshotListener { snapshot, _ ->
                val docs = snapshot?.documents ?: return@addSnapshotListener
                scope.launch {
                    val invites = docs.mapNotNull { doc ->
                        val alreadyJoined = try {
                            db.collection("buddy_event_participants")
                                .whereEqualTo("eventId", doc.id)
                                .whereEqualTo("userId", myUid)
                                .get()
                                .await()
                                .isEmpty
                                .not()
                        } catch (e: Exception) {
                            true // fail safe: don't show an invite if we can't confirm it's new
                        }
                        if (alreadyJoined) return@mapNotNull null

                        val goalConfig = doc.get("goalConfig") as? Map<*, *> ?: return@mapNotNull null
                        EventInvite(
                            eventId = doc.id,
                            targetApp = goalConfig["targetApp"] as? String,
                            limitMinutes = (goalConfig["limitMinutes"] as? Long)?.toInt(),
                            duration = goalConfig["duration"] as? String
                        )
                    }
                    candidateEvents = invites
                }
            }
        onDispose { registration.remove() }
    }

    val visibleInvite = candidateEvents.firstOrNull { it.eventId !in dismissedIds }

    if (visibleInvite != null) {
        AlertDialog(
            onDismissRequest = { /* require an explicit choice */ },
            containerColor = colors.surface,
            title = {
                Text("Shared Goal Invite", color = colors.textPrimary, fontFamily = GildaDisplay, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Your buddy wants to team up on: ${visibleInvite.targetApp ?: "a goal"}" +
                                (visibleInvite.limitMinutes?.let { " · ${it} min" } ?: "") +
                                (visibleInvite.duration?.let { " · ${it.replace('_', ' ')}" } ?: ""),
                        color = colors.textSecondary,
                        fontFamily = Comfortaa
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsSwitch(checked = consentChoice, onCheckedChange = { consentChoice = it })
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Share your progress too",
                            fontFamily = Comfortaa,
                            fontSize = 13.sp,
                            color = colors.textPrimary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isJoining,
                    onClick = {
                        isJoining = true
                        scope.launch {
                            try {
                                ApiClient.service.joinBuddyEvent(
                                    JoinEventRequest(eventId = visibleInvite.eventId, consentedToShare = consentChoice)
                                )
                            } catch (e: Exception) {
                                // listener will just show it again on next composition if this failed
                            } finally {
                                dismissedIds = dismissedIds + visibleInvite.eventId
                                consentChoice = false
                                isJoining = false
                            }
                        }
                    }
                ) {
                    Text("Join", color = colors.accent, fontFamily = Comfortaa, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isJoining,
                    onClick = {
                        // Session-only dismissal — there's no "declined" state
                        // stored anywhere, so this invite would reappear if the
                        // overlay remounts (e.g. app restart) unless joined.
                        dismissedIds = dismissedIds + visibleInvite.eventId
                        consentChoice = false
                    }
                ) {
                    Text("Not now", color = colors.textSecondary, fontFamily = Comfortaa)
                }
            }
        )
    }
}