# Mindful Plant CBT

[![Android CI](https://github.com/PSYCHOHITMAN/MindfulPlantCBT/actions/workflows/android-ci.yml/badge.svg)](https://github.com/PSYCHOHITMAN/MindfulPlantCBT/actions/workflows/android-ci.yml)

A cognitive-behavioural-therapy (CBT) thought-reframing and mood-journal Android app, built
for OPSC6312 (Portfolio of Evidence).

**Group members:** Percy Dube (ST10383359) · Abonga Magugu Nkosi (ST10298002)

**Contents:** [Purpose](#1-purpose-of-the-app) · [Screenshots](#2-screenshots) ·
[Features](#3-features) · [Design considerations](#4-design-considerations) ·
[Architecture](#5-architecture) · [GitHub and GitHub Actions](#6-github-and-github-actions) ·
[Testing](#7-testing) · [Release notes](#8-release-notes) ·
[Publishing](#9-publishing-on-google-play) · [Setup](#10-setup) ·
[Known limitations](#11-known-limitations)

---

## 1. Purpose of the app

Anxious or low moods are often driven by automatic thoughts that feel true in the moment but
are distorted, such as catastrophising or mind-reading. CBT teaches people to catch the
thought, name the distortion, and write a more balanced reframe. That exercise is usually done
on paper worksheets, which are easy to lose and hard to look back on.

Mindful Plant CBT turns that worksheet into a short daily habit on a phone. For each
**thought record** the user:

1. describes the situation,
2. writes the automatic thought,
3. picks the cognitive distortion from a guided list with plain-language descriptions,
4. writes a balanced reframe, and
5. rates their mood before and after (1 to 5).

Over time the app shows the user their own patterns: a day streak, a mood-trend chart, how much
the exercise lifts their mood, which distortion they log most, and their best reframe of the
week.

**Who it is for:** anyone who wants a private, low-effort way to practise CBT thought records,
in English, isiZulu or Sesotho.

> Mindful Plant CBT is a self-reflection tool. It is not a medical device and does not
> diagnose, treat or replace professional mental-health care. Anyone in crisis should contact
> a qualified professional or local emergency services.

## 2. Screenshots

| Home | New thought record | Mood history |
|---|---|---|
| ![Home screen](docs/screenshots/home.png) | ![New thought record](docs/screenshots/thought-record.png) | ![Mood history](docs/screenshots/history.png) |

| Insights | Settings | Login |
|---|---|---|
| ![Insights screen](docs/screenshots/insights.png) | ![Settings screen](docs/screenshots/settings.png) | ![Login screen](docs/screenshots/login.png) |

## 3. Features

### Core requirements
- [x] Register and log in, with passwords hashed with bcrypt (never stored or sent in plain text)
- [x] Single sign-on with Google: Firebase Authentication signs the user in, then the Google
      ID token is exchanged for an API session through `POST /sso`
- [x] Settings: display language, reminder time and profile photo
- [x] Multi-language support: English, isiZulu and Sesotho
- [x] REST API connected to a database (Node.js/Express + MongoDB Atlas)
- [x] Room (SQLite) for local, offline-first persistence
- [x] Offline sync: records are saved locally first, pushed by a WorkManager background job
      (or "Sync now"), and records missing from the phone are pulled down
- [x] Daily reminder notification at the chosen time, scheduled with `AlarmManager` and
      re-created after a reboot or app update

### Additional / innovative features
- [x] Day-streak counter
- [x] Mood-trend chart (Home and Mood History), computed entirely on-device
- [x] Guided cognitive-distortion picker with a plain-language description for each type
- [x] Rule-based Insights screen: trend detection, mood-lift stat, most-logged distortion with
      a CBT-style tip, best reframe of the week, milestone badges
- [x] Profile photo stored as a blob in MongoDB through the API, cropped and compressed on the
      phone first and cached locally so it works offline

## 4. Design considerations

**Offline first.** Journaling should never fail because the signal is bad. Room is the source
of truth: every thought record is written locally with a `pending` sync status, then pushed to
the API when a connection exists. WorkManager waits for a network, retries with growing delays
if the server is unreachable, and the API upserts by `recordId`, so syncing repeatedly never
creates duplicates. The profile photo follows the same pattern.

**Privacy of the user's data.** Thought records can be personal, so the analysis stays on the
device. The Insights screen is plain Kotlin rules with no machine learning and no external
analysis service. Passwords are hashed with bcrypt, sessions use JWTs, and Android backups of
app data are switched off (`allowBackup="false"`).

**Testable logic.** `InsightsEngine` and `StreakCalculator` are pure Kotlin objects with no
Android dependencies. The same streak number is used on Home and Insights, and the logic can
be unit tested on a plain JVM in CI without an emulator.

**Reminders that arrive on time.** WorkManager only guarantees "no earlier than", so a reminder
could be minutes late on a sleeping phone. The app uses an exact, Doze-friendly `AlarmManager`
alarm, falls back to an inexact one if the user disables exact alarms, and re-arms the alarm
after a reboot or app update.

**Language.** Strings are translated into English, isiZulu and Sesotho, and the app uses
Android's per-app language support so the choice is remembered.

**Light, simple UI.** Android Views with ViewBinding, the Navigation Component and Material
Components. The mood chart is a small custom Canvas view, so there is no chart library to
maintain. Profile photos are resized to about 512 x 512 px and compressed (EXIF rotation fixed,
centre-cropped) so they are cheap to store and upload.

## 5. Architecture

![Architecture diagram](docs/images/architecture.svg)

```
app/src/main/java/com/mindfulplant/cbt/
├── data/
│   ├── local/            # Room database, DAOs, entities
│   ├── remote/           # Retrofit API service, DTOs
│   └── repository/       # AuthRepository, RecordRepository, ProfilePhotoRepository
├── domain/               # Pure Kotlin logic: StreakCalculator, InsightsEngine
├── notifications/        # ReminderReceiver, BootReceiver, NotificationHelper
├── sync/                 # SyncScheduler, SyncWorker (WorkManager background sync)
├── ui/                   # login, register, home, thoughtrecord, history, insights, settings, widget
└── util/                 # SessionManager, ReminderPrefs, ReminderScheduler, ImageProcessor, AvatarUtil
```

| Layer | Choice |
|---|---|
| UI | Android Views + ViewBinding, Navigation Component, Material Components |
| Language | Kotlin, coroutines |
| Local storage | Room (SQLite) |
| Networking | Retrofit + OkHttp |
| Background work | WorkManager (sync), AlarmManager (exact daily reminder) |
| Auth | Firebase Authentication (Google Sign-In) + JWT sessions from the API |
| Backend | Node.js / Express on Render |
| Database | MongoDB Atlas |
| Testing | JUnit 4 (JVM unit tests) |
| CI/CD | GitHub Actions |

## 6. GitHub and GitHub Actions

**Repositories.** The project is split into two GitHub repositories:
[MindfulPlantCBT](https://github.com/PSYCHOHITMAN/MindfulPlantCBT) (this Android app) and
[mindfulplantapi](https://github.com/PSYCHOHITMAN/mindfulplantapi) (the Node/Express API).
GitHub is the shared home of the code for both group members, keeps the history of every
change, and is where the pipelines run.

![CI/CD pipeline diagram](docs/images/cicd-pipeline.svg)

**`android-ci.yml`: continuous integration.** On every push and pull request it checks out the
code, sets up JDK 17 and Gradle (with caching), runs the unit tests with
`./gradlew testDebugUnitTest`, and uploads the test report as an artifact. Two things the
runner needs are handled in the workflow:

- `gradlew` is committed without the executable bit, so the workflow runs `chmod +x gradlew`.
- `app/google-services.json` is git-ignored because it holds project keys. The workflow
  decodes it from the `GOOGLE_SERVICES_JSON` secret, or writes a placeholder so the tests can
  still run (they never contact Firebase).

**`release.yml`: continuous delivery.** Pushing the tag `final-PoE` triggers a release build.
It first checks that all the secrets exist, runs the unit tests again, then builds a **signed**
release APK and an Android App Bundle (AAB), verifies the signature with `apksigner`, and
publishes both files as a GitHub Release with these release notes.

**Secrets.** Nothing sensitive is committed. The signing keystore and Firebase config are stored
as encrypted repository secrets and decoded only inside the runner:

| Secret | Purpose |
|---|---|
| `KEYSTORE_BASE64` | Release keystore (base64) |
| `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | Signing credentials |
| `GOOGLE_SERVICES_JSON` | Firebase config (base64) |

Keystore files (`*.jks`, `*.keystore`) are git-ignored. Locally, signing reads the same names
from environment variables or Gradle properties.

## 7. Testing

JVM unit tests run without an emulator (`./gradlew testDebugUnitTest`) and run automatically in
GitHub Actions:

- `InsightsEngineTest`, `InsightsEngineRulesTest`: trend thresholds (±0.3), each insight
  message, the persistent-decline rule, and which entries feed the Insights stats
- `StreakCalculatorTest`: consecutive days, gaps, several entries on one day, unsorted input
- `ReminderSchedulerTest`, `ReminderSchedulerEdgeCaseTest`: delay until the next reminder time

## 8. Release notes

### Mindful Plant CBT 2.0 (final PoE release)

Compared with the Part 2 prototype (1.0).

#### Innovative features added in this version

| Feature | What it does |
|---|---|
| **Google single sign-on** | "Continue with Google" signs the user in through Firebase Authentication. The Google ID token is then exchanged for the app's own API session through the new `POST /sso` endpoint, so Google and email accounts behave the same way afterwards. In the prototype the button existed but did nothing. |
| **Daily reminder notifications** | A notification arrives at the time the user picks in Settings. It is scheduled with `AlarmManager`, re-armed after a reboot or app update, and works with the Android 13+ notification permission and the Android 12+ exact-alarm setting. |
| **Offline-first background sync** | Thought records are always saved on the phone first. A WorkManager job pushes them to the API as soon as a connection is available (with retry and back-off), and "Sync now" remains for manual syncing. Records missing from the phone are pulled down, so a fresh install gets the user's history back. |
| **Multi-language support** | The whole app is available in English, isiZulu and Sesotho, switchable in Settings and remembered. |
| **Profile photo (blob storage)** | Users can add a profile photo from Settings. The phone crops and compresses it to a small square JPEG, caches it so it works offline, and stores it as binary data in MongoDB through `PUT/GET/DELETE /profile/photo`. |

#### Other updates since the prototype
- **API:** new endpoints `POST /sso` and `/profile/photo`; records are upserted by `recordId` so
  repeated syncs never create duplicates.
- **Testing:** JVM unit tests for the Insights engine, streak calculator and reminder
  scheduling (35 tests).
- **GitHub Actions:** CI runs the tests on every push and pull request. Pushing the `final-PoE`
  tag builds a signed release APK and Android App Bundle (AAB) and publishes them as a GitHub
  Release.
- **Release readiness:** release signing is configured without committing any keys; the app
  targets Android 16 (API 36) as Google Play now requires; version raised to 2.0
  (versionCode 2).
- **Cleanup:** the README no longer lists a change-password feature, because the app and API
  do not have one.

#### Carried over from the prototype
Day-streak counter, mood-trend chart, guided cognitive-distortion picker, and the rule-based
Insights screen (trend detection, mood-lift stat, most-logged distortion with a CBT tip, best
reframe of the week, milestone badges).

## 9. Publishing on Google Play

The release pipeline produces a signed AAB, which is the format Google Play requires, and the
app targets API 36. The full step-by-step checklist (developer account, store listing, data
safety form, privacy policy, closed testing and production access) is in
[docs/PLAY_STORE_CHECKLIST.md](docs/PLAY_STORE_CHECKLIST.md), and the app's privacy policy is
in [docs/PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md).

## 10. Setup

### Android app
1. Open the project in Android Studio (Kotlin, AGP 8.13, Gradle 8.13, JDK 17, `compileSdk 36`;
   see `app/build.gradle.kts`).
2. Add your own `app/google-services.json` from the Firebase console. It is git-ignored. For
   Google Sign-In to work, register your debug/release SHA-1 fingerprint in Firebase and
   re-download the file.
3. `API_BASE_URL` is set in `app/build.gradle.kts` as a `buildConfigField`, pointing at the
   deployed backend. Update it there if you redeploy the API.
4. Run on a device or emulator (`minSdk 24`). On Android 13+ allow notifications, and on
   Android 12+ allow "Alarms & reminders" so the reminder fires at the exact time.

### Backend API
Separate repository: [mindfulplantapi](https://github.com/PSYCHOHITMAN/mindfulplantapi)
(Node.js/Express + MongoDB Atlas, deployable on Render's free tier).

| Method | Path | Purpose |
|---|---|---|
| POST | `/register`, `/login` | Email and password auth (bcrypt-hashed passwords, JWT session) |
| POST | `/sso` | Exchange a Google ID token for an API session |
| GET, POST | `/records` | Pull and push thought records (upserted by `recordId`) |
| PUT, GET, DELETE | `/profile/photo` | Profile photo blob storage |

Environment variables (see that repo's `.env.example`): `MONGODB_URI`, `JWT_SECRET`, `PORT`,
`GOOGLE_WEB_CLIENT_ID`.

## 11. Known limitations
- The API is hosted on Render's free tier, so the first request after a period of inactivity
  can be slow while the server wakes up. Sync retries with a growing delay.
- Sync is deliberately simple (a decision from the planning document): the API upserts records
  by `recordId` and there is no conflict-resolution logic beyond that.
- Exact reminder alarms can be switched off by the user on Android 12+. The reminder still
  fires, but Android may delay it slightly.
- There is no change-password feature in the app or the API.
