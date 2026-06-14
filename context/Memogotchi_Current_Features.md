# Memogotchi Current Features & Status Reference

*Updated after Wellness Page implementation*

---

## 1. Feature Status Overview

| Feature | Status | Notes |
|---|---|---|
| **Memogotchi Companion** | Partial | Lottie animations, pet states, speech bubble, and focus timer all working. Wardrobe/customization UI stubbed but no reward economy yet. |
| **Analog Twin** | Partial | Local rule-based task engine and Gemini 1.5 Flash integration both working. Suggestions displayed in Tasks tab with trigger reason pills. |
| **Activity Tree** | Not started | No visual habit tree yet. Placeholder planned under a future Garden tab. |
| **Personality Profile** | Not started | AI-generated summary not yet implemented. Depends on diary and battery data accumulation. |
| **Battery System** | Done | Full four-battery logging (Emotional, Social, Physical, Motivation). Vertical sliders, icon states per tier, overall % card with fill animation. State persists across tab switches. |
| **Diary & Time Capsule** | Partial | FAB with 5 options, diary entry screen with optional sliders, category chips, and text entry. Entries display in diary list. Conditional locks and photo attachments not yet built. |
| **Activity Verification** | Partial | Task tick animations and achievement milestones working. Photo verification, AI confirmation, and DND/Pomodoro tools not yet implemented. |
| **Rewards Economy** | Not started | XP points tracked in stats bar but no shop or spending system built yet. |
| **Navbar & Shell** | Done | 5-tab nav: Pet, Wellness, Screen Time, Tasks. Settings moved to gear icon on Pet tab. Status bar padding fixed globally. |
| **Pet Focus Timer** | Done | Stopwatch counts up continuously even when switching tabs. Tap to pause/resume. Persists across all pages via MainShell state hoisting. |
| **Screen Time Dashboard** | Done | 7-day bar chart, per-app breakdown with icons, category fill bars, responsive phone/tablet layouts, and usage permission prompts all working. |
| **Notifications** | Partial | Passive alerts at 80% and 100% of daily screen limit working. Interactive push notification for battery check-in not yet implemented. |

---

## 2. Current Features in Detail

### a. Frontend — UI & Interactive Components

#### Main Shell & Navigation (`MainActivity.kt`)
- 5-tab bottom navigation: Pet, Wellness, Screen Time, Tasks.
  - Settings removed from navbar — accessible via gear icon on Pet tab.
  - Wellness tab added as second position after Pet.
- State hoisting: Wellness battery states, slider values, diary entries, and pet timer all live in `MainShell` so data persists across tab switches.
- Status bar padding applied globally so no page content overlaps system notifications.
- Responsive Compose layout with `WindowSizeClass` for phone and tablet.
- Dynamic text size scaling via custom `LocalDensity` provider.

#### Pet Screen (`PetScreen.kt`)
- Lottie animations for pet states: idle, happy, concerned, tired, alarmed.
- Speech bubble with context-aware messages based on screen time and time of day.
- Focus stopwatch counting up continuously — persists even when navigating to other tabs.
  - Tap to pause/resume. Timer `LaunchedEffect` runs in `MainShell`, not `PetScreen`.
- Stats bar: device battery %, XP earned, daily screen time total.
- Gear icon in top-right opens Settings overlay with back navigation.

#### Wellness Screen (`WellnessScreen.kt`) — NEW
- Overall battery card with horizontal fill animation reflecting average of all 4 states.
  - Fill color lerps from dark red (low) to dark green (high).
  - Percentage shown in a circle on the right side of the card.
- Four battery states: Emotional, Social, Physical, Motivation.
  - Icons change per tier on submit: 0–20% broken/low, 21–40% muted, 41–60% neutral, 61–80% active, 81–100% peak.
  - All 4 vertical sliders expand together when tapping + or any state icon.
  - Sliders use `rememberUpdatedState` to correctly track drag position at any percentage.
  - Thumb icon stays fixed during drag; main state icons only update on submit.
- Diary section with FAB (floating action button).
  - FAB fans 5 options upward in arc animation: Today, Yesterday, Other Day, Voice, Photo.
  - FAB rotates to ✕ when expanded; scrim darkens background.
  - Date picker (`DatePickerDialog`) for Other Day — shows once, formats date as Month D, YYYY.
  - Diary entry screen: optional sliders, category chips (multi-select), text field.
  - Submit blocked if text field is empty; shows inline error banner.
  - Saved entries appear in diary list with date label and selected categories.

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

#### Settings Screen (`SettingsScreen.kt`)
- Now accessed via gear icon on Pet tab instead of a dedicated nav slot.
- Screen limit slider (30 min to 8 hours in 15-min increments).
- Toggles: Strict Mode, Monochrome UI, Health Alerts, Reduce Motion.
- Text size selector: Small, Normal, Large — scales entire app instantly.

---

### b. Backend — Business Logic & Engines

#### Screen Time Tracking Engine
- `UsageStatsManager` queries in background coroutines.
- Reconstructs foreground durations via `ACTIVITY_RESUMED` / `ACTIVITY_PAUSED` event pairs.

#### App Categorization Engine (`AnalogTaskEngine.kt`)
- System package flags (API 26+) for category resolution.
- Keyword-based regex fallback covering Instagram, TikTok, YouTube, Roblox, ChatGPT, Gemini, Slack, and more.

#### Task Recommendation Engine (`AnalogTaskEngine.kt`)
- Local rule-based engine with screen time thresholds:
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

#### Other Backend Systems
- **Hardware Battery Reader** — `Intent.ACTION_BATTERY_CHANGED` listener.
- **Milestone Engine** — auto-unlocks achievements at screen time thresholds (0h, 2h, 4h, 6h, 12h).
- **SharedPreferences Storage** (`AppSettings.kt`) — persists daily limits, toggles, text scale.
- **Notification Manager** (`NotificationHelper.kt`) — screen time alerts at 80% and 100% of daily limit.
- **Pet State Decision Tree** — computes mood and speech text from screen time + time of day.

---

## 3. Remaining Features to Build

- **Activity Tree** — visual growth map in a new Garden tab with XP-based unlockables.
- **Personality Profile** — AI-generated poetic summary fed by battery logs and diary entries.
- **Rewards Economy** — XP shop for pet clothing, accessories, room themes, tree decorations.
- **Pet Wardrobe** — equip purchased items from the shop onto Memogotchi.
- **Diary Conditional Locks** — date-locked, mood-locked, and milestone-locked entries.
- **Diary Photo Attachments** — photo support inside diary entries.
- **Activity Verification** — photo submission + AI confirmation for completed tasks.
- **Interactive Push Notifications** — actionable battery check-in via notification buttons.
- **DND / Focus Mode & Pomodoro** — built into the task flow for active sessions.
- **Voice & Photo diary modes** — currently stubbed in FAB, logic not yet implemented.