package com.example.memogotchi.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.R
import com.example.memogotchi.ui.theme.ColorPresets
import com.example.memogotchi.ui.theme.Comfortaa
import com.example.memogotchi.ui.theme.GildaDisplay
import com.example.memogotchi.ui.theme.LocalAppColors

private val Ncolors = LocalAppColors
private val DividerColor  = Color(0xFF2C2E34)



@Composable
fun SettingsScreen(today: DayData? = null,
                   currentPetName: String = "",
                   onPetRenamed: (String) -> Unit = {},
                   onOpenGoalBuddy: () -> Unit = {},
                   onOpenBuddyList: () -> Unit = {}) {

    val colors = LocalAppColors.current
    val context = LocalContext.current

    var showRenamePetDialog by remember { mutableStateOf(false) }

    var monochromeUI by remember { mutableStateOf(false) }
    var healthAlerts by remember { mutableStateOf(true) }
    var textSize by remember { mutableStateOf(AppSettings.textSize) }
    var dailyLimitMinutes by remember { mutableStateOf(AppSettings.dailyLimitMinutes) }

    var largeText by remember { mutableStateOf(false) }
    var reduceMotion by remember { mutableStateOf(false) }

    var showDailyLimitDialog by remember { mutableStateOf(false) }

    val totalMs   = today?.totalMs ?: 0L
    val limitMs   = dailyLimitMinutes * 60_000L
    val remainingMs = (limitMs - totalMs).coerceAtLeast(0L)
    val limitSubtitle = if (totalMs >= limitMs) {
        "Limit reached (${formatMs(limitMs)} limit)"
    } else {
        "${formatMs(remainingMs)} remaining of ${formatMs(limitMs)}"
    }


    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.bg).padding(top = 48.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            Text(
                text       = "SETTINGS",
                fontSize   = 24.sp,
                fontFamily = GildaDisplay,
                fontWeight = FontWeight.Bold,
                color      = colors.textPrimary,
                letterSpacing = 3.sp,
                modifier   = Modifier.fillMaxWidth().padding(bottom = 20.dp, start = 24.dp),
                textAlign  = TextAlign.Start,
            )
        }

        item { SectionLabel("FOCUS CONTROLS") }
        item {
            SettingsGroup {
                SettingsRow(
                    icon        = R.drawable.outline_av_timer_icon,
                    title       = "Daily Limit",
                    subtitle    = limitSubtitle,
                    showChevron = true,
                    onClick     = { showDailyLimitDialog = true }
                )
                RowDivider()
                SettingsRow(
                    icon        = R.drawable.memogotchi_vector,
                    title       = "My Goal Buddies",
                    subtitle    = "View and manage your connections",
                    showChevron = true,
                    onClick     = onOpenBuddyList
                )
                RowDivider()
            }
        }

        // ── Goal Buddy ───────────────────────────────────────────────────
        item { SectionLabel("GOAL BUDDY") }
        item {
            SettingsGroup {
                SettingsRow(
                    icon        = R.drawable.memogotchi_vector,
                    title       = "Connect with a Goal Buddy",
                    subtitle    = "Sense nearby users and share goals together",
                    showChevron = true,
                    onClick     = onOpenGoalBuddy
                )
            }
        }

        // ── Pet & Theme ──────────────────────────────────────────────────
        item { SectionLabel("PET & THEME") }
        item {
            SettingsGroup {
                SettingsRow(
                    icon        = R.drawable.memogotchi_vector,
                    title = "Rename Pet",
                    subtitle = currentPetName.ifBlank {"Tap to set a name"},
                    showChevron = true,
                    onClick = { showRenamePetDialog = true}
                )
                RowDivider()
                SettingsRow(
                    icon     = R.drawable.outline_contrast_icon,
                    title    = "Monochrome UI",
                    subtitle = "High contrast mode",
                    trailing = {
                        SettingsSwitch(checked = monochromeUI, onCheckedChange = { monochromeUI = it })
                    }
                )
                RowDivider()
                SettingsRow(
                    icon     = R.drawable.outline_notification_important_icon,
                    title    = "Health Alerts",
                    subtitle = "Notifies your health from Memogotchi",
                    trailing = {
                        SettingsSwitch(checked = healthAlerts,
                            onCheckedChange = {
                                healthAlerts = it
                                AppSettings.setHealthAlertsEnabled(context, it)
                            })
                    }
                )
                RowDivider()
                Text("App Theme", fontSize = 14.sp, color = colors.textPrimary, fontFamily = Comfortaa, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 10.dp))
                ColorSlotPicker("Background", ColorPresets.bg, AppSettings.bgIndex) {
                    AppSettings.bgIndex = it
                    AppSettings.setColorIndex(context, "bg_index", it)
                }
                ColorSlotPicker("Surface", ColorPresets.surface, AppSettings.surfaceIndex) {
                    AppSettings.surfaceIndex = it
                    AppSettings.setColorIndex(context, "surface_index", it)
                }
                ColorSlotPicker("Accent", ColorPresets.accent, AppSettings.accentIndex) {
                    AppSettings.accentIndex = it
                    AppSettings.setColorIndex(context, "accent_index", it)
                }
                ColorSlotPicker("Text Primary", ColorPresets.textPrimary, AppSettings.textPrimaryIndex) {
                    AppSettings.textPrimaryIndex = it
                    AppSettings.setColorIndex(context, "text_primary_index", it)
                }
                ColorSlotPicker("Text Secondary", ColorPresets.textSecondary, AppSettings.textSecondaryIndex) {
                    AppSettings.textSecondaryIndex = it
                    AppSettings.setColorIndex(context, "text_secondary_index", it)
                }
            }
        }

        // ── Accessibility ────────────────────────────────────────────────
        item { SectionLabel("ACCESSIBILITY") }
        item {
            SettingsGroup {
                SettingsRow(
                    icon     = R.drawable.outline_format_size_icon,
                    title    = "Text Size",
                    subtitle = "",
                    columnStyle = true,
                    trailing = {
                        TextSizeSelector(
                            currentSize = textSize,
                            onSelect = {
                                textSize = it
                                AppSettings.setTextSize(context, it)
                            }
                        )
                    }
                )
                RowDivider()
                SettingsRow(
                    icon     = R.drawable.outline_mobile_vibrate_icon,
                    title    = "Reduce Motion",
                    subtitle = "Limit animations and transitions",
                    trailing = {
                        SettingsSwitch(checked = reduceMotion, onCheckedChange = { reduceMotion = it })
                    }
                )
            }
        }

        // ── Data ─────────────────────────────────────────────────────────
        item { SectionLabel("DATA") }
        item {
            SettingsGroup {
                SettingsRow(
                    icon        = R.drawable.outline_file_export_icon,
                    title       = "Export Data",
                    subtitle    = null,
                    showChevron = true,
                    onClick     = { /* TODO: export data */ }
                )
                RowDivider()
                SettingsRow(
                    icon        = R.drawable.outline_file_remove_icon,
                    title       = "Clear All Data",
                    subtitle    = null,
                    titleColor  = Color(0xFFE05252),
                    showChevron = true,
                    onClick     = { /* TODO: confirm + clear data */ }
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showDailyLimitDialog) {
        DailyLimitDialog(
            currentMinutes = dailyLimitMinutes,
            onDismiss = {showDailyLimitDialog = false},
            onConfirm = {
                minutes ->
                dailyLimitMinutes = minutes
                AppSettings.setDailyLimitMinutes(context, minutes)
                showDailyLimitDialog = false
            }
        )
    }
    if (showRenamePetDialog) {
        RenamePetDialog(
            currentName = currentPetName,
            onDismiss = { showRenamePetDialog = false },
            onConfirm = { newName ->
                MemoStore.saveName(context, newName)
                onPetRenamed(newName)
                showRenamePetDialog = false
            }
        )
    }
}
//daily limit dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLimitDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var sliderValue by remember { mutableStateOf(currentMinutes.toFloat()) }
    val snappedMinutes = (sliderValue / 15).toInt() * 15
    val colors = LocalAppColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text("Daily Limit", color = colors.textPrimary, fontFamily = GildaDisplay, fontWeight = FontWeight.Bold)
                },
        text = {
            Column {
                Text(
                    text = formatMs(snappedMinutes * 60_000L),
                    fontFamily = Comfortaa,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                )
                Spacer(Modifier.height(12.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 30f..480f,
                    steps = ((480 - 30) / 15) - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accent,
                        activeTrackColor = colors.accent,
                        inactiveTrackColor = Color(0xFF2C2E34),
                    )
                )
                Text("30 min - 8 hrs, in 15-min steps", fontFamily = Comfortaa, fontSize = 11.sp, color = colors.textSecondary)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(snappedMinutes) }) {
                Text("Save",fontFamily = Comfortaa, color = colors.accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel",fontFamily = Comfortaa, color = colors.textSecondary)
            }
        }
    )
}
//text size selector
@Composable
fun TextSizeSelector(currentSize: TextSizeOption, onSelect: (TextSizeOption) -> Unit) {
    val colors = LocalAppColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TextSizeOption.entries.forEach{ option ->
            val selected = option == currentSize
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if(selected) colors.accent else Color(0xFF2C2E34))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = option.label,
                    fontSize = 11.sp,
                    color = if (selected) Color.White else colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenamePetDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    var name by remember { mutableStateOf(currentName)}
    var error by remember { mutableStateOf<String?>(null)}
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text("Rename Pet", color = colors.textPrimary, fontFamily = GildaDisplay, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= 24) name = it
                        error = null
                    },
                    placeholder = { Text("Tap to rename a pet", color = colors.textSecondary) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 15.sp, color = colors.textPrimary, fontFamily = Comfortaa),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.bg,
                        unfocusedContainerColor = colors.bg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    )
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error!!, color = Color(0xFFE05252), fontSize = 12.sp, fontFamily = Comfortaa)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = name.trim()
                if (trimmed.isEmpty()) {
                    error = "Name can't be empty"
                } else {
                    onConfirm(trimmed)
                }
            }) {
                Text("Rename", fontFamily = Comfortaa, color = colors.accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontFamily = Comfortaa, color = colors.textSecondary)
            }
        }
    )
}

//label sections

@Composable
fun SectionLabel(text: String) {
    val colors = LocalAppColors.current
    Text(
        text       = text,
        fontFamily = GildaDisplay,
        fontSize   = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color      = colors.textSecondary,
        letterSpacing = 1.8.sp,
        modifier   = Modifier.padding(start = 20.dp, top = 32.dp, bottom = 10.dp)
    )
}

//group

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = colors.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 56.dp)
            .background(DividerColor)
    )
}

//row

@Composable
fun SettingsRow(
    icon: Int,
    title: String,
    subtitle: String?,
    titleColor: Color = Ncolors.current.textPrimary,
    showChevron: Boolean = false,
    columnStyle: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Image(
            painter = rememberVectorPainter(ImageVector.vectorResource(icon)),
            contentDescription = title,
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(Ncolors.current.textPrimary)
        )
        when {
            columnStyle == false -> {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 14.sp,
                        fontFamily = Comfortaa,
                        fontWeight = FontWeight.Medium,
                        color = titleColor
                    )
                    if (subtitle != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            subtitle,
                            fontSize = 11.sp,
                            fontFamily = Comfortaa,
                            color = Ncolors.current.textSecondary
                        )
                    }
                }

                when {
                    trailing != null -> trailing()
                    showChevron -> Text("›", fontSize = 20.sp, color = Ncolors.current.textSecondary)
                }
            }
            columnStyle == true -> {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 14.sp,
                        fontFamily = Comfortaa,
                        fontWeight = FontWeight.Medium,
                        color = titleColor
                    )
                    when {
                        trailing != null -> trailing()
                        showChevron -> Text("›", fontSize = 20.sp, color = Ncolors.current.textSecondary)
                    }
                }
            }
        }
    }
}

//switch

@Composable
fun SettingsSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalAppColors.current
    Switch(
        checked         = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor   = Color.White,
            checkedTrackColor   = colors.accent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = colors.accentDark.copy(alpha = 0.55f, red = 0.3f, green = 0.3f, blue = 0.3f),
            uncheckedBorderColor = Color.Transparent,
            checkedBorderColor   = Color.Transparent,
        )
    )
}

@Composable
fun ColorSlotPicker(label: String, options: List<Color>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
        Text(label, fontSize = 11.sp, color = Color(0xFF888888), modifier = Modifier.padding(start = 20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp, start = 20.dp)) {
            options.forEachIndexed { i, c ->
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(c)
                        .border(if (i == selected) 2.dp else 0.dp, Color.White, CircleShape)
                        .clickable { onSelect(i) }
                )
            }
        }
    }
}