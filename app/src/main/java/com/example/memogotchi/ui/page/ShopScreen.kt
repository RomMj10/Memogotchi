package com.example.memogotchi.ui.page

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import org.json.JSONArray


private val BgColor       = Color(0xFF16171C)
private val SurfaceColor  = Color(0xFF1F2125)
private val AccentGreen   = Color(0xFF77C59D)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF888888)
private val LockedColor   = Color(0xFF2A2B30)

enum class ShopCategory(val label: String) {
    PET("Pet"),
    ROOM("Room"),
    OTHER("Other")
}
data class ShopItem(
    val id: String,
    val name: String,
    val cost: Int,
    val category: ShopCategory,
    val assetRef: String,
    val isUnlocked: Boolean = false,
)
val shopCatalog: List<ShopItem> = emptyList()

object ShopStore {
    private const val PREFS = "memogotchi_shop"
    private const val KEY_UNLOCKED_IDS = "unlocked_item_ids"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadUnlockedIds(ctx: Context): Set<String> {
        val str = prefs(ctx).getString(KEY_UNLOCKED_IDS, null) ?: return emptySet()
        return try {
            val arr = JSONArray(str)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } catch (e: Exception) { emptySet() }
    }

    fun unlockItem(ctx: Context, itemId: String) {
        val updated = loadUnlockedIds(ctx) + itemId
        val arr = JSONArray()
        updated.forEach { arr.put(it) }
        prefs(ctx).edit().putString(KEY_UNLOCKED_IDS, arr.toString()).apply()
    }

    fun isUnlocked(ctx: Context, itemId: String): Boolean = itemId in loadUnlockedIds(ctx)
}

@Composable
fun ShopScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(ShopCategory.PET) }
    var totalXp by remember { mutableStateOf(XpStore.loadXp(context)) }
    var unlockedIds by remember { mutableStateOf(ShopStore.loadUnlockedIds(context)) }

    val itemsForTab = remember(selectedCategory, unlockedIds) {
        shopCatalog
            .filter { it.category == selectedCategory }
            .map { it.copy(isUnlocked = it.isUnlocked || it.id in unlockedIds) }
    }

    fun tryPurchase(item: ShopItem) {
        if (item.isUnlocked || item.id in unlockedIds) return
        if (totalXp < item.cost) return
        totalXp = XpStore.addXp(context, -item.cost)
        ShopStore.unlockItem(context, item.id)
        unlockedIds = unlockedIds + item.id
    }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {

        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.ArrowBack, contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp).clickable { onBack() }.padding(start = 8.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Shop", fontFamily = GildaDisplay, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, AccentGreen, RoundedCornerShape(20.dp))
                    .background(SurfaceColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "$totalXp XP", fontFamily = Comfortaa, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, color = AccentGreen
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        // ── Tabs ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceColor)
                .padding(4.dp),
        ) {
            ShopCategory.entries.forEach { cat ->
                ShopTabBtn(
                    label = cat.label,
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Grid / empty state ───────────────────────────────────────────
        if (itemsForTab.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No items yet",
                        fontFamily = GildaDisplay, fontSize = 15.sp, color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Check back later for ${selectedCategory.label.lowercase()} items",
                        fontFamily = Comfortaa, fontSize = 12.sp, color = TextSecondary
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(itemsForTab, key = { it.id }) { item ->
                    ShopItemCard(
                        item = item,
                        canAfford = totalXp >= item.cost,
                        onClick = { tryPurchase(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.ShopTabBtn(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AccentGreen else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(
            label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            fontFamily = Comfortaa,
            color = if (selected) Color.White else TextSecondary,
            maxLines = 1
        )
    }
}
@Composable
private fun ShopItemCard(
    item: ShopItem,
    canAfford: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = when {
        item.isUnlocked -> AccentGreen.copy(alpha = 0.5f)
        canAfford -> TextSecondary.copy(alpha = 0.4f)
        else -> TextSecondary.copy(alpha = 0.2f)
    }

    Column(
        modifier = Modifier
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .background(if (item.isUnlocked) SurfaceColor else LockedColor)
            .clickable(enabled = !item.isUnlocked && canAfford, onClick = onClick)
            .alpha(if (!item.isUnlocked && !canAfford) 0.5f else 1f)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Asset placeholder — real rendering hooks into item.assetRef later
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BgColor),
            contentAlignment = Alignment.Center
        ) {
            if (!item.isUnlocked && !canAfford) {
                Icon(
                    Icons.Outlined.Lock, contentDescription = "Locked",
                    tint = TextSecondary, modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            item.name, fontSize = 11.sp, fontFamily = Comfortaa,
            fontWeight = FontWeight.Medium, color = TextPrimary,
            maxLines = 1
        )

        Spacer(Modifier.height(2.dp))

        if (item.isUnlocked) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AccentGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Owned", fontSize = 9.sp, color = AccentGreen, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Text(
                "${item.cost} XP", fontSize = 10.sp, fontFamily = Comfortaa,
                color = if (canAfford) AccentGreen else TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}