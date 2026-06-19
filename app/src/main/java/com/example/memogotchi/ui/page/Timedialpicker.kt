package com.example.memogotchi.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.ui.theme.GildaDisplay
import kotlinx.coroutines.delay

// ════════════════════════════════════════════════════════════════════════════
//  TIME DIAL PICKER — smooth snapping scroll wheel for HH:MM
// ════════════════════════════════════════════════════════════════════════════

private val DialBg     = Color(0xFF13111A)
private val DialAccent = Color(0xFF77C59D)
private val DialDim    = Color(0xFF888888)

private const val ITEM_HEIGHT_DP = 40
private const val VISIBLE_COUNT = 3 // must be odd

@Composable
fun TimeDialPicker(
    totalMinutes: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hours = (totalMinutes / 60).coerceIn(0, 23)
    val mins  = totalMinutes % 60

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DialBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WheelColumn(
            range = 0..23,
            value = hours,
            label = "HR",
            onValueChange = { newHour -> onValueChange(newHour * 60 + mins) }
        )
        Spacer(Modifier.width(4.dp))
        Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = GildaDisplay)
        Spacer(Modifier.width(4.dp))
        WheelColumn(
            range = 0..59,
            value = mins,
            label = "MIN",
            onValueChange = { newMin -> onValueChange(hours * 60 + newMin) }
        )
    }
}

@Composable
private fun WheelColumn(
    range: IntRange,
    value: Int,
    label: String,
    onValueChange: (Int) -> Unit,
) {
    val itemCount = range.last - range.first + 1
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = value)
    var userScrolling by remember { mutableStateOf(false) }

    // Sync external value changes (e.g. programmatic resets) into the list position
    LaunchedEffect(value) {
        if (!userScrolling) {
            val current = listState.firstVisibleItemIndex
            if (current != value) {
                listState.scrollToItem(value)
            }
        }
    }

    // Detect when scrolling has settled, then snap + report the centered value
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userScrolling = true
        } else {
            val layoutInfo = listState.layoutInfo
            val itemHeightPx = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
            if (itemHeightPx > 0) {
                val firstVisible = listState.firstVisibleItemIndex
                val offset = listState.firstVisibleItemScrollOffset
                val centeredIndex = if (offset > itemHeightPx / 2) firstVisible + 1 else firstVisible
                val clamped = centeredIndex.coerceIn(0, itemCount - 1)
                delay(80)
                listState.animateScrollToItem(clamped)
                onValueChange(range.first + clamped)
            }
            userScrolling = false
        }
    }

    val totalHeight = ITEM_HEIGHT_DP * VISIBLE_COUNT
    val sidePadding = ITEM_HEIGHT_DP * (VISIBLE_COUNT / 2)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .height(totalHeight.dp)
                .width(64.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = sidePadding.dp),
            ) {
                items(itemCount) { index ->
                    val isCentered by remember {
                        derivedStateOf {
                            val layoutInfo = listState.layoutInfo
                            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                            val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
                            if (itemInfo != null) {
                                val itemCenter = itemInfo.offset + itemInfo.size / 2
                                kotlin.math.abs(itemCenter - viewportCenter) < itemInfo.size / 2
                            } else false
                        }
                    }
                    Box(
                        modifier = Modifier
                            .height(ITEM_HEIGHT_DP.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (range.first + index).toString().padStart(2, '0'),
                            fontSize = if (isCentered) 26.sp else 16.sp,
                            fontWeight = if (isCentered) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCentered) DialAccent else DialDim.copy(alpha = 0.5f),
                            fontFamily = GildaDisplay,
                        )
                    }
                }
            }

            // Fade gradient top + bottom so it reads as a wheel, not a flat list
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((ITEM_HEIGHT_DP).dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(DialBg, DialBg.copy(alpha = 0f))))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((ITEM_HEIGHT_DP).dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(DialBg.copy(alpha = 0f), DialBg)))
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 9.sp, color = DialDim, fontWeight = FontWeight.Medium)
    }
}