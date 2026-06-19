package com.example.memogotchi.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ════════════════════════════════════════════════════════════════════════════
//  GROUPED TAG PICKER — shared between diary entries and goals
// ════════════════════════════════════════════════════════════════════════════

private const val TAGS_PER_ROW = 3

@Composable
fun GroupedTagPicker(
    selectedTags: List<String>,
    onToggleTag: (String) -> Unit,
    accentColor: Color,
    textSecondaryColor: Color,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        tagCategories.forEachIndexed { index, category ->
            Text(
                category.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textSecondaryColor,
            )
            Spacer(Modifier.height(6.dp))

            category.tags.chunked(TAGS_PER_ROW).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { tag ->
                        key(tag) {
                            val selected = tag in selectedTags
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(
                                        1.dp,
                                        if (selected) accentColor else textSecondaryColor.copy(alpha = 0.3f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .background(if (selected) accentColor.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { onToggleTag(tag) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    tag,
                                    fontSize = 12.sp,
                                    color = if (selected) accentColor else textSecondaryColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    // Fill remaining slots in the last row so tags don't stretch wider than intended
                    repeat(TAGS_PER_ROW - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (index != tagCategories.lastIndex) {
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}