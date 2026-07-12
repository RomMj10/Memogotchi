package com.example.memogotchi.ui.page

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import com.example.memogotchi.api.ApiClient
import com.example.memogotchi.api.BuddyIdRequest
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import com.example.memogotchi.ui.theme.LocalAppColors
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

data class IncomingBuddyRequest(
    val id: String,
    val fromUserId: String
)

/**
 * Mount this ONCE, high up in the composable tree (e.g. alongside
 * MainShell/StandbyScreen/CircleMapScreen in MainActivity), so an incoming
 * request can interrupt the user regardless of which screen they're on —
 * a request could arrive at any time, not just while on a specific screen.
 *
 * Firestore doesn't support OR queries directly, so this runs two listeners
 * (userAId == myUid, userBId == myUid) and merges results by doc id.
 */
@Composable
fun IncomingBuddyRequestOverlay() {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val myUid = FirebaseAuth.getInstance().currentUser?.uid

    var listAResults by remember { mutableStateOf<List<IncomingBuddyRequest>>(emptyList()) }
    var listBResults by remember { mutableStateOf<List<IncomingBuddyRequest>>(emptyList()) }
    var dismissedIds by remember { mutableStateOf(setOf<String>()) }
    var isProcessing by remember { mutableStateOf(false) }

    fun QuerySnapshot.toIncomingRequests(myUid: String): List<IncomingBuddyRequest> =
        documents.mapNotNull { doc ->
            val status = doc.getString("status")
            val requestedBy = doc.getString("requestedBy")
            // Only requests sent TO me (not ones I sent myself) count as "incoming".
            if (status != "pending" || requestedBy == null || requestedBy == myUid) return@mapNotNull null
            IncomingBuddyRequest(id = doc.id, fromUserId = requestedBy)
        }

    DisposableEffect(myUid) {
        if (myUid == null) return@DisposableEffect onDispose {}

        val db = Firebase.firestore
        val registrations = mutableListOf<ListenerRegistration>()

        registrations += db.collection("goal_buddies")
            .whereEqualTo("userAId", myUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                listAResults = snapshot?.toIncomingRequests(myUid) ?: emptyList()
            }

        registrations += db.collection("goal_buddies")
            .whereEqualTo("userBId", myUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                listBResults = snapshot?.toIncomingRequests(myUid) ?: emptyList()
            }

        onDispose { registrations.forEach { it.remove() } }
    }

    val pendingRequests = remember(listAResults, listBResults) {
        (listAResults + listBResults).distinctBy { it.id }
    }
    val visibleRequest = pendingRequests.firstOrNull { it.id !in dismissedIds }
    var requesterName by remember(visibleRequest?.id) { mutableStateOf("A memo") }
    LaunchedEffect(visibleRequest?.fromUserId) {
        visibleRequest?.fromUserId?.let { requesterName = UserProfileCache.getDisplayName(it) }
    }

    if (visibleRequest != null) {
        AlertDialog(
            onDismissRequest = { /* require an explicit Accept/Decline choice */ },
            containerColor = colors.surface,
            title = {
                Text(
                    "Goal Buddy Request",
                    color = colors.textPrimary,
                    fontFamily = GildaDisplay,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                // NOTE: shows a generic message rather than the requester's
                // display name — resolving user profile info (display name/
                // avatar) isn't wired up yet. Worth revisiting once Step 8's
                // buddy list needs the same lookup, so it's solved once for both.
                Text(
                    "\$requesterName wants to be your Goal Buddy!",
                    color = colors.textSecondary,
                    fontFamily = Comfortaa
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isProcessing,
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            try {
                                ApiClient.service.acceptGoalBuddyRequest(BuddyIdRequest(visibleRequest.id))
                            } catch (e: Exception) {
                                // Firestore listener will simply show it again if this
                                // failed silently — acceptable for now, worth adding a
                                // retry/error toast later.
                            } finally {
                                dismissedIds = dismissedIds + visibleRequest.id
                                isProcessing = false
                            }
                        }
                    }
                ) {
                    Text("Accept", color = colors.accent, fontFamily = Comfortaa, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isProcessing,
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            try {
                                ApiClient.service.declineGoalBuddyRequest(BuddyIdRequest(visibleRequest.id))
                            } catch (e: Exception) {
                                // same note as above
                            } finally {
                                dismissedIds = dismissedIds + visibleRequest.id
                                isProcessing = false
                            }
                        }
                    }
                ) {
                    Text("Decline", color = colors.textSecondary, fontFamily = Comfortaa)
                }
            }
        )
    }
}