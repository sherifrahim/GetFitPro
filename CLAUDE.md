# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

A **native Android port of the GetFit fitness app**, shipped as **Forge**. The app began as a
high-fidelity, fully-working HTML/JS prototype — `GetFit.dc.html`. That prototype is the
**source of truth** for layout, copy, interactions, animations, and — critically —
**all state/business logic**.

> ⚠️ **Colour is the one exception, and it is no longer the prototype's.** The palette was
> deliberately replaced (lime `#CBF25C` on warm brown `#17120D` → blue `#0B7BF7` on true black)
> at the user's request, in the style of Hevy. `core/theme/Tokens.kt` is now the sole authority
> on colour. **Do not "restore" prototype colours** — see *Theme* below before touching any.

The Android app (Kotlin + Jetpack Compose) is otherwise built to look and behave identically.

> Status: **built, running on real hardware (OPPO CPH2651 + Galaxy Watch7) and pushed to
> `sherifrahim/main`.** Three modules: the phone app in `app/`, a Wear OS companion in `wear/`, and
> `engine/` — a pure-Kotlin module holding the session state machine (`SessionEngine.kt`,
> `SessionEdits.kt`, `Formatters.kt`, `Units.kt`, package `com.getfit.domain`) that BOTH apps run.
> All 12 planned phases are implemented, plus: routines with a rotation, supersets, program
> templates, mid-workout add/replace/skip/remove, plate calculator + warm-up ramp, per-exercise
> progress chart, PR history, recovery-based "next routine" pick, period recap, Hevy-style workout
> feed + detail (muscle split, watch HR trace, vs-last deltas), profile/measurements/preferences
> parity with Hevy, AI review + AI coach + body check, backup, CSV import/export (with routine
> creation and muscle inference), live cloud sync, and a watch that mirrors the phone OR runs a
> workout on its own and syncs it back. Unit tests live in `app/src/test/…` (**195 tests**).
> `docs/hevy-parity-notes.md` records the Hevy walkthrough these features were built against.

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

- **Markup** (lines ~37–671): every screen and overlay, with exact sizes/copy/animations inlined as
  styles. Template bindings look like `{{ x }}`, loops `<sc-for>`, conditionals `<sc-if>`. Take
  layout, spacing, copy and motion from here — but **not colours** (see *Theme* below).
- **Logic** (`<script data-dc-script>`, lines ~673–1058): the `Component extends DCLogic` class holds
  the entire state model, seed data, and all computed values in `renderVals()`. **This is the Kotlin
  logic spec.** Port its behavior exactly; do not "improve" the math.

## Theme (supersedes the prototype)

`core/theme/Tokens.kt` is the only authority on colour. The prototype's warm/lime palette was
replaced on request with a black + blue scheme:

| role      | was (prototype)          | now                                   |
|-----------|--------------------------|---------------------------------------|
| surfaces  | `#17120D` / `#211A12`    | `#000000` / `#121212` (neutral)       |
| accent    | `#CBF25C` lime           | `#0B7BF7` blue, **white** on it       |
| text      | `#F6F0E6` warm off-white | `#FFFFFF` + a neutral grey ramp       |
| hairlines | `rgba(246,240,230,a)`    | `rgba(255,255,255,a)`                 |

Notes for anyone changing colour:

- Tokens are named `Accent` / `AccentFill*`, **not** `Lime` — don't reintroduce hue-specific names.
- Difficulty pills and the trend card are **monochrome**; difficulty encodes rank as *brightness*
  rather than hue. Two semantic colours deliberately survive because they carry information, not
  brand: **amber** = the rest phase, **coral** = destructive actions.
- White on `#0B7BF7` is ~4.1:1 — fine for the large bold button labels it's used for, **not** for
  small text. Keep body copy off the accent fill.
- Colour also lives outside Kotlin: `res/values/colors.xml` (`windowBackground` paints before
  Compose's first frame, so a stale value shows as a coloured flash at cold start), both launcher
  icons, the onboarding slides in `data/db/Curated.kt`, and `wear/…/theme/WearTheme.kt`. Change
  them together.

## Wear OS companion (`wear/`)

Separate module, bundled via `wearApp` from `app/build.gradle.kts`. Design doc:
`docs/wear-companion-design.md`. Phone side is `data/wear/PhoneWearSync.kt` + `WearProtocol.kt`;
watch side is `wear/…/wear/data/`. `MessageClient`, not `DataClient` (see the doc for why). Heart
rate via Health Services `ExerciseClient`, batched every 8s.

- The protocol (`WearProtocol.kt`) lives ONCE in `engine/`, package `com.getfit.data.wear`, used by
  both apps. It used to be duplicated on each side with a drift test; the shared module made that
  unnecessary. Add fields with defaults only, so a phone and watch on different builds keep decoding
  each other.
- **Do not hand-manage Guava in `wear/build.gradle.kts`.** Forcing `listenablefuture` across all
  configurations, or excluding full Guava from `health-services-client`, crashes the watch app on
  launch (`NoClassDefFoundError: com.google.common.base.Preconditions`). The force must stay scoped
  to `*CompileClasspath`. The comments in that file explain the whole trap — read them first.
- **Status: verified on a real Galaxy Watch7 paired to the OPPO** — phone→watch mirror, watch→phone
  Done/Skip, heart rate in both directions. Three things that were load-bearing to get there: the
  wear `applicationId` MUST equal the phone's (`com.getfit`) or the Data Layer silently drops every
  message; snapshots are received by a manifest-registered `WearableListenerService`
  (`ForgeListenerService`) so they arrive with the watch app closed; and Android 16 watches need
  `android.permission.health.READ_HEART_RATE` on top of `BODY_SENSORS` for any HR sample.
- **Standalone mode.** The watch runs the shared `:engine` itself when the phone doesn't answer a
  Start within 3 s (`LocalSession`, routine cache + pending uploads in `WatchStore`, SharedPrefs
  JSON). Finished sessions go up as `WearSessionUpload` and are retried on every reconnect until the
  phone's `WearUploadService` (also a manifest listener — works with the phone app closed) stores
  them and acks by id. Verified end to end on the Wear AVD with a seeded cache; the phone→watch leg
  of that loop is verified on real hardware, the upload leg only by code review.
- The two emulators (`forge_phone`, `forge_watch`) are NOT paired (that needs the Play companion +
  Google sign-in), so on AVDs test the watch standalone: seed `shared_prefs/forge_watch.xml` via
  `run-as com.getfit`.
- Orientation: the watch activity is `screenOrientation="locked"` (not `nosensor` — the Galaxy
  Watch runs at ROTATION_270 and `nosensor` would render against the natural orientation). If the
  whole watch rotates, that is the system `accelerometer_rotation` setting, not the app.

## Post-MVP features

- **AI coach** (`data/ai/`, `ui/ai/`) — two features, one client:
  - *AI review* sends `buildWorkoutSummary`'s brief (sessions, bests, targets, trend verdicts, and
    **training patterns** from `domain/Patterns.kt` — skipped weeks, gaps, day-of-week habits, rest
    cadence, pace, volume direction, muscle balance) and asks for schedule → balance → progression
    advice. Patterns and trends are **pre-computed facts**; the prompt tells the model to explain them,
    not re-derive them.
  - *Body check* (Progress → Body check) takes up to five photos via the system Photo Picker (no
    storage permission), a goal, and an explicit **per-open consent** tick, then returns a physique
    read plus concrete training changes. Photos are downscaled, EXIF-stripped, sent once, **never
    persisted** — see the privacy notes at the top of `BodyAnalysis.kt` before touching it.
  - `AnthropicClient` is the only HTTP path (plain `HttpURLConnection`, no Retrofit/OkHttp — this
    project avoids new Gradle dependencies where a built-in approach works). Rules baked into it,
    with reasons in the class doc: default model `claude-opus-5`; **don't send `thinking`** (omitting
    is adaptive on Opus 5 and safe on older models); `max_tokens` must be thousands, not 1024,
    because thinking tokens count against it; check `stop_reason == "refusal"` before reading
    content; `fallbacks: "default"` only on models that accept it. `AnthropicRequestTest` pins the
    wire shape. The user's key lives in `SecureKeyStore`; existing installs keep their **stored**
    model — a new default doesn't overwrite it.
- **Backup** (`data/backup/`) — full-fidelity on-device snapshot (JSON) in `filesDir/backups/`, plus
  save/restore through the storage picker. Restore REPLACES user data and is confirm-gated. Distinct
  from the CSV import/export below, which moves sets between apps and is lossy.
- **Import/export** (`data/importexport/`) — Hevy/Strong/FitNotes/Forge CSV. Matches column **names**,
  not positions, because real sample exports couldn't be verified up front.
- **Cloud sync** (`data/sync/` + `server/forge-sync/`) — **live.** Snapshot model: the app uploads its
  backup document ~20 s after any change (`SyncRepo.noteChange()` is called from every write path)
  and can pull the newest one back (confirm-gated, replaces local data). One user, whole snapshots,
  last-writer-wins — deliberately NOT op-based merge. The server is a ~150-line FastAPI app at
  **https://getfit.mooo.com** on the user's Oracle VM (`129.151.128.111`, Ubuntu, 1 GB RAM, shared
  with an unrelated `threatfeed` service and Postgres 14 — **never touch threatfeed's nginx block or
  service**). It reuses the existing Postgres (db `forge`), runs under systemd `forge-sync` with
  `MemoryMax=160M`, and is deployed by the idempotent `server/forge-sync/deploy.sh`. Bearer token
  lives in `/home/ubuntu/forge-sync/.env` on the server and in `SecureKeyStore` (slot `SYNC`) on
  the phone. Restore pulls the **latest** version only; the server keeps the last 20.
- **Trend insight** (`domain/Trend.kt`) — pure Kotlin, no network; surfaced on Detail and fed to the
  AI review prompt as a pre-computed fact so the model explains it rather than re-deriving it.
- **Routines** (`data/prefs/Prefs.kt` `RoutinesStore`, `WorkoutRepo`) — the prototype's single plan
  became a list with a rotation (`RoutinesData.currentId`, advanced by `advanceAfter` when a session
  for it is saved; a legacy edited `plan` key migrates into Push Day). `Curated.PROGRAMS` are
  template routine sets. Imports create a routine per workout name; a past workout can be saved as
  one. The watch gets the list (with items, prefill weights and bests) on every snapshot.
- **Coach** (`data/ai/Coach.kt`, `ui/ai/CoachScreen.kt`) — three asks over the same brief as the
  review: last-workout critique (vs-last deltas are pre-computed), what's next (fed the
  `domain/Recovery.kt` readiness estimate + the rotation's pick) with PR attempts, and a goal
  review. NEXT/GOALS answers end in a `TARGETS:` block the app parses into "Set as target" chips.
- **Room is at v2** (`GetFitDatabase.MIGRATION_1_2`, additive: sessions gained routineId/avgBpm/
  maxBpm/calories, plus `heart_rate_samples` and `measurements`). Never use destructive migration —
  the user's phone carries real imported history. `AppContainer.seedOnFirstLaunch` also runs two
  one-shot upgrades gated by DataStore flags (demo-row purge, imported-muscle re-inference).
- **Placeholders are gone.** No seeded PRs/targets, no "Alex Rivera": Home/Settings show the
  profile name or a person icon, and empty states everywhere. Don't bring demo data back.

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
- **Session state machine** (`startSession` → `tick` → `doneSet` → `advanceFromRest` → `endSession`,
  now in `engine/`): work/rest phases, 1s ticker, PR checked on every logged set, volume accumulates
  (weighted only), a `history` record is written on end. Weight prefilled from last logged set, else
  `DEFAULTW`, else 20. Extensions on top of the prototype, all pure and tested: supersets
  (`nextPosition` works linked items in rounds with no rest between partners), mid-workout edits
  (`SessionEdits.kt`: add/replace/remove/skip, totals include orphaned logged sets), a kg↔lb switch
  mid-session (`convertSessionUnits` — every weight in the state is in display units, so all of them
  convert together), and a downsampled watch heart-rate trace on the state (`addHeartRate`).
- **Persistence**: the prototype seeded demo history (`SEED`), `DEFAULTW`, `DEFAULT_PLAN` and 2 demo
  targets on first launch. **The Android app deliberately does not seed demo history or targets any
  more** (dropped once it ran on real devices): first run is the exercise library + the default
  routines + an empty log. `Curated.SEED` survives only as the reference data for `PrTest`. "Clear all
  data" wipes user rows and resets routines/preferences; the library stays. Prototype uses
  `localStorage getfit_v1`; Android uses Room + DataStore.

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

Three modules: `:app` (phone, min SDK 26), `:wear` (Wear OS, min SDK 30) and `:engine` (pure Kotlin
JVM, no Android — keep it that way). Always build both apps — `:app` compiling proves nothing about
`:wear`. `:engine` has no tests of its own; its tests live in `app/src/test` and run with `:app`'s.

```bash
./gradlew :app:assembleDebug :wear:assembleDebug   # build both (do this first)
./gradlew :app:installDebug                        # install phone app
./gradlew :app:testDebugUnitTest                   # JVM unit tests (195, incl. the engine's)
./gradlew :app:testDebugUnitTest --tests "com.getfit.domain.PrTest"   # single test class
./gradlew :app:connectedDebugAndroidTest           # instrumented/Compose UI tests
./gradlew :app:lintDebug                           # Android lint
```

Environment notes (already solved — don't rediscover):

- **JDKs — two different things, easy to conflate.** (1) The *wrapper script* needs `JAVA_HOME` or
  `java` on `PATH` just to launch; that's a system setting, set system-wide on the dev machine.
  (2) The *daemon* runs on the JDK chosen by `gradle/gradle-daemon-jvm.properties` (Java 21, found
  in `~/.jdks/jbr-21.0.11` locally; Temurin 21 on CI). **That file is load-bearing** — Gradle 8.13's
  Kotlin DSL compiler can't run on newer JDKs, and Studio's bundled JBR is Java 25. **Never set
  `org.gradle.java.home`** in any `gradle.properties`: pointing it at the JBR would break every
  `.kts` compile if honoured, and a Windows path there breaks Linux CI.
- **`local.properties`** is gitignored, so a fresh **git worktree** has no SDK path and every build
  fails with "SDK location not found". Copy it in from the main checkout.
- **Don't move the repo back under OneDrive** — it locked files mid-build and caused repeated
  Gradle failures. `D:\GetFitPro` is deliberate.
- AGP was bumped to 8.13.2 by Android Studio itself. That's intentional; leave it.
- Piping gradle through `| tail` **masks its exit code** — check `${PIPESTATUS[0]}` or redirect to a
  file, or a failed build will look like it passed.

## Planning artifacts

- `docs/superpowers/specs/` — the approved design/spec.
- `docs/superpowers/plans/` — the phased implementation plan (build order: design system → nav shell →
  data layer + domain tests → Home → Exercises → Detail → Builder → Session → Progress → Settings →
  Onboarding/Splash → animation polish).

Work through screens in that order. When a value is unclear, open `GetFit.dc.html` and copy it
exactly — **except colours**, which come from `core/theme/Tokens.kt`, not the prototype.
