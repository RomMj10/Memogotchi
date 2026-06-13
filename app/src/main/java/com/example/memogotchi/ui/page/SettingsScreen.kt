package com.example.memogotchi.ui.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.R

//Palette
private val BgColor       = Color(0xFF16171C)
private val SurfaceColor  = Color(0xFF1F2125)
private val DividerColor  = Color(0xFF2C2E34)
private val AccentGreen   = Color(0xFF77C59D)
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF888888)


@Composable
fun SettingsScreen() {
    var strictMode by remember { mutableStateOf(false) }

    var monochromeUI by remember { mutableStateOf(false) }
    var healthAlerts by remember { mutableStateOf(true) }

    var largeText by remember { mutableStateOf(false) }
    var reduceMotion by remember { mutableStateOf(false) }
    var screenReaderHints by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgColor).padding(top = 48.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        item {
            Text(
                text       = "SETTINGS",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                letterSpacing = 3.sp,
                modifier   = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        //Focus Controls
        item { SectionLabel("FOCUS CONTROLS") }
        item {
            SettingsGroup {
                SettingsRow(
                    icon        = R.drawable.outline_av_timer_icon,
                    title       = "Daily Limit",
                    subtitle    = "2h 30m remaining",
                    showChevron = true,
                    onClick     = { /* TODO: navigate to daily limit picker */ }
                )
                RowDivider()
                SettingsRow(
                    icon     = R.drawable.outline_timer_off_icon,
                    title    = "Strict Mode",
                    subtitle = "Disable app opening",
                    trailing = {
                        SettingsSwitch(checked = strictMode, onCheckedChange = { strictMode = it })
                    }
                )
                RowDivider()
                SettingsRow(
                    icon        = R.drawable.outline_monitor_heart_icon,
                    title       = "Monitored Apps",
                    subtitle    = "12 apps selected",
                    showChevron = true,
                    onClick     = { /* TODO: navigate to app picker */ }
                )
            }
        }

        // ── Pet & Theme ──────────────────────────────────────────────────
        item { SectionLabel("PET & THEME") }
        item {
            SettingsGroup {
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
                    subtitle = "When pet is hungry",
                    trailing = {
                        SettingsSwitch(checked = healthAlerts, onCheckedChange = { healthAlerts = it })
                    }
                )
            }
        }

        // ── Accessibility ────────────────────────────────────────────────
        item { SectionLabel("ACCESSIBILITY") }
        item {
            SettingsGroup {
                SettingsRow(
                    icon     = R.drawable.outline_format_size_icon,
                    title    = "Large Text",
                    subtitle = "Increase font size app-wide",
                    trailing = {
                        SettingsSwitch(checked = largeText, onCheckedChange = { largeText = it })
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
                RowDivider()
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
}

//label sections

@Composable
fun SectionLabel(text: String) {
    Text(
        text       = text,
        fontSize   = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color      = TextSecondary,
        letterSpacing = 1.5.sp,
        modifier   = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
    )
}

//group

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = SurfaceColor,
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
    titleColor: Color = TextPrimary,
    showChevron: Boolean = false,
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
            modifier = Modifier.size(20.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = titleColor)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }
        }

        when {
            trailing != null -> trailing()
            showChevron       -> Text("›", fontSize = 20.sp, color = TextSecondary)
        }
    }
}

//switch

@Composable
fun SettingsSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked         = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor   = Color.White,
            checkedTrackColor   = AccentGreen,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFF44464C),
            uncheckedBorderColor = Color.Transparent,
            checkedBorderColor   = Color.Transparent,
        )
    )
}