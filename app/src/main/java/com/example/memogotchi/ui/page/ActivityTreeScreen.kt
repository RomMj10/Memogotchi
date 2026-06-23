package com.example.memogotchi.ui.page


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BgColor       = Color(0xFF16171C)
private val SurfaceColor  = Color(0xFF1F2125)
private val AccentGreen   = Color(0xFF77C59D)
private val TrunkColor    = Color(0xFF44503F)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF888888)

private fun branchColor(cat: AppCategory) = when (cat) {
    AppCategory.SOCIAL        -> Color(0xFF6B9FD4)
    AppCategory.GAMES         -> Color(0xFFB07FD4)
    AppCategory.ENTERTAINMENT -> Color(0xFFE8925A)
    AppCategory.BROWSER       -> Color(0xFF77C59D)
    AppCategory.PRODUCTIVITY  -> Color(0xFFD4C56B)
    AppCategory.OTHER         -> AccentGreen
}

private const val NODE_SPACING_DP = 110
private const val SIDE_OFFSET_DP  = 90

@Composable
fun ActivityTreeScreen(
    completedTasks: List<CompletedTaskRecord>,
    onBack: () -> Unit = {},
) {
    val tasks = completedTasks.sortedBy { it.completedAtMs }
    val density = LocalDensity.current
    val nodeSpacingPx = with(density) { NODE_SPACING_DP.dp.toPx() }
    val sideOffsetPx  = with(density) { SIDE_OFFSET_DP.dp.toPx() }
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(Modifier.width(4.dp))
            Column {
                Text("Activity Tree", fontFamily = GildaDisplay, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${tasks.size} completed tasks", fontFamily = Comfortaa, fontSize = 11.sp, color = TextSecondary)
            }
        }

        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(48.dp))
                    Text("Your tree hasn't sprouted yet", fontFamily = Comfortaa, fontSize = 13.sp, color = TextSecondary)
                    Text("Complete analog tasks to grow it", fontFamily = Comfortaa, fontSize = 12.sp, color = TextSecondary)
                }
            }
            return
        }

        val totalHeightDp = (tasks.size * NODE_SPACING_DP + 160).dp

        LaunchedEffect(tasks.size) {
            scrollState.animateScrollTo(Int.MAX_VALUE)
        }

        Box(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Box(modifier = Modifier.fillMaxWidth().height(totalHeightDp)) {

                Canvas(modifier = Modifier.matchParentSize()) {
                    val centerX = size.width / 2
                    val bottomY = size.height - 40.dp.toPx()

                    drawLine(
                        color = TrunkColor,
                        start = Offset(centerX, bottomY),
                        end   = Offset(centerX, bottomY - tasks.size * nodeSpacingPx),
                        strokeWidth = 10f,
                    )

                    tasks.forEachIndexed { i, task ->
                        val y = bottomY - (i + 1) * nodeSpacingPx
                        val left = i % 2 == 0
                        val nodeX = if (left) centerX - sideOffsetPx else centerX + sideOffsetPx
                        drawLine(
                            color = branchColor(task.category),
                            start = Offset(centerX, y + nodeSpacingPx * 0.3f),
                            end   = Offset(nodeX, y),
                            strokeWidth = 6f,
                            pathEffect = PathEffect.cornerPathEffect(80f),
                        )
                    }
                }

                tasks.forEachIndexed { i, task ->
                    val yPx =
                        with(density) { totalHeightDp.toPx() - 40.dp.toPx() - (i + 1) * nodeSpacingPx }
                    val yDp = with(density) { yPx.toDp() }
                    val left = i % 2 == 0


                    Box(
                        modifier = Modifier
                            .align(if (left) Alignment.TopStart else Alignment.TopEnd)
                            .offset(x = if (left) 8.dp else (-8).dp, y = yDp - 26.dp)
                            .border(2.dp, branchColor(task.category), RoundedCornerShape(14.dp))
                            .widthIn(max = 150.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceColor)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(task.category.emoji, fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    task.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    fontFamily = GildaDisplay
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                SimpleDateFormat(
                                    "MMM d",
                                    Locale.ENGLISH
                                ).format(Date(task.completedAtMs)),
                                fontSize = 9.sp, color = TextSecondary
                            )
                        }
                    }
                }


                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccentGreen)
                ) {
                    Icon(
                        Icons.Outlined.PersonOutline,
                        contentDescription = "Sprout",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp).align(Alignment.Center)
                    )
                }
            }
        }
    }
}