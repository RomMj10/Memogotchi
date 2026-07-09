package com.example.memogotchi.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.AppTheme
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeMenu(
    onDismiss: () -> Unit,
    onEquippedChanged: () -> Unit = {},
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(ShopCategory.PET)}
    var unlockIds by remember { mutableStateOf(ShopStore.loadUnlockedIds(context))}
    var equippedByCategory by remember { mutableStateOf(ShopStore.loadAllEquipped(context)) }
    var previewItem by remember { mutableStateOf<ShopItem?>(null)}

    val itemsForTab = remember(selectedCategory, unlockIds) {
        shopCatalog.filter { it.category == selectedCategory }
    }

    fun equip(item: ShopItem) {
        val current = equippedByCategory[item.category]
        val newId = if (current == item.id) null else item.id
        ShopStore.setEquippedItemId(context, item.category, newId)
        equippedByCategory = ShopStore.loadAllEquipped(context)
        onEquippedChanged()
    }

    ModalBottomSheet(
        onDismissRequest =  onDismiss,
        containerColor = AppTheme.current.bg
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "Wardrobe",
                fontFamily = GildaDisplay, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = AppTheme.current.textPrimary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppTheme.current.surface)
                    .padding(4.dp),
            ) {
                ShopCategory.entries.forEach { cat ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedCategory == cat) AppTheme.current.accent else Color.Transparent)
                            .clickable { selectedCategory = cat }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            cat.label, fontSize = 12.sp, fontFamily = Comfortaa,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedCategory == cat) Color.White else AppTheme.current.textSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (itemsForTab.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No items in this category yet", color = AppTheme.current.textSecondary, fontSize = 12.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 320.dp)
                ) {
                    items(itemsForTab, key = { it.id }) { item ->
                        val unlocked = item.id in unlockIds
                        val equipped = equippedByCategory[item.category] == item.id
                        WardrobeItemCard(
                            item = item,
                            unlocked = unlocked,
                            equipped = equipped,
                            onClick = { if (unlocked) previewItem = item }
                        )
                    }
                }
            }
        }
    }

    previewItem?.let { item ->
        val equipped = equippedByCategory[item.category] == item.id
        WardrobePreviewDialog(
            item = item,
            isEquipped = equipped,
            onDismiss = { previewItem = null },
            onEquip = {
                equip(item)
                previewItem = null
            }
        )
    }
}

@Composable
private fun WardrobeItemCard(
    item: ShopItem,
    unlocked: Boolean,
    equipped: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = when {
        equipped -> AppTheme.current.accent
        unlocked -> AppTheme.current.textSecondary.copy(alpha = 0.3f)
        else -> AppTheme.current.textSecondary.copy(alpha = 0.15f)
    }
    Column(
        modifier = Modifier
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(14.dp))
            .border(if (equipped) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .background(AppTheme.current.surface)
            .clickable(enabled = unlocked, onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AppTheme.current.bg),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(item.assetRes),
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(6.dp)
                    .alpha(if (unlocked) 1f else 0.35f)
            )
            if (!unlocked) {
                Icon(Icons.Outlined.Lock, contentDescription = "Locked", tint = AppTheme.current.textSecondary, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(item.name, fontSize = 10.sp, fontFamily = Comfortaa, color = AppTheme.current.textPrimary, maxLines = 1)
        if (equipped) {
            Spacer(Modifier.height(2.dp))
            Text("Equipped", fontSize = 9.sp, color = AppTheme.current.accent, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WardrobePreviewDialog(
    item: ShopItem,
    isEquipped: Boolean,
    onDismiss: () -> Unit,
    onEquip: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(AppTheme.current.surface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(160.dp).clip(RoundedCornerShape(16.dp)).background(AppTheme.current.bg),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(item.assetRes),
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(item.name, fontFamily = GildaDisplay, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.current.textPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (isEquipped) AppTheme.current.textSecondary.copy(alpha = 0.4f) else AppTheme.current.accent)
                    .clickable(onClick = onEquip)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isEquipped) "Unequip" else "Equip",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = if (isEquipped) AppTheme.current.textPrimary else AppTheme.current.bg
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppTheme.current.textSecondary, fontSize = 13.sp)
            }
        }
    }
}







