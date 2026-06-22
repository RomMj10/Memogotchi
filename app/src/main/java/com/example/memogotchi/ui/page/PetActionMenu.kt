package com.example.memogotchi.ui.page


import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import kotlin.math.cos
import kotlin.math.sin

// ── Local palette (mirrors PetScreen.kt) ────────────────────────────────────
private val BgColorHex      = Color(0xFF16171C)
private val SurfaceColorHex = Color(0xFF1F2125)
private val AccentHex       = Color(0xFF77C59D)
private val TextLightHex    = Color(0xFF98ABA0)
private val TextDimHex      = Color(0xFF68736C)

data class HexMenuItem(
    val icon: ImageVector,
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit = {},
)


@Composable
fun PetHexFabMenu(
    expanded: Boolean,
    items: List<HexMenuItem>,
    modifier: Modifier = Modifier,
    radius: Dp = 118.dp,
    startAngleDeg: Float = -90f,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        items.take(6).forEachIndexed { i, item ->
            val angleDeg = startAngleDeg + i * 60f
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val targetX  = (radius.value * cos(angleRad)).toFloat()
            val targetY  = (radius.value * sin(angleRad)).toFloat()

            val transition = updateTransition(expanded, label = "hex_$i")
            val offsetX by transition.animateDp(
                transitionSpec = {
                    if (targetState) spring(dampingRatio = 0.62f, stiffness = 210f) else tween(150)
                },
                label = "hexX_$i"
            ) { isExpanded -> if (isExpanded) targetX.dp else 0.dp }

            val offsetY by transition.animateDp(
                transitionSpec = {
                    if (targetState) spring(dampingRatio = 0.62f, stiffness = 210f) else tween(150)
                },
                label = "hexY_$i"
            ) { isExpanded -> if (isExpanded) targetY.dp else 0.dp }

            val itemAlpha by transition.animateFloat(
                transitionSpec = { tween(if (targetState) 180 + i * 35 else 100) },
                label = "hexAlpha_$i"
            ) { if (it) 1f else 0f }

            val itemScale by transition.animateFloat(
                transitionSpec = { tween(if (targetState) 180 + i * 35 else 100) },
                label = "hexScale_$i"
            ) { if (it) 1f else 0.4f }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .alpha(itemAlpha)
                    .scale(itemScale)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceColorHex)
                        .border(
                            width = 1.dp,
                            color = if (item.enabled) AccentHex.copy(alpha = 0.55f) else TextDimHex,
                            shape = CircleShape
                        )
                        .clickable(enabled = item.enabled && expanded) { item.onClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (item.enabled) AccentHex else TextDimHex,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.label,
                    fontSize = 9.sp,
                    fontFamily = Comfortaa,
                    color = if (item.enabled) TextLightHex else TextDimHex,
                )
            }
        }
    }
}

/**
 * Small modal panel listing a few analog tasks, popped up when the "Tasks"
 * hex item is tapped. Tapping outside the card dismisses it.
 */
@Composable
fun MiniTaskPanel(
    tasks: List<AnalogTask>,
    onDismiss: () -> Unit,
    onViewAll: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onDismiss() }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceColorHex)
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Today's Analog Tasks",
                        fontFamily = GildaDisplay,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6FCFF)
                    )
                }

                Spacer(Modifier.height(14.dp))

                if (tasks.isEmpty()) {
                    Text(
                        "No tasks yet — check back after a bit more screen time.",
                        fontFamily = Comfortaa,
                        fontSize = 12.sp,
                        color = TextLightHex,
                        lineHeight = 17.sp,
                    )
                } else {
                    tasks.take(3).forEach { task ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Text(task.category.emoji, fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    task.title,
                                    fontSize = 13.sp,
                                    fontFamily = Comfortaa,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                                    color = if (task.isDone) TextLightHex else Color(0xFFE6FCFF)
                                )
                                Text(
                                    "${task.durationMinutes} min · ${task.triggerReason}",
                                    fontSize = 10.sp,
                                    fontFamily = Comfortaa,
                                    color = TextLightHex,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentHex)
                        .clickable { onViewAll() }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "View all tasks",
                        fontFamily = Comfortaa,
                        fontWeight = FontWeight.SemiBold,
                        color = BgColorHex,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}