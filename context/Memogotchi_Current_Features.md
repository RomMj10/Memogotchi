# Memogotchi Current Features & Status Reference

LATEST UPDATE
---

## 1. Feature Status Overview

| Feature | Status | Notes |
|---|---|---|
| **Memogotchi Companion** | Partial | Lottie animations, pet states, speech bubble, and focus timer all working. Wardrobe/customization UI stubbed but no reward economy yet. |
| **Analog Twin** | Partial | Local rule-based task engine and Gemini 1.5 Flash integration both working. Suggestions displayed in Tasks tab with trigger reason pills. |
| **Activity Tree** | Not started | A dedicated screen (`ActivityTreeScreen`) is implemented, but the core visual growth map and XP-based unlockables are not yet functional. Placeholder planned under a future Garden tab. |
| **Personality Profile** | Partial | AI-generated summary now functional via Gemini 2.5 Flash. Frontend display, unlock conditions, and refresh mechanism are implemented. Depends on diary and screen time data accumulation. |
| **Battery System** | Done | Full four-battery logging (Emotional, Social, Physical, Motivation). Vertical sliders, icon states per tier, overall % card with fill animation. State persists across tab switches. |
| **Diary & Time Capsule** | Partial | FAB with 5 options, diary entry screen with optional sliders, category chips, and text entry. Entries display in diary list with **swipe-to-delete functionality**. Photo/audio attachments are now supported in the entry editor and displayed in the card. Conditional locks not yet built. |
| **Activity Verification** | Partial | Task tick animations and achievement milestones working. Photo verification, AI confirmation, and DND/Pomodoro tools not yet implemented. |
| **Rewards Economy** | Not started | XP points tracked in stats bar but no shop or spending system built yet. |
| **Navbar & Shell** | Done | Initial onboarding (pet naming, permissions) and 4-tab nav (Pet, Wellness, Screen Time, Tasks). Settings, Personality, and Activity Tree screens are implemented as full-screen overlays. Status bar padding fixed globally. |
| **Pet Naming / Onboarding** | Done | Initial setup flow with `NameInputScreen` and `PermissionScreen` is implemented. |
| **Dialogue System** | Done | Context-aware dialogue (`DialoguePool`) integrated for task events and initial pet interaction. |
| **Task Timer / Pomodoro Management** | Done | Integrated stopwatch/pomodoro timer (elapsed seconds, running state, modes) persists across tab switches and is tied to active tasks. |
| **Pet Focus Timer** | Done | Stopwatch counts up continuously even when switching tabs. Tap to pause/resume. Persists across all pages via MainShell state hoisting. |
| **Screen Time Dashboard** | Done | 7-day bar chart, per-app breakdown with icons, category fill bars, responsive phone/tablet layouts, and usage permission prompts all working. |
| **Notifications** | Partial | Passive alerts at 80% and 100% of daily screen limit working. Goal notification channels created, but interactive push notifications for battery check-in and goal reminders not yet implemented. |

---

## 2. Current Features in Detail

### a. Frontend — UI & Interactive Components

#### Main Shell & Navigation (`MainActivity.kt`)
- **Initial Onboarding Flow**: Guides new users through pet naming (`NameInputScreen`) and granting usage statistics permission (`PermissionScreen`) before entering the main application.
- **Main Navigation**: Features a 4-tab bottom navigation bar: Pet, Wellness, Screen Time, Tasks.
- **Overlay Screens**: `SettingsScreen`, `PersonalityScreen`, and `ActivityTreeScreen` are displayed as full-screen overlays, accessible from other parts of the app (e.g., from `PetScreen`).
- **State Hoisting**: Wellness battery states, slider values, diary entries, and the pet timer are all managed in `MainShell` to ensure data persistence across tab switches.
- **UI/UX Enhancements**: Status bar padding applied globally. Responsive Compose layout with `WindowSizeClass` for phone and tablet. Dynamic text size scaling via custom `LocalDensity` provider.

#### Pet Screen (`PetScreen.kt`)
- **Lottie Animations**: Renders expressive animations for different pet states (idle, happy, concerned, tired, alarmed) using raw Lottie resource files.
- **Speech Bubble**: Displays customizable, context-aware messages from the pet, dynamically generated based on screen time, time of day, and initial interactions.
- **Integrated Focus Timer**: A stopwatch counting elapsed seconds, with tap-to-pause/resume functionality. This timer (`elapsedSeconds`, `timerRunning`, `timerMode`) is controlled from `MainShell` and persists across navigation. Supports `STOPWATCH` and `POMODORO` modes.
- **Stats Bar**: Displays hardware battery levels, acquired XP points, and total daily screen time.
- **Navigation Callbacks**: Provides callbacks to open various other screens as overlays (`onOpenSettings`, `onOpenPersonality`, `onOpenActivityTree`) or switch tabs directly (`onOpenTasks`, `onOpenScreenTime`, `onOpenWellness`).
- **Task Announcements**: Displays `taskAnnouncement` messages, providing dialogue related to task generation or completion.

#### Wellness Screen (`WellnessScreen.kt`) — NEW / UPDATED
- **Overall Battery Card**: Features a horizontal fill animation where color interpolates from dark red (low) to dark green (high), reflecting the average of the four wellness states. Percentage displayed in a circular indicator.
- **Four Battery States**: Emotional, Social, Physical, Motivation.
  - Icons (`Icons.Outlined.HeartBroken`/`FavoriteBorder`/`Favorite` for emotional, `PersonOff`/`Person`/`People`/`Groups` for social, `Bedtime`/`DirectionsWalk`/`DirectionsRun`/`FitnessCenter` for physical, `SentimentVeryDissatisfied`/`LocalFireDepartment`/`ElectricBolt` for motivation) dynamically change based on tier (0–20% low/broken, 21–40% muted, 41–60% neutral, 61–80% active, 81–100% peak).
  - Vertical sliders expand together upon tapping the '+' button or any state icon, allowing users to log their current levels. Sliders use `rememberUpdatedState` for smooth dragging, with main state icons updating only on submission.
- **Diary Section**:
  - Features a Floating Action Button (FAB) that expands to reveal five options for creating new entries (Today, Yesterday, Other Day, Voice, Photo) with arc animations.
  - FAB rotates to a '✕' icon when expanded; a scrim darkens the background.
  - Date picker (`DatePickerDialog`) for selecting "Other Day" entries.
  - Diary entry screen: Allows users to set optional slider values (snapshot), select multiple category chips, and write text.
  - **Supports attaching photos and audio recordings** directly from the entry editor or via the FAB. Attached media is displayed as thumbnails/players within the entry.
  - Submission is blocked if the text field is empty and no media is attached, showing an inline error banner.
  - Saved entries appear in a list with date labels, selected categories, and displays for attached photos/audio.
  - **Diary entries are swipeable to delete** (swiping an entry reveals a delete icon and removes it upon confirmation). There is no explicit delete button on the individual `DiaryEntryCard`.
- `AttachButton` composable for consistent styling of media attachment buttons.

#### NEW: Personality Screen (`PersonalityScreen.kt`)
- **Display**: Presents a generated personality profile for "Memo".
- **Content**: Includes a concise tagline, three descriptive trait phrases (styled as visual pills), and 2-3 longer paragraphs reflecting observed user behavior.
- **User Feedback**: Displays an "unlock" message if the criteria for profile generation are not yet met. Shows "Memo is thinking..." during the AI generation process.
- **Interactivity**: Features a "refresh" button to request a new profile, accompanied by a "dirty" indicator (small green circle) if the underlying behavioral data has changed since the last profile was generated.

#### Screen Time Dashboard (`ScreenTimeScreen.kt`)
- 7-day interactive bar chart — tap any bar to update the app detail panel.
- Per-app breakdown: launch icons, package labels, duration, category fill bar.
- Dual layout: `PhoneLayout` (nested panels) and `TabletLayout` (side-by-side columns).
- Permission request screen if usage access is not granted.

#### Tasks & Achievements Screen (`TaskScreen.kt`)
- Task cards with category symbols, duration badges, and trigger reason pills.
- Tap-to-toggle tick animation marks tasks as done.
- Achievements milestone tracker with locked/unlocked states.
- Focus dashboard header with hardware battery color thresholds.
- **Integration**: Receives callbacks for task timer management (`onStartTaskTimer`, `onCancelTaskTimer`) and for generating dialogue (`onTasksGenerated`, `onTaskCompleted`).

#### Settings Screen (`SettingsScreen.kt`)
- Now accessed via a gear icon on the Pet tab instead of a dedicated nav slot.
- Screen limit slider (30 min to 8 hours in 15-min increments).
- Toggles: Strict Mode, Monochrome UI, Health Alerts, Reduce Motion.
- Text size selector: Small, Normal, Large — scales entire app instantly.

---

### b. Backend — Business Logic & Engines

#### Screen Time Tracking Engine (`ScreenTimeScreen.kt` / `MainActivity.kt`)
- `UsageStatsManager` queries in background coroutines.
- Reconstructs foreground durations via `ACTIVITY_RESUMED` / `ACTIVITY_PAUSED` event pairs.

#### App Categorization Engine (`AnalogTaskEngine.kt`)
- System package flags (API 26+) for category resolution.
- Keyword-based regex fallback covering Instagram, TikTok, YouTube, Roblox, ChatGPT, Gemini, Slack, and more.

#### Task Recommendation Engine (Local Rule-Based) (`AnalogTaskEngine.kt`)
- Evaluates categorized app metrics against screen-time thresholds, generating context-specific recommendations.
  - Social >= 1.5h → personal letter/notes
  - Social >= 3h → face-to-face meetup
  - Gaming >= 1h → walk/break
  - Gaming >= 2.5h → analog games
  - Entertainment >= 2h → physical reading
  - Browsing >= 1.5h → offline journaling
  - Total >= 2h → posture stretching
  - Total >= 4h → phone-free food prep
  - Battery < 20% → put phone down to charge

#### Gemini AI Task Engine (`GeminiTaskEngine.kt`)
- Integrates Google Generative AI SDK with Gemini 1.5 Flash.
- Structured JSON schema responses parsed into `AnalogTask` entities.
- Auto-fallback to local rule-based engine on API failure or missing credentials.

#### NEW: Personality Data Store (`PersonalityStore.kt`)
- **Function**: Manages persistent storage for the `PersonalityProfile` and tallies various user behaviors using `SharedPreferences`.
- **Tag-Category Tally**: `incrementTagCategoryTally` and `loadTagCategoryTally` track how often tags from diary entries and goals are used, mapping them to predefined `TagCategory` labels.
- **Screen-Time Category Tally**: `rollupScreenCategoryTallyIfNeeded` and `loadScreenCategoryTally` accumulate cumulative screen time hours for each `AppCategory` daily, preventing double-counting.
- **Profile Unlock Logic**: `isUnlocked` determines if enough tag and screen time data has been collected (based on `TAG_TALLY_THRESHOLD`, `SCREEN_HOURS_THRESHOLD`, and `CATEGORIES_NEEDED`) to generate a personality profile.
- **Profile Management**: `saveProfile` and `loadProfile` handle serialization and deserialization of `PersonalityProfile` objects.
- **Dirty Flag**: `markDirty`, `clearDirty`, `isDirty` track whether the underlying data has changed, indicating if the cached profile might be stale and needs regeneration.

#### NEW: Gemini Personality Profile Generation (`PersonalityGemini.kt`)
- **Function**: Generates a poetic personality reflection for "Memo" using the **Google Gemini 2.5 Flash** model.
- **Input Prompt**: Constructs a detailed prompt based on summarized `tagCategoryTally` and `screenCategoryTally` data, adhering to specific stylistic and content rules (warm, observational, non-prescriptive, evolving phrasing).
- **Output & Parsing**: Expects and parses a strict JSON response into a `PersonalityProfile` object (tagline, 3 traits, 2-3 paragraphs).
- **Robustness**: Includes checks for `BuildConfig.GEMINI_API_KEY` and robust error logging for API failures.

#### NEW: Memo Store (`MemoStore.kt`)
- **Function**: Manages persistent storage for the user-defined name of the virtual companion, "Memo".

#### NEW: Dialogue System (e.g., `ui.data.DialoguePool`, `ui.data.DialogueCategory`, `ui.data.fillTemplate`)
- **Function**: Provides a structured way to manage and retrieve context-aware dialogue lines for the virtual companion. Supports different `DialogueCategory` types (e.g., `TASK_DONE`, `NEW_TASK`) and templating to insert dynamic values.

#### NEW: Task Timer Store (`TaskTimerStore.kt`)
- **Function**: Manages the persistent state of a single active task timer, including the task's ID, title, target duration, and the date it was started.
- **Key Functions**: `start`, `load`, `completeActiveTaskAndClear`, `clear` for managing the active task timer.

#### Pomodoro Store (`PomodoroStore.kt`)
- **Updated**: Manages the stopwatch/pomodoro timer's elapsed time, running state, and mode. Now works in conjunction with `TaskTimerStore` and is integrated into the `MainShell` for global control of task-based timers.
- **Key Functions**: `loadElapsedSeconds`, `isRunning`, `loadMode`, `setMode`, `start`, `pause`, `reset`.

#### Goal Store (`Goalstore.kt`)
- **Updated**: Its `incrementTagTally` and `loadTagTally` functions are directly integrated into the `PersonalityStore` to feed behavioral data for personality profile generation. Manages user-defined goals with checklists, schedules, categories, and tags.

#### Tag Categories (`Tagcategories.kt`)
- **Function**: Defines a shared set of `TagCategory` objects (e.g., Social, Work, Wellness, Lifestyle) and their associated tags. Provides utility functions (`categoryLabelForTag`, `categoryLabelsForTags`) used by `PersonalityStore` and `GoalStore` for consistent categorization of diary entries and goals, which in turn fuels the personality generation.

#### Other Backend Systems
- **Hardware Battery Reader** — `Intent.ACTION_BATTERY_CHANGED` listener.
- **Milestone Engine** — auto-unlocks achievements at screen time thresholds (0h, 2h, 4h, 6h, 12h).
- **SharedPreferences Storage** (`AppSettings.kt`) — persists daily limits, toggles, text scale.
- **Notification Manager** (`NotificationHelper.kt`) — screen time alerts at 80% and 100% of daily limit. Now also calls `createGoalNotificationChannels` for future goal-related notifications.
- **Pet State Decision Tree** (`ScreenTimeScreen.kt`) — computes mood and speech text from screen time, time of day, initial dialogue status, and pet name.

---

## 3. Remaining Features to Build

- **Activity Tree** — visual growth map in a new Garden tab with XP-based unlockables (UI exists, but core logic is pending).
- **Personality Profile** — Further integration and expansion of AI-generated summaries (e.g., deeper analysis, more dynamic updates).
- **Rewards Economy** — XP shop for pet clothing, accessories, room themes, tree decorations.
- **Pet Wardrobe** — equip purchased items from the shop onto Memogotchi.
- **Diary Conditional Locks** — date-locked, mood-locked, and milestone-locked entries.
- **Activity Verification** — photo submission + AI confirmation for completed tasks.
- **Interactive Push Notifications** — actionable battery check-in and goal reminders via notification buttons.
- **DND / Focus Mode & Pomodoro** — more comprehensive integration of these tools into the task flow for active sessions.