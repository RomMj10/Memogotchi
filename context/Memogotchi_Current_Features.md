# Memogotchi Current Features & Status Reference

This document maps the planned features of Memogotchi (from the Feature Reference Document) against the currently implemented codebase.

---

## 1. Needed Features

These are the features defined in the concept stage as necessary for the complete Memogotchi experience:

- **Memogotchi Companion**:
  - Virtual pet that reacts to screen time; weakens with high screen time and recovers offline.
  - Dynamic, expressive visual pet states (representing mood, wellness, and screen health).
  - Customizable items, clothes, accessories, and pet customization themes.
- **Analog Twin**:
  - Recommends offline activities and alternatives matched to user interests (e.g., cooking, book reading, physical fitness).
  - Suggestions triggered based on app category usage (from Digital Wellbeing screen time statistics) and device state.
- **Activity Tree**:
  - A visual habit progress tree that grows based on completed offline habits and logging events.
  - Unlocks decorative items/upgrades using reward points.
- **Personality Profile**:
  - AI-generated poetic summary reflecting the user's changing habits, interests, and milestones over time.
- **Battery System**:
  - Four distinct wellness batteries: Emotional, Social, Physical, and Motivation batteries.
  - Aggregates logs into an overall wellness percentage, triggering tailored restorative recommendations.
- **Diary & Time Capsule**:
  - Private reflection journal supporting goal-setting and photo attachments.
  - Conditional lock systems (date/mood/milestone-locked) allowing users to write to their future selves.
- **Activity Verification & Rewards**:
  - Verification of completed offline tasks (e.g., via AI-based photo verification).
  - Rewards economy to purchase clothing, pet accessories, room themes, and tree decorations.
  - Built-in Focus Mode, Do Not Disturb (DND), and Pomodoro/stopwatch timer tools to aid during tasks.

---

## 2. Current Features It Has

### a.) Frontend Features (UI & Interactive Components)

- **Main Screen shell & Custom Bottom Navigation Bar (`MainActivity.kt`)**:
  - Implements dynamic custom bottom navigation with 4 main pages: **Pet**, **Screen Time**, **Tasks**, and **Settings**.
  - Fully responsive Compose layout utilizing `WindowSizeClass` to support adaptive scaling for both mobile and tablet devices.
  - Dynamic accessibility text size scaling powered by a custom LocalDensity composition provider.
- **Interactive Pet Screen (`PetScreen.kt`)**:
  - **Lottie Animations**: Renders floating, expressive animations for different pet states (idle, happy, concerned, tired, alarmed) using raw Lottie resource files.
  - **Speech Bubble Row**: Displays customizable context-aware speech bubbles containing direct comments/alerts from the pet.
  - **Stopwatch Focus Timer**: A beautiful stopwatch ("STAY OFF YOUR PHONE") counting elapsed seconds. Supports full tap-to-pause/resume functionality, styled with a ticking glow and a dual-colored progress track indicating time spent focusing.
  - **Stats Bar**: Floating dashboard metrics displaying hardware battery levels, acquired XP points, and total screen time formatted dynamically.
- **Screen Time Dashboard (`ScreenTimeScreen.kt`)**:
  - **Interactive Bar Chart**: Renders a custom-drawn 7-day bar chart showing total daily screen time. Clicking any bar dynamically updates the panel data for that specific day.
  - **Responsive Dual Layouts**: Supports `PhoneLayout` (nested tab-panels) and `TabletLayout` (side-by-side columns) based on device screen classification.
  - **Custom App Detail Lists**: Displays all tracked apps for the selected day along with their official launch icons, package name labels, duration readouts, and a category percentage fill bar.
  - **Interactive State Prompts**: Incorporates permission request screens requesting usage access logs if permission is missing.
- **Interactive Tasks & Achievements Screen (`TaskScreen.kt`)**:
  - **Focus Dashboard Header**: Summarizes task statistics, total focus time, and a dynamic hardware battery energy meter changing color thresholds (Red, Orange, Green) based on device level.
  - **Suggestive Task Cards**: Displays custom, styled interactive task rows with categorization symbols, description parameters, customized duration badges, and specific trigger reason pills (e.g., "YouTube: 2h 4m today"). Includes custom tap-to-toggle tick animations marking a task as done.
  - **Achievements Milestone Tracker**: Dynamically lists unlocked and locked screen-time achievements with custom visual indicators, lock states, unlocked milestone icons, and description milestones.
- **Settings Screen (`SettingsScreen.kt`)**:
  - **Interactive Slider Dialog**: Adjusts screen limit parameters using a sliding drawer increments of 15 minutes (ranging from 30 min to 8 hours).
  - **Preferences Settings Toggles**: Fully operational switches enabling/disabling Strict Mode, Monochrome UI, Health Alerts, and Reduce Motion.
  - **Dynamic Text Size Selector**: Interactive SMALL, NORMAL, and LARGE buttons instantly scaling the entire application UI layout.

---

### b.) Backend Features (Business Logic, Services & Engines)

- **Millisecond Screen Time Tracking Engine (`ScreenTimeScreen.kt` / `MainActivity.kt`)**:
  - Interacts with system-level `UsageStatsManager` in background coroutine threads to query system usage events.
  - Reconstructs precise daily app foreground durations by processing matching `ACTIVITY_RESUMED` and `ACTIVITY_PAUSED` events, including handling current open app edge cases.
- **Automated App Categorization Engine (`AnalogTaskEngine.kt`)**:
  - Inspects package metadata using standard system flags (API 26+) to resolve categorical types (Social, Games, Video/Entertainment, Productivity, etc.).
  - Supported by a comprehensive keyword-based fallback regex mapping covering popular modern applications (such as Instagram, TikTok, Roblox, King, ChatGPT, Gemini, YouTube, Slack, etc.).
- **Dynamic Task Recommendation Engine (Local Rule-Based) (`AnalogTaskEngine.kt`)**:
  - Evaluates categorized app metrics against screen-time thresholds, generating context-specific recommendations:
    - *Social Usage >= 1.5h*: Triggers personal letters/notes.
    - *Social Usage >= 3h*: Triggers face-to-face meetups.
    - *Gaming Usage >= 1h*: Triggers walking/breaks.
    - *Gaming Usage >= 2.5h*: Triggers analog games (board/chess).
    - *Entertainment Usage >= 2h*: Triggers physical reading.
    - *Browsing Usage >= 1.5h*: Triggers offline journaling.
    - *Total Screentime >= 2h*: Triggers quick posture stretching.
    - *Total Screentime >= 4h*: Triggers phone-free food prep.
    - *Low Battery (<20%)*: Triggers a "Put the phone down to charge" offline task.
- **Generative AI Task Engine (`GeminiTaskEngine.kt`)**:
  - Integrates the Google Generative AI Android SDK to communicate directly with the **Gemini 1.5 Flash** model.
  - Supplies structured prompts detailing current daily screen time, battery readings, and top categorized apps.
  - Enforces JSON schema responses and parses returned strings safely into `AnalogTask` entities.
  - Implements auto-fallback to local rule-based generation if API calls fail or credentials are omitted.
- **Hardware Battery Level Reader (`AnalogTaskEngine.kt`)**:
  - Registers intent receivers listening to system-level `Intent.ACTION_BATTERY_CHANGED` to retrieve actual device battery percentages.
- **Automated Milestone Engine (`AnalogTaskEngine.kt`)**:
  - Automatically assesses today's screen time to unlock achievements at defined thresholds (e.g., First Check-in at 0h, 2-Hour Mark, Half a Workday at 4h, Heavy User at 6h, Full Day at 12h).
- **Persistent SharedPreferences Storage (`AppSettings.kt`)**:
  - Implements persistent key-value configuration storage for global properties like daily limits, health notification settings, and preferred scale options.
- **Real-Time Notification Delivery Manager (`NotificationHelper.kt`)**:
  - Configures a custom system notification channel ("Screen Time Alerts") on Android Oreo and above.
  - Dynamically issues system-level status notifications when the user reaches 80% of their daily screen limit, or exceeds it completely.
- **Pet State Decision Tree (`ScreenTimeScreen.kt`)**:
  - Computes the pet's mood state (Happy, Idle, Concerned, Tired, Alarmed) and custom speech texts based on the combination of daily total usage and time-of-day variables.
