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
import androidx.compose.foundation.shape.CircleShape
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
import com.example.memogotchi.ui.data.DialogueCategory
import com.example.memogotchi.ui.data.DialoguePool
import com.example.memogotchi.ui.data.fillTemplate
import com.example.memogotchi.ui.page.ActivityTreeScreen
import com.example.memogotchi.ui.page.AnalogTask
import com.example.memogotchi.ui.page.AppSettings
import com.example.memogotchi.ui.page.BatteryState
import com.example.memogotchi.ui.page.DayData
import com.example.memogotchi.ui.page.AppUsageInfo
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
import com.example.memogotchi.ui.page.PersonalityScreen
import com.example.memogotchi.ui.page.PersonalityStore
import com.example.memogotchi.ui.page.TaskTimerStore
import com.example.memogotchi.ui.page.TimerMode
import com.example.memogotchi.ui.page.petStateFromScreenTime
import com.example.memogotchi.ui.theme.GildaDisplay
import com.example.memogotchi.ui.page.createGoalNotificationChannels
import java.util.Calendar
import androidx.compose.ui.draw.blur
import com.example.memogotchi.ui.page.FocusGuardFabMenu
import com.example.memogotchi.ui.page.FocusGuardAction
import com.example.memogotchi.focusguard.AppBlockerScreen
import com.example.memogotchi.focusguard.AppTimerScreen
import com.example.memogotchi.focusguard.ScheduleScreen
import com.example.memogotchi.focusguard.FocusGuardStore
import com.example.memogotchi.ui.page.XpStore
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.Canvas
import com.example.memogotchi.ui.page.ShopScreen
import com.example.memogotchi.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
//signin
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.SetOptions
import com.example.memogotchi.ui.page.SignInScreen
import com.example.memogotchi.ui.page.NearbyOptInScreen
import com.example.memogotchi.fcm.NearbyDeepLinkState
import com.example.memogotchi.ui.page.StandbyScreen
import com.example.memogotchi.ui.page.CircleMapScreen
import com.example.memogotchi.ui.page.IncomingBuddyRequestOverlay
//buddy
import com.example.memogotchi.ui.page.BuddyListScreen
import com.example.memogotchi.ui.page.BuddyDetailScreen
import com.example.memogotchi.ui.page.CreateEventScreen
import com.example.memogotchi.ui.page.IncomingEventInviteOverlay
import com.example.memogotchi.ui.page.SharedGoalScreen
import com.example.memogotchi.usage.UsageReportScheduler
import com.example.memogotchi.ui.page.ShowQrCodeScreen
import com.example.memogotchi.ui.page.ScanQrCodeScreen

val AppTheme = LocalAppColors

class MainActivity : ComponentActivity() {

    private fun handleDeepLinkIntent(intent: Intent?) {
        if (intent?.getStringExtra("deepLinkTarget") == "standby") {
            val matchId = intent.getStringExtra("matchId")
            NearbyDeepLinkState.set(matchId)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @RequiresApi(Build.VERSION_CODES.Q)
    @androidx.annotation.RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLinkIntent(intent)

        AppSettings.init(this)
        createNotificationChannel(this)
        createGoalNotificationChannels(this)

        setContent {
            val appColors = remember(AppSettings.bgIndex, AppSettings.surfaceIndex, AppSettings.accentIndex,
                AppSettings.textPrimaryIndex, AppSettings.textSecondaryIndex) {
                AppSettings.currentAppColors()
            }
            val windowSizeClass = calculateWindowSizeClass(this)
            val baseDensity = LocalDensity.current
            val scaledDensity = remember(AppSettings.textSize, baseDensity) {
                Density(
                    density = baseDensity.density,
                    fontScale = baseDensity.fontScale * AppSettings.textSize.scale
                )
            }

            MemogotchiTheme {
                CompositionLocalProvider(LocalDensity provides scaledDensity, LocalAppColors provides appColors) {
                    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
                    var activeOtherUserId by remember { mutableStateOf<String?>(null) }
                    val pendingMatchId = NearbyDeepLinkState.pendingMatchId.value

                    LaunchedEffect(currentUser) {
                        if (currentUser != null) UsageReportScheduler.schedule(applicationContext)
                    }

                    when {
                        currentUser == null -> {
                            SignInScreen(onSignedIn = { user ->
                                currentUser = user
                                Firebase.firestore.collection("users").document(user.uid)
                                    .set(mapOf("displayName" to (user.displayName ?: "A memo")), SetOptions.merge())
                                FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                                    Firebase.firestore.collection("users").document(user.uid)
                                        .set(mapOf("fcmToken" to fcmToken), SetOptions.merge())
                                }
                                UsageReportScheduler.schedule(applicationContext)
                            })
                        }
                        pendingMatchId != null -> {
                            StandbyScreen(
                                matchId = pendingMatchId,
                                onBothReady = { otherUserId ->
                                    activeOtherUserId = otherUserId
                                    NearbyDeepLinkState.consume()
                                },
                                onCancel = { NearbyDeepLinkState.consume() }
                            )
                            IncomingBuddyRequestOverlay()
                            IncomingEventInviteOverlay()
                        }
                        activeOtherUserId != null -> {
                            CircleMapScreen(
                                otherUserId = activeOtherUserId!!,
                                onConnected = { activeOtherUserId = null },
                                onBack = { activeOtherUserId = null }
                            )
                            IncomingBuddyRequestOverlay()
                            IncomingEventInviteOverlay()
                        }
                        else -> {
                            MainShell(windowSizeClass)
                            IncomingBuddyRequestOverlay()
                            IncomingEventInviteOverlay()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavTabItem(
    tab: NavTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Image(
            painter = rememberVectorPainter(ImageVector.vectorResource(tab.iconRes)),
            contentDescription = tab.label,
            modifier = Modifier.size(22.dp),
            colorFilter = if (isSelected) ColorFilter.tint(AppTheme.current.accent) else ColorFilter.tint(AppTheme.current.textSecondary)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            fontFamily = GildaDisplay,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) AppTheme.current.accentLight else AppTheme.current.textSecondary
        )
    }
}


enum class NavTab(val label: String, val iconRes: Int) {
    PET("Pet", R.drawable.memogotchi_vector),
    WELLNESS("Wellness", R.drawable.ic_nav_wellness),
    SCREEN_TIME("Screen Time", R.drawable.ic_nav_screentime),
    TASKS("Tasks", R.drawable.ic_nav_tasks),
    SHOP("Shop", R.drawable.ic_nav_shopping)
}

@SuppressLint("RememberReturnType")
@androidx.annotation.RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MainShell(windowSizeClass: WindowSizeClass) {
    val context = LocalContext.current
    val batteryLevel = remember { getBatteryLevel(context) }
    var showSettings by remember { mutableStateOf(false) }
    var showActivityTree by remember { mutableStateOf(false) }
    var showFocusGuardMenu by remember { mutableStateOf(false) }
    var showAppBlocker by remember { mutableStateOf(false) }
    var showAppTimer by remember { mutableStateOf(false) }
    var showSchedule by remember { mutableStateOf(false) }

    var currentTab by remember { mutableStateOf(NavTab.PET) }
    var weekData by remember { mutableStateOf<List<DayData>>(emptyList()) }
    var isLoadingWeekData by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(context.hasUsageStatsPermission()) }
    var petName by remember { mutableStateOf(MemoStore.loadName(context)) }
    val startupComplete = !petName.isNullOrBlank() && hasPermission
    var totalXp by remember { mutableStateOf(XpStore.loadXp(context)) }
    var showNearbyOptIn by remember { mutableStateOf(false) }
    var showBuddyList by remember { mutableStateOf(false) }
    var selectedBuddy by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showCreateEvent by remember { mutableStateOf(false) }
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    var showShowQrCode by remember { mutableStateOf(false) }
    var showScanQrCode by remember { mutableStateOf(false) }


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

    val dateKeyToday = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
    var activeTaskTimer by remember { mutableStateOf(TaskTimerStore.load(context)) }
    var scheduledBlockedApps by remember { mutableStateOf<List<AppUsageInfo>>(emptyList()) }

    var isFirstDialogue by remember { mutableStateOf(true) }
    val hourNow = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val today = weekData.lastOrNull()
    val yesterdayTotalMs = remember(weekData) { weekData.getOrNull(weekData.size - 2)?.totalMs }
    LaunchedEffect(today) {
        PersonalityStore.rollupScreenCategoryTallyIfNeeded(
            context,
            today
        )
    }
    val petState = remember(today) {
        petStateFromScreenTime(
            today?.totalMs ?: 0L,
            hourNow,
            isFirstDialogue,
            petName ?: ""
        )
    }
    LaunchedEffect(petState) {
        isFirstDialogue = false
    }
    var taskAnnouncement by remember { mutableStateOf<String?>(null) }
    var showPersonality by remember {
        mutableStateOf(false)
    }


    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(timerRunning) {
        while (timerRunning) {
            activeTaskTimer?.let { active ->
                if (elapsedSeconds >= active.targetSeconds) {
                    TaskTimerStore.completeActiveTaskAndClear(context)
                    totalXp = XpStore.loadXp(context)
                    taskAnnouncement = DialoguePool.randomLine(DialogueCategory.TASK_DONE)
                        ?.fillTemplate("task" to active.taskTitle, "name" to (petName ?: ""))
                    activeTaskTimer = null
                    PomodoroStore.reset(context)
                    elapsedSeconds = 0L
                    timerRunning = false
                }
            }
            if (!timerRunning) break
            delay(1000L)
            elapsedSeconds++
            activeTaskTimer?.let { TaskTimerStore.tick(context, elapsedSeconds) }
        }
    }

    LaunchedEffect(showSchedule) {
        if (!showSchedule) {
            scheduledBlockedApps = FocusGuardStore.getScheduledBlockedApps(context)
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

    val onStartTaskTimer: (AnalogTask) -> Unit = { task ->
        if (activeTaskTimer == null) {
            TaskTimerStore.start(context, task.id, task.title, task.durationMinutes, dateKeyToday)
            activeTaskTimer = TaskTimerStore.load(context)
            timerMode = TimerMode.STOPWATCH
            PomodoroStore.setMode(context, TimerMode.STOPWATCH)
            PomodoroStore.start(context, 0L)
            elapsedSeconds = 0L
            timerRunning = true
            currentTab = NavTab.PET
        }
    }
    val onCancelTaskTimer: (AnalogTask) -> Unit = {
        TaskTimerStore.clear(context)
        activeTaskTimer = null
        PomodoroStore.reset(context)
        elapsedSeconds = 0L
        timerRunning = false
    }


    if (!startupComplete) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.current.bg)
                .then(if (showFocusGuardMenu) Modifier.blur(18.dp) else Modifier)
        ) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.current.bg)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = 72.dp)
            ) {
                if (showSettings) {
                    SettingsScreen(
                        currentPetName = petName ?: "",
                        onPetRenamed = { newName -> petName = newName },
                        onOpenGoalBuddy = {
                            showNearbyOptIn = true
                            showSettings = false
                                          },
                        onOpenBuddyList = {
                            showBuddyList = true
                            showSettings = false
                        }
                    )
                } else if (showActivityTree) {
                    ActivityTreeScreen(
                        completedTasks = remember { TaskStore.getCompletedHistory(context) },
                        onBack = { showActivityTree = false }
                    )
                } else if (showPersonality) {
                    PersonalityScreen(
                        onBack = {
                            showPersonality = false
                        }
                    )
                } else if (showAppBlocker) {
                    AppBlockerScreen(
                        onBack = { showAppBlocker = false }
                    )
                } else if (showAppTimer) {
                    AppTimerScreen(
                        onBack = { showAppTimer = false }
                    )
                } else if (showSchedule) {
                    ScheduleScreen(
                        onBack = { showSchedule = false }
                    )
                }  else if (showNearbyOptIn) {
                    NearbyOptInScreen(
                        onBack = {
                            showNearbyOptIn = false
                            showSettings = true
                                 },
                        onShowQrCode = {
                            showNearbyOptIn = false
                            showShowQrCode = true
                        },
                        onScanQrCode = {
                            showNearbyOptIn = false
                            showScanQrCode = true
                        }
                    )
                } else if (showShowQrCode) {
                    ShowQrCodeScreen(
                        onBack = {
                            showShowQrCode = false
                            showNearbyOptIn = true
                        }
                    )
                } else if (showScanQrCode) {
                    ScanQrCodeScreen(
                        onBack = {
                            showScanQrCode = false
                            showNearbyOptIn = true
                        },
                        onMatched = { matchId ->
                            showScanQrCode = false
                            NearbyDeepLinkState.set(matchId)
                        }
                    )
                } else if (showCreateEvent && selectedBuddy != null) {
                    CreateEventScreen(
                        buddyConnectionId = selectedBuddy!!.first,
                        onCreated = { showCreateEvent = false },
                        onBack = { showCreateEvent = false }
                    )
                } else if (selectedEventId != null) {
                    SharedGoalScreen(
                        eventId = selectedEventId!!,
                        onBack = { selectedEventId = null }
                    )
                } else if (selectedBuddy != null) {
                    BuddyDetailScreen(
                        buddyId = selectedBuddy!!.first,
                        otherUserId = selectedBuddy!!.second,
                        onCreateEvent = { showCreateEvent = true },
                        onOpenEvent = { eventId -> selectedEventId = eventId },
                        onConnectionEnded = { selectedBuddy = null },
                        onBack = { selectedBuddy = null }
                    )
                } else if (showBuddyList) {
            BuddyListScreen(
                onOpenBuddy = { buddyId, otherUserId -> selectedBuddy = buddyId to otherUserId },
                onBack = {
                    showBuddyList = false
                    showSettings = true
                }
            )
        } else {
                    when (currentTab) {
                        NavTab.PET -> PetScreen(
                            today = weekData.lastOrNull(),
                            petState = petState,
                            xpEarned = totalXp,
                            batteryLevel = batteryLevel,
                            elapsedSeconds = elapsedSeconds,
                            timerRunning = timerRunning,
                            timerMode = timerMode,
                            activeTaskTitle = activeTaskTimer?.taskTitle,
                            activeTaskTargetSeconds = activeTaskTimer?.targetSeconds,
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
                            onOpenActivityTree = { showActivityTree = true },
                            onOpenPersonality = {
                                showPersonality = true
                            },
                            taskAnnouncement = taskAnnouncement,
                            onTaskAnnouncementConsumed = { taskAnnouncement = null },
                            yesterdayTotalMs = yesterdayTotalMs,
                            scheduledBlockedApps = scheduledBlockedApps,
                            onScheduledBlockClick = {
                                showSchedule = true
                            }

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
                            weekData = weekData,
                            onTasksGenerated = { newTasks ->
                                newTasks.firstOrNull()?.let { task ->
                                    taskAnnouncement =
                                        DialoguePool.randomLine(DialogueCategory.NEW_TASK)
                                            ?.fillTemplate(
                                                "task" to task.title,
                                                "name" to (petName ?: "")
                                            )
                                }
                            },
                            activeTaskTimer = activeTaskTimer,
                            activeElapsedSeconds = elapsedSeconds,
                            onStartTaskTimer = onStartTaskTimer,
                            onCancelTaskTimer = onCancelTaskTimer,
                            onTaskCompleted = { tasks ->
                                taskAnnouncement =
                                    DialoguePool.randomLine(DialogueCategory.TASK_DONE)
                                        ?.fillTemplate(
                                            "name" to (petName ?: ""),
                                            "task" to tasks.title,
                                        )
                            }
                        )

                        NavTab.SHOP -> ShopScreen(
                            onBack = { currentTab = NavTab.PET }
                        )
                    }
                }
            }

            // Gear icon on Pet tab
            if (currentTab == NavTab.PET && !showSettings && !showActivityTree && !showAppBlocker && !showAppTimer && !showSchedule && !showPersonality) {
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                        .offset(x = (-12).dp, y = 32.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = AppTheme.current.surface),
                    shape = RoundedCornerShape(24.dp)

                ) {
                    Icon(
                        Icons.Outlined.Settings, contentDescription = "Settings",
                        tint = AppTheme.current.textSecondary, modifier = Modifier
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
                    Text(
                        "< Back",
                        color = AppTheme.current.accent,
                        fontFamily = GildaDisplay,
                        fontSize = 16.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                // 1. Custom curved background drawn on canvas
                val barColor = AppTheme.current.surface
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val curveWidth = 250f   // how wide the arch is
                    val curveDepth = 44f    // how deep the arch cuts upward
                    val centerX = width / 2f

                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(centerX - curveWidth / 2f, 0f)
                        cubicTo(
                            centerX - curveWidth / 4f, 0f,
                            centerX - curveWidth / 4f, -curveDepth,
                            centerX, -curveDepth
                        )
                        cubicTo(
                            centerX + curveWidth / 4f, -curveDepth,
                            centerX + curveWidth / 4f, 0f,
                            centerX + curveWidth / 2f, 0f
                        )
                        lineTo(width, 0f)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(path, color = barColor)
                }

                // 2. Nav items row (transparent background, sits on top of canvas)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavTabItem(
                        tab = NavTab.PET,
                        isSelected = currentTab == NavTab.PET && !showSettings,
                        onClick = {
                            showSettings = false
                            showActivityTree = false
                            showPersonality = false
                            showNearbyOptIn = false
                            showBuddyList = false
                            selectedBuddy = null
                            selectedEventId = null
                            showShowQrCode = false
                            showScanQrCode = false
                            currentTab = NavTab.PET
                        }
                    )
                    NavTabItem(
                        tab = NavTab.WELLNESS,
                        isSelected = currentTab == NavTab.WELLNESS && !showSettings,
                        onClick = {
                            showSettings = false
                            showActivityTree = false
                            showPersonality = false
                            showNearbyOptIn = false
                            showBuddyList = false
                            selectedBuddy = null
                            selectedEventId = null
                            showShowQrCode = false
                            showScanQrCode = false
                            currentTab = NavTab.WELLNESS
                        }
                    )
                    Spacer(modifier = Modifier.weight(1f))
//                    NavTabItem(
//                        tab = NavTab.SCREEN_TIME,
//                        isSelected = currentTab == NavTab.SCREEN_TIME && !showSettings,
//                        onClick = {
//                            showSettings = false
//                            showActivityTree = false
//                            currentTab = NavTab.SCREEN_TIME
//                        }
//                    )
                    NavTabItem(
                        tab = NavTab.SHOP,
                        isSelected = currentTab == NavTab.SHOP && !showSettings,
                        onClick = {
                            showSettings = false
                            showActivityTree = false
                            showPersonality = false
                            showNearbyOptIn = false
                            showBuddyList = false
                            selectedBuddy = null
                            selectedEventId = null
                            showShowQrCode = false
                            showScanQrCode = false
                            currentTab = NavTab.SHOP
                        }
                    )
                    NavTabItem(
                        tab = NavTab.TASKS,
                        isSelected = currentTab == NavTab.TASKS && !showSettings,
                        onClick = {
                            showSettings = false
                            showPersonality = false
                            showActivityTree = false
                            showNearbyOptIn = false
                            showBuddyList = false
                            selectedBuddy = null
                            selectedEventId = null
                            showShowQrCode = false
                            showScanQrCode = false
                            currentTab = NavTab.TASKS
                        }
                    )

                }

                // 3. Floating FocusGuard button (sits in the arch)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .offset(y = (-14).dp)
                        .background(color = AppTheme.current.accent, shape = CircleShape)
                        .clip(CircleShape)
                        .clickable { showFocusGuardMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberVectorPainter(
                            ImageVector.vectorResource(R.drawable.ic_nav_focusguard)
                        ),
                        contentDescription = "Focus Guard",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
            } // end bottom nav Box

            // ── FocusGuard overlay menu (inside root Box so it layers on top) ─
            FocusGuardFabMenu(
                visible = showFocusGuardMenu,
                onDismiss = { showFocusGuardMenu = false },
                onActionSelected = { action ->
                    when (action) {
                        FocusGuardAction.BLOCK_APP -> showAppBlocker = true
                        FocusGuardAction.SET_TIME_LIMIT -> showAppTimer = true
                        FocusGuardAction.CREATE_SCHEDULE -> showSchedule = true
                    }
                    showFocusGuardMenu = false
                }
            )

        }
    }
}

@Preview
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun PreviewPage(modifier: Modifier = Modifier) {
    PetScreen()
}