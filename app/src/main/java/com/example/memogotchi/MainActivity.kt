package com.example.memogotchi

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.ui.page.DayData
import com.example.memogotchi.ui.page.PetScreen
import com.example.memogotchi.ui.theme.MemogotchiTheme
import com.example.memogotchi.ui.page.ScreenTimeScreen
import com.example.memogotchi.ui.page.TasksScreen
import com.example.memogotchi.ui.page.SettingsScreen
import com.example.memogotchi.ui.page.getBatteryLevel
import com.example.memogotchi.ui.page.hasUsageStatsPermission
import com.example.memogotchi.ui.page.loadWeekData
import com.example.memogotchi.ui.page.petStateFromScreenTime
import java.util.Calendar


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
           val windowSizeClass = calculateWindowSizeClass(this)
            MemogotchiTheme {
                MainShell(windowSizeClass)
            }
        }
    }
}

private val BgColor      = Color(0xFF16171C)
private val SurfaceColor = Color(0xFF1F2125)
private val AccentGreen  = Color(0xFF77C59D)
private val TextSecondary = Color(0xFF888888)

enum class NavTab(val label: String, val iconRes: Int) {
    PET("Pet", R.drawable.ic_nav_pet),
    SCREEN_TIME("Screen Time", R.drawable.ic_nav_screentime),
    TASKS("Tasks", R.drawable.ic_nav_tasks),
    SETTINGS("Settings", R.drawable.ic_nav_settings)
}

@SuppressLint("RememberReturnType")
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MainShell(windowSizeClass: WindowSizeClass) {
    val context = LocalContext.current
    val batteryLevel = remember { getBatteryLevel(context) }
    var currentTab by remember { mutableStateOf(NavTab.PET) }
    var weekData by remember { mutableStateOf<List<DayData>>(emptyList()) }
    var hasPermission by remember {mutableStateOf(context.hasUsageStatsPermission()) }
    val virtualPoints  = 0
    val xpEarned = 0

    val hourNow = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY)}
    val today = weekData.lastOrNull()
    val petState = remember(today) { petStateFromScreenTime(today?.totalMs ?: 0L, hourNow) }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            weekData = loadWeekData(context)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        // Page content — leaves room for nav bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp)
        ) {

            when (currentTab) {
                NavTab.PET         -> PetScreen(
                    today = weekData.lastOrNull(),
                    petState = petState,
                    xpEarned = xpEarned,
                    batteryLevel = batteryLevel
                )
                NavTab.SCREEN_TIME -> ScreenTimeScreen(windowSizeClass)
                NavTab.TASKS       -> TasksScreen(today = weekData.lastOrNull())
                NavTab.SETTINGS    -> SettingsScreen()
            }
        }

        // Bottom nav bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(SurfaceColor)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = tab == currentTab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { currentTab = tab }
                            .padding(vertical = 8.dp)
                    ) {
                        Image(
                            painter = rememberVectorPainter(ImageVector.vectorResource(tab.iconRes)),
                            contentDescription = tab.label,
                            modifier = Modifier.size(22.dp),
                            colorFilter = if (isSelected) ColorFilter.tint(AccentGreen) else null
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) AccentGreen else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Preview
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ScreenTimeScreenPage(modifier: Modifier = Modifier) {
    PetScreen()
}

