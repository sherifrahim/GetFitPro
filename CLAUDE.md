# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

A **native Android port of the GetFit fitness app**. The app currently exists only as a
high-fidelity, fully-working HTML/JS prototype — `GetFit.dc.html`. That prototype is the
**source of truth** for layout, visual design, copy, interactions, animations, and — critically —
**all state/business logic**. The Android app (Kotlin + Jetpack Compose) is being built to look
and behave identically.

> Status: **built and running.** The Android app lives in `app/` (Kotlin + Compose). All 12
> planned phases are implemented and verified on an emulator: splash → onboarding → home →
> exercises → detail → builder → guided session → progress → settings, hydrated from Room +
> DataStore. Domain logic + seeding are unit-tested (`app/src/test/…`, 28 tests). Build on the
> `feat/android-mvp` branch. The build order/spec live in `docs/superpowers/`.

## UI architecture note (important)

The prototype is a single `DCLogic` component that computes everything in one `renderVals()`.
The Android port mirrors this with **one `AppViewModel`** (`ui/AppViewModel.kt`) that combines all
repos into an `AppData` snapshot + holds transient nav/overlay state (`NavState`), plus a
`SessionController` for the guided session. Per-screen composables under `ui/<screen>/` are thin —
they read `vm.data` / `vm.settings` / `vm.nav` and call VM actions. This is a deliberate choice over
per-screen ViewModels: it avoids re-deriving shared state (best-map, streak, plan) in multiple places
and keeps the port faithful to the single source of truth. `RootScaffold` renders the tab content +
all overlays (detail, session, settings, onboarding, splash, goal sheet, toast).

## Source of truth: `GetFit.dc.html`

Read it before touching a feature. It is a single-file app in a custom React-like framework
(`<x-dc>` templating + a `DCLogic` component class). The important parts:

- **Markup** (lines ~37–671): every screen and overlay, with exact colors/sizes/copy/animations
  inlined as styles. Template bindings look like `{{ x }}`, loops `<sc-for>`, conditionals `<sc-if>`.
- **Logic** (`<script data-dc-script>`, lines ~673–1058): the `Component extends DCLogic` class holds
  the entire state model, seed data, and all computed values in `renderVals()`. **This is the Kotlin
  logic spec.** Port its behavior exactly; do not "improve" the math.

## Logic that must stay byte-for-byte faithful

These are load-bearing and easy to get subtly wrong. When porting, add unit tests that reproduce the
prototype's outputs:

- **PR / personal best** (`bestFor`): weighted lifts → max weight, tiebreak on reps, estimated 1RM =
  `round(w * (1 + reps/30))`. Bodyweight lifts → max reps, no weight/1RM.
- **Bodyweight test** (`isBW`): `equipment == "Bodyweight"` **or** the rep string ends in `s`
  (time-based holds like `"45s"`). BW exercises track reps, never weight; steppers hide weight.
- **Streak** (`streakCount`): consecutive calendar days with ≥1 session, counting back from today
  (today optional — if no session today, start from yesterday).
- **Week aggregation**: week starts **Monday** (`(getDay()+6)%7`). Per-day volume drives the bar chart;
  weekly goal is **5 sessions**.
- **Session state machine** (`startSession` → `tick` → `doneSet` → `advanceFromRest` → `endSession`):
  work/rest phases, 1s ticker, PR checked on every logged set, volume accumulates (weighted only),
  a `history` record is written on end. Weight prefilled from last logged set, else `DEFAULTW`, else 20.
- **Persistence**: seed-on-first-launch (`SEED`, `DEFAULTW`, `DEFAULT_PLAN`, 2 demo targets);
  "Clear all data" wipes and resets to the same seed. Prototype uses `localStorage getfit_v1`;
  Android uses Room + DataStore.

## Data & media

- Dataset: `warpirate/exercises-dataset` → `data/exercises.json` — **1,342 exercises**
  (fields: `id, name, category, body_part, equipment, target, muscle_group, secondary_muscles,
  instructions{en,es,…}, media_id`). Ship it in `assets/` and seed Room on first launch.
- **Media is NOT in the dataset**, but demos DO animate. Each record's `media_id` is the ExerciseDB v1
  media code (squat = `qXTaZnJ`). The official host `static.exercisedb.dev` is dead (NXDOMAIN), so
  `Constants.MEDIA_BASE` points at a **community mirror served free via jsdelivr**
  (`cdn.jsdelivr.net/gh/andresmonc/LogWell@<sha>/assets/exercise-gif/` + `{media_id}.gif`), pinned to a
  commit SHA (immutable, cached permanently by jsdelivr). Verified loading on device. The detail demo
  uses `SubcomposeAsyncImage` (shimmer → gif on success → animated-icon fallback on missing/error), so
  ids absent from the mirror degrade gracefully.
  **TODO(prod):** self-host these GIFs (own bucket/CDN) rather than depend on a third-party repo — then
  just change `MEDIA_BASE`. The free `oss.exercisedb.dev/api/v1` API also has richer data (step
  instructions, target + secondary muscles) for a future re-seed.

## Target architecture (Kotlin + Compose)

Single-activity, Compose-only, **manual DI** (no Hilt): an `AppContainer` builds the Room db,
DataStore, repos, and a ViewModel factory. Clean layering:

- `core/` — `theme/` (all prototype tokens: colors, Space Grotesk + Manrope fonts, shapes),
  `anim/` (spring specs, count-ups, PR celebration, stagger), `ui/` (reusable atoms).
- `data/` — `db/` (Room entities + DAOs + seeder), `prefs/` (DataStore settings + plan),
  `repo/`. Entities: `ExerciseEntity` (seeded), `SetLogEntity` (one row per logged set),
  `SessionEntity` (+sets relation, = history), `TargetEntity`.
- `domain/` — pure Kotlin for the load-bearing math above; unit-tested against prototype values.
- `ui/` — per-screen `Screen.kt` + `ViewModel.kt` + `UiState`, plus `nav/`.

Navigation: bottom-tab `NavHost` (Home / Exercises / Build / Progress) with a spring-animated sliding
indicator; Detail / Session / Settings / Onboarding / Splash are top-level routes above the tab
scaffold; GoalSheet + Toast are state-driven overlays. All motion via Compose Animation
(`animate*AsState`, `AnimatedContent`, `updateTransition`, `Animatable` springs) — CSS keyframes in the
prototype map to these (see the design doc for the table).

## Commands

Android toolchain (apply once the Gradle module is scaffolded — min SDK 26, target latest, Compose BOM, KTS):

```bash
./gradlew assembleDebug            # build
./gradlew installDebug             # build + install on device/emulator
./gradlew testDebugUnitTest        # JVM unit tests (domain logic)
./gradlew testDebugUnitTest --tests "com.getfit.domain.PrTest"   # single test class
./gradlew connectedDebugAndroidTest   # instrumented/Compose UI tests
./gradlew lintDebug                # Android lint
```

## Planning artifacts

- `docs/superpowers/specs/` — the approved design/spec.
- `docs/superpowers/plans/` — the phased implementation plan (build order: design system → nav shell →
  data layer + domain tests → Home → Exercises → Detail → Builder → Session → Progress → Settings →
  Onboarding/Splash → animation polish).

Work through screens in that order. When a value is unclear, open `GetFit.dc.html` and copy it exactly.
