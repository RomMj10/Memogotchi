package com.example.memogotchi.ui.page

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.memogotchi.ui.theme.GildaDisplay

// COLORS
private val BgColor        = Color(0xFF16171C)
private val SurfaceColor   = Color(0xFF1F2125)
private val AccentGreen    = Color(0xFF77C59D)
private val AccentOrange   = Color(0xFFE8925A)
private val AccentBlue     = Color(0xFF6B9FD4)
private val AccentPurple   = Color(0xFFB07FD4)
private val TextPrimary    = Color(0xFFFFFFFF)
private val TextSecondary  = Color(0xFF888888)
private val TrackColor     = Color(0xFF2C2E34)
private val LockedColor    = Color(0xFF2A2B30)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PersonalityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var unlocked by remember { mutableStateOf(PersonalityStore.isUnlocked(context)) }
    var profile by remember { mutableStateOf(PersonalityStore.loadProfile(context)) }
    var dirty by remember { mutableStateOf(PersonalityStore.isDirty(context)) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        if (loading) return
        loading = true
        errorMessage = null
        scope.launch {
            val newProfile = generatePersonalityProfile(
                tagCategoryTally = PersonalityStore.loadTagCategoryTally(context),
                screenCategoryTally = PersonalityStore.loadScreenCategoryTally(context),
            )
            if (newProfile != null) {
                PersonalityStore.saveProfile(context, newProfile)
                profile = newProfile
                dirty = false
            } else {
                errorMessage = "Memo couldn't think of anything right now. Try again?"
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        if (unlocked && profile == null) refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.ArrowBack, contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp).clickable { onBack() }
            )
            Spacer(Modifier.width(12.dp))
            Text("Memo's Personality", fontFamily = GildaDisplay, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        Spacer(Modifier.height(32.dp))

        if (!unlocked) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Memo is still getting to know you.",
                    fontSize = 14.sp, color = TextSecondary
                )
            }
            return@Column
        }

        if (loading) {
            Text("Memo is thinking...", fontSize = 13.sp, color = TextSecondary)
            return@Column
        }

        if (errorMessage != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 60.dp)) {
                Text(errorMessage!!, fontSize = 13.sp, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text("Tap to retry", fontSize = 12.sp, color = AccentGreen, modifier = Modifier.clickable { refresh() })
            }
            return@Column
        }

        if (profile == null) {
            Text("Memo is thinking...", fontSize = 13.sp, color = TextSecondary)
            return@Column
        }

        val p = profile!!

        Text(p.tagline, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium)

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            p.traits.forEach { trait ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AccentGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(trait, fontSize = 12.sp, color = AccentGreen)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        p.paragraphs.forEach { para ->
            Text(para, fontSize = 13.sp, color = TextSecondary, lineHeight = 19.sp)
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceColor)
                .clickable { refresh() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("See if Memo has changed", fontSize = 12.sp, color = TextSecondary)
            if (dirty) {
                Spacer(Modifier.width(6.dp))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
            }
        }
    }
}