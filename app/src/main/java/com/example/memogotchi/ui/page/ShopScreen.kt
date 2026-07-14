package com.example.memogotchi.ui.page

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.memogotchi.AppTheme
import com.example.memogotchi.R
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import org.json.JSONArray


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
    @DrawableRes @RawRes val assetRes: Int,
    val isUnlocked: Boolean = false,
)
val shopCatalog: List<ShopItem> = listOf(
    ShopItem(
        id = "pet_headphone_black",
        name = "Headphone (Black)",
        cost = 40,
        category = ShopCategory.PET,
        assetRes = R.raw.pet_headphone_black
    ),
    ShopItem(
        id = "pet_scarf_red",
        name = "Red Scarf",
        cost = 80,
        category = ShopCategory.PET,
        assetRes = R.raw.pet_scarf_red
    ),
    ShopItem(
        id = "pet_tophat",
        name = "Tophat",
        cost = 80,
        category = ShopCategory.PET,
        assetRes = R.raw.pet_tophat
    ),
    ShopItem(
        id = "pet_traffic_cone",
        name = "Traffic Cone",
        cost = 80,
        category = ShopCategory.PET,
        assetRes = R.raw.pet_traffic_cone
    ),
    ShopItem(
        id = "pet_umbrella_yellow",
        name = "Yellow Umbrella Hat",
        cost = 80,
        category = ShopCategory.PET,
        assetRes = R.raw.pet_umbrella_yellow
    ),
    ShopItem(
        id = "room_livingroom",
        name = "Living Room",
        cost = 40,
        category = ShopCategory.ROOM,
        assetRes = R.raw.room_livingroom
    ),
    ShopItem(
        id = "room_library",
        name = "Library",
        cost = 80,
        category = ShopCategory.ROOM,
        assetRes = R.raw.room_library
    ),
    ShopItem(
        id = "room_office",
        name = "Office",
        cost = 100,
        category = ShopCategory.ROOM,
        assetRes = R.raw.room_office
    )
)

object ShopStore {
    private const val PREFS = "memogotchi_shop"
    private const val KEY_UNLOCKED_IDS = "unlocked_item_ids"
    private const val KEY_EQUIPPED_PREFIX = "equipped_"

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

    fun equippedItemId(ctx: Context, category: ShopCategory): String? =
        prefs(ctx).getString(KEY_EQUIPPED_PREFIX + category.name, null)

    fun setEquippedItemId(ctx: Context, category: ShopCategory, itemId: String?) {
        prefs(ctx).edit().apply {
            if (itemId == null) remove(KEY_EQUIPPED_PREFIX + category.name)
            else putString(KEY_EQUIPPED_PREFIX + category.name, itemId)
        }.apply()
    }

    fun loadAllEquipped(ctx: Context): Map<ShopCategory, String> =
        ShopCategory.entries.mapNotNull { cat -> equippedItemId(ctx, cat)?.let { cat to it } }.toMap()
}

@Composable
fun ShopScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(ShopCategory.PET) }
    var totalXp by remember { mutableStateOf(XpStore.loadXp(context)) }
    var unlockedIds by remember { mutableStateOf(ShopStore.loadUnlockedIds(context)) }
    var previewItem by remember { mutableStateOf<ShopItem?>(null) }

    val itemsForTab = remember(selectedCategory, unlockedIds) {
        shopCatalog
            .filter { it.category == selectedCategory }
            .map { it.copy(isUnlocked = it.isUnlocked || it.id in unlockedIds) }
    }

    fun purchase(item: ShopItem) {
        if (item.isUnlocked || item.id in unlockedIds) return
        if (totalXp < item.cost) return
        totalXp = XpStore.addXp(context, -item.cost)
        ShopStore.unlockItem(context, item.id)
        unlockedIds = unlockedIds + item.id
    }

    Column(modifier = Modifier.fillMaxSize().background(AppTheme.current.bg)) {

        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(8.dp))
            Text(
                "Shop", fontFamily = GildaDisplay, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, color = AppTheme.current.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, AppTheme.current.accent, RoundedCornerShape(20.dp))
                    .background(AppTheme.current.surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "$totalXp XP", fontFamily = Comfortaa, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold, color = AppTheme.current.accent
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        // ── Tabs ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AppTheme.current.surface)
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
                        fontFamily = GildaDisplay, fontSize = 15.sp, color = AppTheme.current.textSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Check back later for ${selectedCategory.label.lowercase()} items",
                        fontFamily = Comfortaa, fontSize = 12.sp, color = AppTheme.current.textSecondary
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
                        onClick = { previewItem = item }
                    )
                }
            }
        }
    }
    previewItem?.let {item ->
        ShopItemPreviewDialog(
            item = item,
            totalXp = totalXp,
            onDismiss = { previewItem = null },
            onBuy = {
                purchase(item)
                previewItem = null
            }
        )
    }
}
@Composable
private fun RowScope.ShopTabBtn(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AppTheme.current.accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(
            label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            fontFamily = Comfortaa,
            color = if (selected) Color.White else AppTheme.current.textSecondary,
            maxLines = 1
        )
    }
}
private val grayscaleColorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
private fun ShopItemCard(
    item: ShopItem,
    onClick: () -> Unit,
) {
    val borderColor = when {
        item.isUnlocked -> AppTheme.current.accent.copy(alpha = 0.5f)
        else -> AppTheme.current.textSecondary.copy(alpha = 0.2f)
    }

    Column(
        modifier = Modifier
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .background(if (item.isUnlocked) AppTheme.current.surface else AppTheme.current.surface.copy(alpha = 0.80f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(AppTheme.current.bg),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(item.assetRes),
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                colorFilter = if (item.isUnlocked) null else grayscaleColorFilter,
                modifier = Modifier.fillMaxSize()
                    .padding(6.dp)
                    .alpha(if (item.isUnlocked) 1f else 0.45f)
            )
            if (!item.isUnlocked) {
                Icon(
                    Icons.Outlined.Lock, contentDescription = "Locked",
                    tint = AppTheme.current.textSecondary, modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            item.name, fontSize = 11.sp, fontFamily = Comfortaa,
            fontWeight = FontWeight.Medium, color = AppTheme.current.textPrimary,
            maxLines = 1
        )

        Spacer(Modifier.height(2.dp))

        if (item.isUnlocked) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppTheme.current.accent.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Owned", fontSize = 9.sp, color = AppTheme.current.accent, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Text(
                "${item.cost} XP", fontSize = 10.sp, fontFamily = Comfortaa,
                color = AppTheme.current.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ShopItemPreviewDialog(
    item: ShopItem,
    totalXp: Int,
    onDismiss: () -> Unit,
    onBuy: () -> Unit,
) {
    val canAfford = totalXp >= item.cost
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(AppTheme.current.surface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppTheme.current.bg),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(item.assetRes),
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    colorFilter = if (item.isUnlocked) null else grayscaleColorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .alpha(if (item.isUnlocked) 1f else 0.45f)
                )
                if (!item.isUnlocked) {
                    Icon(
                        Icons.Outlined.Lock, contentDescription = "Locked",
                        tint = AppTheme.current.textPrimary.copy(alpha = 0.85f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                item.name, fontFamily = GildaDisplay, fontSize = 18.sp,
                fontWeight = FontWeight.Bold, color = AppTheme.current.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            when {
                item.isUnlocked -> Text(
                    "Owned", fontSize = 13.sp, color = AppTheme.current.accent,
                    fontWeight = FontWeight.SemiBold
                )
                else -> Text(
                    "${item.cost} XP", fontSize = 13.sp, fontFamily = Comfortaa,
                    color = if (canAfford) AppTheme.current.accent else AppTheme.current.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(20.dp))

            if (!item.isUnlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (canAfford) AppTheme.current.accent else AppTheme.current.textSecondary.copy(alpha = 0.5f))
                        .clickable(enabled = canAfford, onClick = onBuy)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (canAfford) "Buy for ${item.cost} XP" else "Not enough XP",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (canAfford) AppTheme.current.bg else AppTheme.current.textSecondary
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            TextButton(onClick = onDismiss) {
                Text("Close", color = AppTheme.current.textSecondary, fontSize = 13.sp)
            }
        }
    }
}