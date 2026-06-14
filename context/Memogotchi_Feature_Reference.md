# Memogotchi
*Me + Memo + go (egg) + tchi = Memogotchi*

**Feature Reference Document**

---

## 1. Overview

Memo is a wellness app that turns your digital habits into self-awareness. It combines three core ideas into one cohesive experience:

- A virtual pet (Memogotchi) that reflects your screen health
- An analog twin that learns your interests and nudges you toward offline alternatives
- A diary with time-capsule entries you write to your future self

Unlike traditional virtual pets that thrive on constant attention, Memogotchi weakens when the user doom-scrolls and recovers health when the user disconnects. The pet observes your usage patterns and suggests real-world alternatives. Over time, Memo builds a personality profile based on your activity — and it grows especially when you go offline.

---

## 2. Feature Summary

| Feature | Description |
|---|---|
| **Memogotchi** | Virtual pet that reacts to screentime; weakens with high screentime and recovers offline. Includes visual health indicator and dynamic pet behaviors. |
| **Analog Twin** | Observes user behavior and provides offline activity suggestions triggered by screentime levels or battery system readings. Tracks app category usage and duration via Digital Wellbeing data. |
| **Activity Tree** | Visual growth map representing the user's offline and online habit history over time. |
| **Personality Profile** | AI-generated poetic identity summary that evolves as the Activity Tree grows. |
| **Battery System** | Overall wellness percentage input broken down into four categories: Emotional, Social, Physical, and Motivation batteries. |
| **Diary** | Private journal with conditional lock system (date/mood/self-unlocked). Supports photo attachments and goal setting. |
| **Activity Verification** | Users can accept offline activity suggestions, trigger DND/focus mode or a Pomodoro timer, and later confirm completion via a follow-up prompt. |

---

## 3. Feature Details

### 3.1 Memogotchi Companion

At the center of the application is Memogotchi — a virtual pet that directly reflects the user's digital well-being. Its health, mood, and growth are driven by screen time habits and lifestyle choices.

- The pet weakens when users spend excessive time on distracting applications.
- The pet gradually recovers when users engage in productive or offline activities.
- Visual indicators and behaviors change in real time based on usage patterns.
- The pet's appearance and state serve as a constant, ambient reminder of the user's digital balance.

---

### 3.2 Analog Twin

The Analog Twin acts as an intelligent behavioral companion that learns from the user's interests and activity patterns. It analyzes data such as:

- App usage categories and screen time duration
- Music preferences and listening history
- Video viewing habits
- Wellness inputs from the Battery System

Based on these patterns, the Analog Twin recommends offline alternatives aligned to the user's interests. Examples:

- A user who frequently watches cooking videos may be encouraged to try a new recipe.
- A user who listens to fitness podcasts may be prompted to take a walk or exercise.
- A user who doom-scrolls may be suggested journaling, reading, or a creative hobby.

The Analog Twin bridges digital interests with real-world experiences.

---

### 3.3 Activity Tree

The Activity Tree is a visual growth map that tracks the evolution of the user's offline and online habits. It grows and changes over time as the user completes offline activities and logs wellbeing data.

- Reflects the user's progress visually — healthy habits grow the tree.
- Decorative elements can be unlocked using reward points earned via Activity Verification.
- Serves as a motivational record of the user's journey over time.

---

### 3.4 Personality Profile

As users interact with the application, Memogotchi continuously develops a Personality Profile. Using AI-generated insights, the system creates a poetic and reflective summary of the user's evolving interests, habits, strengths, and personal growth.

- The profile is updated over time as the Activity Tree evolves.
- Rather than labels, it highlights meaningful patterns and behavioral changes.
- Provides a unique, personalized narrative of the user's wellness journey.

---

### 3.5 Battery System

The Battery System converts emotional and motivational states into a simple, actionable visual metric. Users regularly log their levels across four categories:

- **Emotional Battery** — how emotionally charged or drained the user feels
- **Social Battery** — energy levels for social interaction
- **Physical Battery** — physical wellness and energy
- **Motivation Battery** — drive and motivation levels

These values combine into an overall wellness percentage. When certain battery levels drop low, Memogotchi and the Analog Twin proactively suggest activities to help restore them.

---

### 3.6 Diary & Time Capsule

The Diary feature provides a private space for reflection and self-expression. Users can create journal entries that may include:

- Written reflections
- Personal goals
- Photo attachments
- Messages to their future self

Entries can be locked behind custom conditions such as:

- Specific dates (birthdays, anniversaries)
- Particular moods or emotional states
- Personal milestones or self-unlocking challenges

This creates a time-capsule experience where users can communicate with their future selves and revisit important moments when they matter most.

---

### 3.7 Activity Verification & Rewards

When the Analog Twin suggests an offline activity — such as reading, cycling, drawing, or spending time outdoors — the user can choose to accept the challenge. Upon completing the activity, the user may submit a photo as proof. The system uses AI image recognition or predefined verification methods to confirm the activity was completed.

Once verified, users earn reward points that can be used to purchase:

- Clothing and accessories for Memogotchi
- Pet customization items
- Decorative room themes
- Activity Tree decorations
- Diary customization options

During the activity, the user can trigger DND / Focus Mode or start a Pomodoro / stopwatch timer directly from the suggestion. Memo will follow up afterward to ask if the user completed the suggested activity.

---

## 4. Data Source Integration (APIs)

The following platforms and APIs are being explored as potential data sources to power Memogotchi's behavior tracking and Analog Twin suggestions. This section is a reference for what data can realistically be obtained and how — it does not represent finalized integrations.

### 4.1 API Reference Table

| Platform | API / Method | What It Can Track | Access Level |
|---|---|---|---|
| YouTube | YouTube Data API v3 | Watch history, video categories, liked videos, subscriptions, watch duration | Official, free with OAuth login |
| Spotify | Spotify Web API | Listening history, genres, moods, top artists and tracks | Official, free with OAuth login |
| TikTok | Data Portability (manual export) | Watch history, liked videos, search history | Manual — user exports file from settings |
| Instagram | Data Download (manual export) | Reels watched, liked posts, search history | Manual — user exports file from settings |
| Reddit | Reddit API (PRAW) | Subreddits followed, upvoted posts, browsing patterns | Official, free with OAuth login |
| Android | Digital Wellbeing API | App usage duration, unlock frequency, notification count | Official, device-level access |
| iOS | Screen Time API | App usage duration, category limits | Restricted — primarily for parental controls |
| Apple Health | HealthKit API | Steps, activity, sleep, heart rate | Official, opt-in, pairs with analog verification |
| Google Fit | Google Fit REST API | Physical activity, steps, workout sessions | Official, opt-in, pairs with analog verification |

### 4.2 Integration Notes

- **Official APIs** (YouTube, Spotify, Reddit, Android, Google Fit, HealthKit) can be integrated directly via OAuth, providing real-time or near-real-time data.
- **Manual exports** (TikTok, Instagram) require user action — the user downloads their data package from the platform's settings and uploads it to Memogotchi for processing.
- **iOS Screen Time API** is restricted to parental control apps; standard third-party access is not available. Alternative approaches (e.g., Device Activity Framework on iOS 16+) may be explored.
- All data collection should be governed by explicit user consent, with privacy-first design as a core principle.

---

## 5. Notes for Development Reference

This document reflects the features described in the current Memogotchi concept. It is intended to serve as a baseline reference to track what has been defined and what remains to be built or validated.

- Features listed in Sections 2 and 3 represent the core scope of the current development plan.
- The API section (Section 4) is exploratory — actual integration will depend on feasibility, platform restrictions, and user privacy requirements.
- The Personality Profile and pet behaviors (marked with "AI? algo?" in the original concept) are still pending a decision on whether to use an AI model or a rules-based algorithm.
- Activity Verification photo confirmation and the reward system are defined at the concept level and will need technical planning for implementation.

---

*Memogotchi Feature Reference Document — v1.0*
