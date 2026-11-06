# Mindful Plant CBT

A CBT thought-reframing and mood-journal Android app, built for OPSC6312 (Portfolio of
Evidence). It guides a user through a structured cognitive-behavioural-therapy exercise:
describe a situation, capture the automatic thought, identify the cognitive distortion,
write a balanced reframe, and rate mood before and after. The app works offline first and
syncs to a REST API when a connection is available.

**Group member:** Percy Dube (ST10383359) · **Group member:** Abonga Magugu Nkosi (ST10298002)

---

## Features

### Core requirements
- [x] Register and log in, passwords hashed with bcrypt (never stored or sent in plain text)
- [x] Single sign-on with Google: Firebase Authentication signs the user in, then the Google
      ID token is exchanged for an API session through `POST /sso`
- [x] Settings: change display language and reminder time, and manage the profile photo
- [x] Multi-language support: English, isiZulu and Sesotho
- [x] REST API connected to a database (Node.js/Express + MongoDB Atlas)
- [x] Room (SQLite) for local, offline-first persistence
- [x] Offline sync: thought records are saved locally first, then pushed to the API by a
      WorkManager background job (or "Sync now" on the Mood History screen), and records the
      phone doesn't have yet are pulled down
- [x] Daily reminder notification at the chosen time, scheduled with `AlarmManager` and
      re-created after a reboot or app update

### Additional / innovative features
- [x] Day-streak counter
- [x] Mood-trend chart (Home and Mood History), computed entirely on-device from Room data
- [x] Guided cognitive-distortion picker with a plain-language description for each type
- [x] Rule-based Insights screen: trend detection, mood-lift stat, most-logged distortion with
      a CBT-style tip, best reframe of the week, milestone badges
- [x] Profile photo stored as a blob in MongoDB through the API, resized and compressed on the
      phone first, cached locally so it shows instantly and works offline

## Tech stack

| Layer | Choice |
|---|---|
| UI | Android Views + ViewBinding, Navigation Component, Material Components |
| Language | Kotlin, coroutines |
| Local storage | Room (SQLite) |
| Networking | Retrofit + OkHttp |
| Background work | WorkManager (sync), AlarmManager (exact daily reminder) |
| Auth | Firebase Authentication (Google Sign-In) + JWT sessions from the API |
| Backend | Node.js / Express, hosted on Render |
| Database | MongoDB Atlas |
| Testing | JUnit 4 (JVM unit tests) |
| CI/CD | GitHub Actions (unit tests on every push, signed release APK on tag) |

## Project structure

```
app/src/main/java/com/mindfulplant/cbt/
├── data/
│   ├── local/            # Room database, DAOs, entities
│   ├── remote/           # Retrofit API service, DTOs
│   └── repository/       # AuthRepository, RecordRepository, ProfilePhotoRepository (offline-first)
├── domain/               # Pure Kotlin logic: StreakCalculator, InsightsEngine
├── notifications/        # ReminderReceiver, BootReceiver, NotificationHelper
├── sync/                 # SyncScheduler, SyncWorker (WorkManager background sync)
├── ui/
│   ├── login/ register/  # Auth screens (email/password and Google)
│   ├── home/             # Dashboard: streak, mood chart, recent entries
│   ├── thoughtrecord/    # New Thought Record form
│   ├── history/          # Mood History list + "Sync now"
│   ├── insights/         # Trends, badges, best reframe
│   ├── settings/         # Language, reminder time, profile photo
│   └── widget/           # Custom MoodTrendChartView (Canvas-based, no chart library)
└── util/                 # SessionManager, ReminderPrefs, ReminderScheduler, ImageProcessor, AvatarUtil

app/src/test/java/com/mindfulplant/cbt/   # JVM unit tests (see Testing)
.github/workflows/                        # android-ci.yml, release.yml
```

## Setup

### Android app
1. Open the project in Android Studio (built against `compileSdk 35`, Kotlin, AGP 8.5.2,
   Gradle 8.7 and JDK 17; see `app/build.gradle.kts` for exact versions).
2. Add your own `app/google-services.json` from the Firebase console. It is listed in
   `.gitignore`, so it is not in the repository. For Google Sign-In to work, register your
   debug/release SHA-1 fingerprint in Firebase and re-download the file.
3. `API_BASE_URL` is set in `app/build.gradle.kts` as a `buildConfigField`, pointing at the
   deployed backend. Update it there if you redeploy the API elsewhere.
4. Run on a device or emulator (`minSdk 24`). On Android 13+ allow notifications, and on
   Android 12+ allow "Alarms & reminders" so the reminder fires at the exact time.

### Backend API
The API lives in a separate repository, `mindfulplantapi` (Node.js/Express + MongoDB Atlas,
deployable on Render's free tier). Endpoints:

| Method | Path | Purpose |
|---|---|---|
| POST | `/register`, `/login` | Email and password auth (bcrypt-hashed passwords, JWT session) |
| POST | `/sso` | Exchange a Google ID token for an API session |
| GET, POST | `/records` | Pull and push thought records (upserted by `recordId`) |
| PUT, GET, DELETE | `/profile/photo` | Profile photo blob storage |

Environment variables (see that repo's `.env.example`): `MONGODB_URI`, `JWT_SECRET`,
`PORT`, `GOOGLE_WEB_CLIENT_ID`.

## Testing

JVM unit tests run without an emulator (`./gradlew testDebugUnitTest`):

- `InsightsEngineTest`, `InsightsEngineRulesTest`: trend thresholds, messages, persistent
  decline rule, and which entries feed the Insights stats
- `StreakCalculatorTest`: consecutive days, gaps, same-day entries, unsorted input
- `ReminderSchedulerTest`, `ReminderSchedulerEdgeCaseTest`: delay until the next reminder time

## CI/CD

- **`android-ci.yml`** runs the unit tests on every push and pull request, and uploads the
  test report. If no `GOOGLE_SERVICES_JSON` secret is set, it writes a placeholder
  `google-services.json` so the tests can still run.
- **`release.yml`** builds a signed release APK and publishes it as a GitHub Release when the
  `final-PoE` tag is pushed. It needs these repository secrets: `KEYSTORE_BASE64`,
  `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` and `GOOGLE_SERVICES_JSON`.
- Locally, release signing reads `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and
  `KEY_PASSWORD` from environment variables or Gradle properties. Keystore files are
  git-ignored and must never be committed.

## Known limitations
- The API is hosted on Render's free tier, so the first request after a period of inactivity
  can be slow while the server wakes up. Sync retries with a growing delay.
- Sync is deliberately simple (a decision from the planning document): the API upserts
  records by `recordId` and there is no conflict-resolution logic beyond that.
- Exact reminder alarms can be switched off by the user on Android 12+. In that case the
  reminder still fires, but Android may delay it slightly.
- There is no change-password feature in the app or the API.
