package com.example.memogotchi.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.api.ApiClient
import com.example.memogotchi.api.GoalConfig
import com.example.memogotchi.api.SetConsentRequest
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import com.example.memogotchi.ui.theme.LocalAppColors
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.platform.LocalContext
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.memogotchi.usage.UsageReportWorker

private const val AMBER_THRESHOLD = 0.8f
private const val RED_THRESHOLD = 1.0f

@Composable
fun SharedGoalScreen(
    eventId: String,
    onBack: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val myUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    var goalConfig by remember { mutableStateOf<GoalConfig?>(null) }
    var buddyConnectionId by remember { mutableStateOf<String?>(null) }
    var otherUserId by remember { mutableStateOf<String?>(null) }
    var otherDisplayName by remember { mutableStateOf("Buddy") }
    var loadError by remember { mutableStateOf<String?>(null) }

    var myConsent by remember { mutableStateOf(false) }
    var consentLoaded by remember { mutableStateOf(false) }
    var isTogglingConsent by remember { mutableStateOf(false) }

    var insightByUser by remember { mutableStateOf<Map<String, Pair<Double, Long>>>(emptyMap()) }

    // Event doc: goalConfig + buddyConnectionId.
    DisposableEffect(eventId) {
        val reg: ListenerRegistration = Firebase.firestore.collection("buddy_events")
            .document(eventId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    android.util.Log.e("SharedGoalScreen", "buddy_events listener failed", error)
                }
                if (snap != null && snap.exists()) {
                    val gc = snap.get("goalConfig") as? Map<*, *>
                    if (gc != null) {
                        goalConfig = GoalConfig(
                            targetApp = gc["targetApp"] as? String,
                            limitMinutes = (gc["limitMinutes"] as? Number)?.toInt(),
                            metricType = gc["metricType"] as? String ?: "screen_time_limit",
                            duration = gc["duration"] as? String ?: "today"
                        )
                    }
                    buddyConnectionId = snap.getString("buddyConnectionId")
                } else {
                    loadError = "This shared goal couldn't be found."
                }
            }
        onDispose { reg.remove() }
    }

    // Resolve "the other user" from the goal_buddies doc.
    LaunchedEffect(buddyConnectionId) {
        val bcId = buddyConnectionId ?: return@LaunchedEffect
        try {
            val doc = Firebase.firestore.collection("goal_buddies").document(bcId).get().await()
            val userA = doc.getString("userAId")
            val userB = doc.getString("userBId")
            val other = if (userA == myUid) userB else userA
            otherUserId = other
            if (other != null) {
                otherDisplayName = UserProfileCache.getDisplayName(other)
            }
        } catch (e: Exception) {
            // Non-fatal — buddy name just stays as the placeholder.
        }
    }

    // My own consent state for this specific event.
    DisposableEffect(eventId, myUid) {
        if (myUid.isEmpty()) return@DisposableEffect onDispose { }
        val reg = Firebase.firestore.collection("buddy_event_participants")
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("userId", myUid)
            .addSnapshotListener { snap, _ ->
                val doc = snap?.documents?.firstOrNull()
                if (doc != null) {
                    myConsent = doc.getBoolean("consentedToShare") ?: false
                    consentLoaded = true
                }
            }
        onDispose { reg.remove() }
    }

    // Live progress. consentActive filter is required — matches the Firestore
    // rule (resource.data.consentActive == true), and Firestore requires
    // collection queries to be provably scoped to what the rule allows.
    DisposableEffect(eventId) {
        val reg = Firebase.firestore.collection("insight_shares")
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("consentActive", true)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    android.util.Log.e("SharedGoalScreen", "insight_shares listener failed", error)
                }
                val latest = mutableMapOf<String, Pair<Double, Long>>()
                snap?.documents?.forEach { doc ->
                    val userId = doc.getString("userId") ?: return@forEach
                    val value = doc.getDouble("value") ?: return@forEach
                    val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    val existing = latest[userId]
                    if (existing == null || updatedAt >= existing.second) {
                        latest[userId] = value to updatedAt
                    }
                }
                insightByUser = latest
            }
        onDispose { reg.remove() }
    }

    fun fractionOf(value: Double?): Float {
        val limit = goalConfig?.limitMinutes
        if (value == null || limit == null || limit <= 0) return 0f
        return (value / limit).toFloat()
    }

    fun colorFor(fraction: Float): Color = when {
        fraction >= RED_THRESHOLD -> Color(0xFFE05252)
        fraction >= AMBER_THRESHOLD -> Color(0xFFE0A652)
        else -> Color(0xFF52C97A)
    }

    fun petMoodFor(fraction: Float): PetMood = when {
        fraction >= RED_THRESHOLD -> PetMood.ALARMED
        fraction >= AMBER_THRESHOLD -> PetMood.CONCERNED
        else -> PetMood.HAPPY
    }

    val myValue = insightByUser[myUid]?.first
    val otherValue = otherUserId?.let { insightByUser[it]?.first }
    val myFraction = fractionOf(myValue)
    val otherFraction = fractionOf(otherValue)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(top = 48.dp)
    ) {
        Text(
            text = "Shared Goal",
            fontSize = 22.sp,
            fontFamily = GildaDisplay,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)
        )

        val gc = goalConfig
        Text(
            text = gc?.let {
                val target = it.targetApp ?: it.metricType
                val when_ = if (it.duration == "this_week") "this week" else "today"
                "$target · ${it.limitMinutes ?: "?"} min · $when_"
            } ?: "Loading goal...",
            fontFamily = Comfortaa,
            fontSize = 12.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)
        )

        loadError?.let {
            Text(
                text = it,
                color = Color(0xFFE05252),
                fontFamily = Comfortaa,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                PetCard(petState = PetState(mood = petMoodFor(myFraction)), modifier = Modifier.size(120.dp))
                Text("You", fontFamily = Comfortaa, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                PetCard(petState = PetState(mood = petMoodFor(otherFraction)), modifier = Modifier.size(120.dp))
                Text(
                    text = otherDisplayName,
                    fontFamily = Comfortaa,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "PROGRESS",
            fontFamily = GildaDisplay,
            fontSize = 13.sp,
            color = colors.textSecondary,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        SharedGoalProgressRow(
            label = "You",
            minutesUsed = myValue,
            limitMinutes = gc?.limitMinutes,
            fraction = myFraction,
            color = colorFor(myFraction),
            statusText = when {
                consentLoaded && !myConsent -> "Sharing off"
                myValue == null -> "Hasn't shared today"
                else -> "${myValue.toInt()} / ${gc?.limitMinutes ?: "?"} min"
            },
            showBar = myValue != null && (!consentLoaded || myConsent)
        )

        Spacer(Modifier.height(10.dp))

        SharedGoalProgressRow(
            label = otherDisplayName,
            minutesUsed = otherValue,
            limitMinutes = gc?.limitMinutes,
            fraction = otherFraction,
            color = colorFor(otherFraction),
            statusText = if (otherValue == null) "Hasn't shared today" else "${otherValue.toInt()} / ${gc?.limitMinutes ?: "?"} min",
            showBar = otherValue != null
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(colors.surface, RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Sharing your progress", fontFamily = Comfortaa, fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 13.sp)
                Text(
                    text = if (myConsent) "ON — $otherDisplayName can see your progress" else "OFF — your progress stays private",
                    fontFamily = Comfortaa,
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
            Switch(
                checked = myConsent,
                enabled = consentLoaded && !isTogglingConsent,
                onCheckedChange = { newValue ->
                    val previous = myConsent
                    myConsent = newValue
                    isTogglingConsent = true
                    scope.launch {
                        try {
                            ApiClient.service.setEventConsent(SetConsentRequest(eventId, newValue))
                        } catch (e: Exception) {
                            myConsent = previous
                        } finally {
                            isTogglingConsent = false
                        }
                    }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = colors.accent)
            )
        }

        Spacer(Modifier.weight(1f))

        TextButton(onClick = onBack, modifier = Modifier.padding(start = 20.dp, bottom = 16.dp)) {
            Text("< Back", color = colors.accent, fontFamily = GildaDisplay, fontSize = 16.sp)
        }
    }
}

@Composable
private fun SharedGoalProgressRow(
    label: String,
    minutesUsed: Double?,
    limitMinutes: Int?,
    fraction: Float,
    color: Color,
    statusText: String,
    showBar: Boolean
) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontFamily = Comfortaa, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, fontSize = 13.sp)
            Text(statusText, fontFamily = Comfortaa, fontSize = 12.sp, color = colors.textSecondary)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(colors.textSecondary.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
        ) {
            if (showBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(color, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}