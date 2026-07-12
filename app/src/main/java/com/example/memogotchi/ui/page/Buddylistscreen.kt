package com.example.memogotchi.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import com.example.memogotchi.ui.theme.LocalAppColors
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore

data class BuddySummary(
    val buddyId: String,
    val otherUserId: String,
    val displayName: String
)

@Composable
fun BuddyListScreen(
    onOpenBuddy: (buddyId: String, otherUserId: String) -> Unit,
    onBack: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val myUid = FirebaseAuth.getInstance().currentUser?.uid

    // (buddyDocId, otherUserId) pairs from each side of the OR query.
    var listA by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var listB by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var summaries by remember { mutableStateOf<List<BuddySummary>>(emptyList()) }

    DisposableEffect(myUid) {
        if (myUid == null) return@DisposableEffect onDispose {}
        val db = Firebase.firestore
        val registrations = mutableListOf<ListenerRegistration>()

        registrations += db.collection("goal_buddies")
            .whereEqualTo("userAId", myUid)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, _ ->
                listA = snapshot?.documents?.mapNotNull { doc ->
                    doc.getString("userBId")?.let { other -> doc.id to other }
                } ?: emptyList()
            }

        registrations += db.collection("goal_buddies")
            .whereEqualTo("userBId", myUid)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, _ ->
                listB = snapshot?.documents?.mapNotNull { doc ->
                    doc.getString("userAId")?.let { other -> doc.id to other }
                } ?: emptyList()
            }

        onDispose { registrations.forEach { it.remove() } }
    }

    val merged = remember(listA, listB) { (listA + listB).distinctBy { it.first } }

    LaunchedEffect(merged) {
        summaries = merged.map { (buddyId, otherUid) ->
            BuddySummary(buddyId, otherUid, UserProfileCache.getDisplayName(otherUid))
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.bg).padding(top = 48.dp)) {
        Text(
            text = "GOAL BUDDIES",
            fontSize = 22.sp,
            fontFamily = GildaDisplay,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            letterSpacing = 3.sp,
            modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
        )

        if (summaries.isEmpty()) {
            Text(
                text = "No Goal Buddies yet — find one nearby!",
                fontFamily = Comfortaa,
                fontSize = 13.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 24.dp)) {
                items(summaries, key = { it.buddyId }) { buddy ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(colors.surface, RoundedCornerShape(12.dp))
                            .clickable { onOpenBuddy(buddy.buddyId, buddy.otherUserId) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buddy.displayName,
                            fontFamily = Comfortaa,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        TextButton(onClick = onBack, modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 16.dp)) {
            Text("< Back", color = colors.accent, fontFamily = GildaDisplay, fontSize = 16.sp)
        }
    }
}