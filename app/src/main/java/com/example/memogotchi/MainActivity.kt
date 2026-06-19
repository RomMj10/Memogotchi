package com.example.memogotchi

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memogotchi.ui.page.ActivityTreeScreen
import com.example.memogotchi.ui.page.AppSettings
import com.example.memogotchi.ui.page.BatteryState
import com.example.memogotchi.ui.page.DayData
import com.example.memogotchi.ui.page.DiaryEntry
import com.example.memogotchi.ui.page.PetScreen
import com.example.memogotchi.ui.page.PomodoroStore
import com.example.memogotchi.ui.theme.MemogotchiTheme
import com.example.memogotchi.ui.page.ScreenTimeScreen
import com.example.memogotchi.ui.page.TasksScreen
import com.example.memogotchi.ui.page.SettingsScreen
import com.example.memogotchi.ui.page.TaskStore
import com.example.memogotchi.ui.page.WellnessScreen
import com.example.memogotchi.ui.page.WellnessStore
import com.example.memogotchi.ui.page.createNotificationChannel
import com.example.memogotchi.ui.page.getBatteryLevel
import com.example.memogotchi.ui.page.hasUsageStatsPermission
import com.example.memogotchi.ui.page.loadWeekData
import com.example.memogotchi.ui.page.maybeSendHealthAlert
import com.example.memogotchi.ui.page.MemoStore
import com.example.memogotchi.ui.page.NameInputScreen
import com.example.memogotchi.ui.page.PermissionScreen
import com.example.memogotchi.ui.page.petStateFromScreenTime
import com.example.memogotchi.ui.theme.GildaDisplay
import com.example.memogotchi.ui.page.TimerMode
import com.example.memogotchi.ui.page.createGoalNotificationChannels
import com.google.ai.client.generativeai.GenerativeModel
import java.util.Calendar
import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @RequiresApi(Build.VERSION_CODES.Q)
    @androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AppSettings.init(this)
        createNotificationChannel(this)
        createGoalNotificationChannels(this)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val baseDensity = LocalDensity.current
            val scaledDensity = remember(AppSettings.textSize, baseDensity) {
                Density(
                    density = baseDensity.density,
                    fontScale = baseDensity.fontScale * AppSettings.textSize.scale
                )
            }
            MemogotchiTheme {
                CompositionLocalProvider(LocalDensity provides scaledDensity) {
                    MainShell(windowSizeClass)
                }
            }
        }
    }
}

private val BgColor       = Color(0xFF16171C)
private val SurfaceColor  = Color(0xFF1F2125)
private val AccentGreen   = Color(0xFF77C59D)
private val TextSecondary = Color(0xFF888888)

enum class NavTab(val label: String, val iconRes: Int) {
    PET("Pet",           R.drawable.ic_nav_pet),
    WELLNESS("Wellness", R.drawable.ic_nav_wellness),
    SCREEN_TIME("Screen Time", R.drawable.ic_nav_screentime),
    TASKS("Tasks",       R.drawable.ic_nav_tasks),
}

@SuppressLint("RememberReturnType")
@androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MainShell(windowSizeClass: WindowSizeClass) {
    val context = LocalContext.current
    val batteryLevel = remember { getBatteryLevel(context) }
    var showSettings by remember { mutableStateOf(false) }
    var showActivityTree by remember { mutableStateOf(false) }

    var currentTab by remember { mutableStateOf(NavTab.PET) }
    var weekData by remember { mutableStateOf<List<DayData>>(emptyList()) }
    var isLoadingWeekData by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(context.hasUsageStatsPermission()) }
    var petName by remember { mutableStateOf(MemoStore.loadName(context)) }
    val startupComplete = !petName.isNullOrBlank() && hasPermission
    val virtualPoints = 0
    val xpEarned = 0


    // ── Wellness state hoisted here so it survives tab switches ──────────
    val wellnessStates = remember {
        val savedSliders = WellnessStore.loadSliders(context)
        mutableStateListOf(
            BatteryState(
                "emotional",
                "Emotional",
                Color(0xFF5C2A3A),
                Color(0xFFD4537E),
                value = savedSliders[0]
            ),
            BatteryState(
                "social",
                "Social",
                Color(0xFF2E2A5C),
                Color(0xFF7C6FE0),
                value = savedSliders[1]
            ),
            BatteryState(
                "physical",
                "Physical",
                Color(0xFF0D3D2E),
                Color(0xFF1D9E75),
                value = savedSliders[2]
            ),
            BatteryState(
                "motivation",
                "Motivation",
                Color(0xFF4A2E05),
                Color(0xFFD4920A),
                value = savedSliders[3]
            ),
        )
    }

    val wellnessSliders = remember {
        mutableStateListOf<Float>().also { list ->
            list.addAll(WellnessStore.loadSliders(context))
        }
    }

    val diaryEntries = remember {
        mutableStateListOf<DiaryEntry>().also { list ->
            list.addAll(WellnessStore.loadDiaryEntries(context))
        }
    }

    LaunchedEffect(wellnessStates.map { it.value }) {
        WellnessStore.saveSliders(context, wellnessStates.map { it.value ?: 50f })
    }

    LaunchedEffect(wellnessSliders.toList()) {
        WellnessStore.saveSliders(context, wellnessSliders.toList())
    }

    LaunchedEffect(diaryEntries.toList()) {
        WellnessStore.saveDiaryEntries(context, diaryEntries.toList())
    }

    // ── Pet timer state hoisted here so it survives tab switches ─────────
    var elapsedSeconds by remember { mutableLongStateOf(PomodoroStore.loadElapsedSeconds(context)) }
    var timerRunning by remember { mutableStateOf(PomodoroStore.isRunning(context)) }
    var timerMode by remember { mutableStateOf(PomodoroStore.loadMode(context)) }

    LaunchedEffect(timerRunning) {
        while (timerRunning) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    val hourNow = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val today = weekData.lastOrNull()
    val petState = remember(today) { petStateFromScreenTime(today?.totalMs ?: 0L, hourNow) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            while (true) {
                if (weekData.isEmpty()) isLoadingWeekData = true
                isLoadingWeekData = true
                weekData = loadWeekData(context)
                isLoadingWeekData = false
                weekData.lastOrNull()?.let { day ->
                    maybeSendHealthAlert(context, day.totalMs, AppSettings.dailyLimitMinutes)
                }
                delay(60_000L)
            }
        }
    }

    if (!startupComplete) {
        Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
            if (petName.isNullOrBlank()) {
                NameInputScreen(onSubmit = { Name ->
                    MemoStore.saveName(context, Name)
                    petName = Name
                })
            } else {
                PermissionScreen(
                    onGrant = {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    },
                    onRefresh = { hasPermission = context.hasUsageStatsPermission() }
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(BgColor)) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = 72.dp)
            ) {
                if (showSettings) {
                    SettingsScreen()
                } else if (showActivityTree) {
                    ActivityTreeScreen(
                        completedTasks = remember { TaskStore.getCompletedHistory(context) },
                        onBack = { showActivityTree = false }
                    )
                } else {
                    when (currentTab) {
                        NavTab.PET -> PetScreen(
                            today = weekData.lastOrNull(),
                            petState = petState,
                            xpEarned = xpEarned,
                            batteryLevel = batteryLevel,
                            elapsedSeconds = elapsedSeconds,
                            timerRunning = timerRunning,
                            timerMode = timerMode,
                            petName = petName ?: "",
                            onTimerToggle = {
                                if (timerRunning) {
                                    PomodoroStore.pause(context, elapsedSeconds)
                                    timerRunning = false
                                } else {
                                    PomodoroStore.start(context, elapsedSeconds)
                                    timerRunning = true
                                }
                            },
                            onModeChange = { newMode ->
                                timerMode = newMode
                                PomodoroStore.setMode(context, newMode)
                                elapsedSeconds = 0L
                                timerRunning = false
                            },
                            onReset = {
                                PomodoroStore.reset(context)
                                elapsedSeconds = 0L
                                timerRunning = false
                            },
                            previewTasks = remember(weekData) {
                                val dateKey = java.text.SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    java.util.Locale.getDefault()
                                ).format(java.util.Date())
                                TaskStore.loadTasksForDate(context, dateKey) ?: emptyList()
                            },
                            onOpenTasks = { currentTab = NavTab.TASKS },
                            onOpenScreenTime = { currentTab = NavTab.SCREEN_TIME },
                            onOpenWellness = { currentTab = NavTab.WELLNESS },
                            onOpenActivityTree = { showActivityTree = true }

                        )

                        NavTab.WELLNESS -> WellnessScreen(
                            states = wellnessStates,
                            sliderValues = wellnessSliders,
                            diaryEntries = diaryEntries
                        )

                        NavTab.SCREEN_TIME -> ScreenTimeScreen(
                            windowSizeClass = windowSizeClass,
                            weekData = weekData,
                            isLoading = isLoadingWeekData,
                            hasPermission = hasPermission,
                            onGrantPermission = {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            },
                            onRefreshPermission = {
                                hasPermission = context.hasUsageStatsPermission()
                            }
                        )

                        NavTab.TASKS -> TasksScreen(
                            today = weekData.lastOrNull(),
                            weekData = weekData
                        )
                    }
                }
            }

            // Gear icon on Pet tab
            if (currentTab == NavTab.PET && !showSettings && !showActivityTree) {
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                        .offset(x = (-12).dp, y = 32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Settings, contentDescription = "Settings",
                        tint = TextSecondary, modifier = Modifier
                            .size(22.dp)

                    )
                }
            }

            // Back from settings
            if (showSettings) {
                TextButton(
                    onClick = { showSettings = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 12.dp, y = 32.dp)
                        .padding(top = 12.dp, start = 4.dp)
                ) {
                    Text("< Back", color = AccentGreen, fontFamily = GildaDisplay, fontSize = 16.sp)
                }
            }
            // Bottom nav
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
                        val isSelected = tab == currentTab && !showSettings
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showSettings = false
                                    showActivityTree = false
                                    currentTab = tab
                                }
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
                                fontFamily = GildaDisplay,
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
}

@Preview
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun PreviewPage(modifier: Modifier = Modifier) {
    PetScreen()
}