# Mindful Plant CBT

A CBT thought-reframing and mood-journal Android app, built for OPSC6312 (Part 2 of the
Portfolio of Evidence). Guides a user through a structured cognitive-behavioural-therapy
exercise situation, automatic thought, cognitive distortion, balanced reframe, mood
before/after and tracks mood trends over time.

**Student:** Percy Dube (ST10383359) · **Group member:** Abonga Magugu Nkosi (ST10298002)

---

## Features

### Core requirements
- [x] Register and log in, passwords hashed with bcrypt (never stored or sent in plain text)
- [ ] Single sign-on via Google (Firebase Authentication) — **not yet implemented**; the button
      is in the UI but has no handler, and the Firebase Google OAuth client still needs to be
      configured (SHA-1 fingerprint + re-download of `google-services.json`)
- [x] Settings: change display language, reminder time, and password
- [x] REST API connected to a database (Node.js/Express + MongoDB Atlas)
- [x] Offline-first with manual sync records save to RoomDB first and sync on demand
- [x] RoomDB/SQLite for local persistence
- [ ] Real-time local push notifications at the chosen reminder time — **not yet implemented**;
      the time is saved and the manifest permissions are in place, but nothing schedules the
      actual `AlarmManager`/`WorkManager` alarm yet
- [x] Multi-language support (English, isiZulu, Sesotho) — mostly complete.

### Additional / innovative features
- [x] Day-streak counter
- [x] Mood-trend chart (Home and Mood History), computed entirely on-device from RoomDB data
- [x] Guided cognitive-distortion picker with a plain-language description for each type
- [x] Rule-based Insights screen: trend detection, mood-lift stat, most-logged distortion with
      a CBT-style tip, best reframe of the week, milestone badges
- [ ] Data export (JSON/CSV) not yet implemented

## Tech stack

| Layer | Choice |
|---|---|
| UI | Android Views + ViewBinding, Navigation Component, Material Components |
| Language | Kotlin, coroutines |
| Local storage | Room (SQLite) |
| Networking | Retrofit + OkHttp |
| Auth | Firebase Authentication (Google Sign-In — in progress) |
| Backend | Node.js / Express, hosted on Render |
| Database | MongoDB Atlas |
| Reminders | AlarmManager / WorkManager (planned) |

## Project structure

```
app/src/main/java/com/mindfulplant/cbt/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/          # Retrofit API service, DTOs
│   └── repository/      # AuthRepository, RecordRepository (offline-first sync logic)
├── domain/               # Pure Kotlin logic: StreakCalculator, InsightsEngine
├── ui/
│   ├── login/ register/  # Auth screens
│   ├── home/             # Dashboard: streak, mood chart, recent entries
│   ├── thoughtrecord/    # New Thought Record form
│   ├── history/          # Mood History list + sync
│   ├── insights/         # Trends, badges, best reframe
│   ├── settings/         # Language, reminders, password, sync
│   └── widget/           # Custom MoodTrendChartView (Canvas-based, no chart library)
└── util/                 # SessionManager, ReminderPrefs
```

## Setup

### Android app
1. Open the project in Android Studio (this was built against `compileSdk 35`, Kotlin, AGP —
   see `app/build.gradle.kts` for exact versions).
2. `google-services.json` is already in `app/` for Firebase; if you regenerate it in the
   Firebase console, drop the new copy in the same place.
3. `API_BASE_URL` is set in `app/build.gradle.kts` as a `buildConfigField`, pointing at the
   deployed backend. Update it there if you redeploy the API elsewhere.
4. Run on a device or emulator (`minSdk 24`).

### Backend API
The API lives in a separate repository (`mindfulplant-api`) — a small Express app with
`/register`, `/login`, `/change-password`, and `/records` (GET/POST), backed by MongoDB Atlas.
See that repo's own README for deployment steps (it's set up to deploy on Render's free tier).

## Known limitations
- Google Sign-In and local push notifications are UI-ready but not functionally wired up yet
  (see checklist above) planned for the next iteration.
- Sync is one-way, on-demand ("Sync now"), not continuous background sync, and assumes one
  device per user (last-write-wins, no conflict merging) this was a deliberate scope
  decision from the Part 1 planning document, not an oversight.
- A handful of UI strings added late in development haven't been translated into isiZulu or
  Sesotho yet.
