package com.example.memogotchi.ui.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.ui.theme.GildaDisplay

// ── Tokens reused from MainActivity's established palette ───────────────
// NOTE: these mirror the private vals in MainActivity.kt. If you centralize
// your theme tokens later, replace these with shared references instead of
// duplicating the hex values.
private val BgColor       = Color(0xFF16171C)
private val SurfaceColor  = Color(0xFF1F2125)
private val AccentGreen   = Color(0xFF77C59D)
private val TextSecondary = Color(0xFF888888)
private val TextPrimary   = Color(0xFFF2F2F2)

enum class FocusGuardAction {
    BLOCK_APP,
    SET_TIME_LIMIT,
    CREATE_SCHEDULE
}

/**
 * The scrim + card shown when the FocusGuard nav button is tapped.
 *
 * This composable does NOT own the blur effect — that's applied by the
 * caller (MainShell) to the outer content Box, since the "behind" content
 * already lives earlier in MainShell's own Box stack and isn't something
 * this component has access to. This composable only renders the dimmed
 * scrim, the "What do you want to do?" header, and the three action rows.
 *
 * Call site (MainActivity.kt) is responsible for:
 *   1. Applying Modifier.blur(...) to the outer content Box when visible
 *   2. Rendering this composable as a full-screen sibling on top of that Box
 *   3. Handling onDismiss (tap outside card / system back) and onActionSelected
 */
@Composable
fun FocusGuardFabMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    onActionSelected: (FocusGuardAction) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(150))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgColor.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text(
                    text = "What do you want to do?",
                    fontFamily = GildaDisplay,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 28.dp)
                )

                FocusGuardActionItem(
                    icon = Icons.Outlined.Block,
                    label = "Block an app",
                    onClick = { onActionSelected(FocusGuardAction.BLOCK_APP) }
                )
                Spacer(Modifier.height(14.dp))
                FocusGuardActionItem(
                    icon = Icons.Outlined.Timer,
                    label = "Set a time limit",
                    onClick = { onActionSelected(FocusGuardAction.SET_TIME_LIMIT) }
                )
                Spacer(Modifier.height(14.dp))
                FocusGuardActionItem(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "Create a schedule",
                    onClick = { onActionSelected(FocusGuardAction.CREATE_SCHEDULE) }
                )
            }
        }
    }
}

@Composable
private fun FocusGuardActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                fontFamily = GildaDisplay,
                fontSize = 15.sp,
                color = TextPrimary
            )
        }
    }
}