# GetFit — native Android fitness app

A production-quality, animation-heavy fitness app: fast onboarding → first logged set, streaks, PR
celebrations, and progress tracking. It's a faithful native port of the `GetFit.dc.html` prototype
(the source of truth for design, copy, interactions, and logic).

![status](https://img.shields.io/badge/status-MVP%20complete-CBF25C)

## Stack

Kotlin · Jetpack Compose · Material 3 · Navigation-in-Compose · Compose Animation · Room ·
DataStore · Coil (+ coil-gif) · manual DI. Single-activity, Compose-only. Min SDK 26.

## Features

- **Splash → 3-slide onboarding** → home in the fewest taps
- **Home** — time-aware greeting, Push Day hero card, 7-day streak strip, weekly quick stats, browse-by-muscle
- **Exercises** — live search + muscle filters over **1,324 seeded exercises**, PB badges, quick-add
- **Exercise detail** — animated demo stage (Coil GIF + shimmer + fallback), PR / last-trained, target progress, how-to cues
- **Workout builder** — reorder, per-exercise set steppers, intensity segmented control
- **Guided session** — work/rest state machine, progress ring, weight/reps steppers (prefilled), live PR badge, rest controls, completion summary + PR celebration
- **Progress** — weekly goal ring, volume bar chart, personal records, goals/targets, recent sessions
- **Settings** — units, sound/haptics/auto-rest, rest default, two-tap clear-all, private-by-design

All data is local (Room + DataStore). Empty states show real zeros, never fake data.

## Build & run

```bash
./gradlew installDebug            # build + install on a device/emulator
./gradlew assembleDebug           # APK only
./gradlew testDebugUnitTest       # JVM unit tests (domain + seeding)
./gradlew testDebugUnitTest --tests "com.getfit.domain.SessionEngineTest"
```

Requires JDK 17 + Android SDK (compileSdk 35). A `local.properties` with `sdk.dir` is git-ignored.

## Structure

```
app/src/main/java/com/getfit/
├─ core/{theme,anim,ui}      design tokens, springs, reusable atoms, icon map
├─ data/{db,prefs,repo}      Room, DataStore, repositories, dataset seeder
├─ domain/                   pure business logic (PR/1RM, streak, week-agg, session engine) — unit-tested
├─ di/                       AppContainer (manual DI) + ViewModelFactory
└─ ui/                       AppViewModel + per-screen composables + RootScaffold
```

## Exercise demo GIFs

The dataset (`warpirate/exercises-dataset`) ships only a `media_id` per exercise — **not the GIFs**
(ExerciseDB's CDN blocks hotlinking and ownership is disputed). Point `core/Constants.MEDIA_BASE` at a
host you control; demos then load `{media_id}.gif` via Coil with a shimmer + animated-icon fallback.
Empty by default — the app runs and degrades gracefully with no host.

## Docs

- `docs/superpowers/specs/` — design spec
- `docs/superpowers/plans/` — phased implementation plan
- `CLAUDE.md` — guidance for future contributors / agents
