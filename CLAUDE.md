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

> Status: **built and running.** The phone app lives in `app/`, a Wear OS companion in `wear/`.
> All 12 planned phases are implemented and verified on an emulator: splash → onboarding → home →
> exercises → detail → builder → guided session → progress → settings, hydrated from Room +
> DataStore. Since then: AI review, CSV import/export, cloud-sync groundwork (inert), a Wear OS
> companion, rest-timer feedback, and the trend insight. Unit tests live in `app/src/test/…`
> (**136 tests**). The build order/spec live in `docs/superpowers/`.

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

- The protocol data classes are **deliberately duplicated** on both sides rather than shared via a
  module. Keep that. `WearProtocolDriftTest` fails the build if the two copies stop agreeing, and
  `app/build.gradle.kts` declares both files as test-task inputs so editing only the watch copy
  still re-runs it.
- **Do not hand-manage Guava in `wear/build.gradle.kts`.** Forcing `listenablefuture` across all
  configurations, or excluding full Guava from `health-services-client`, crashes the watch app on
  launch (`NoClassDefFoundError: com.google.common.base.Preconditions`). The force must stay scoped
  to `*CompileClasspath`. The comments in that file explain the whole trap — read them first.
- Status: builds, installs, and runs to its Idle screen on a Wear OS 5.1 emulator. **Phone↔watch
  mirroring and heart rate are still unverified** — that needs a paired device, and emulator
  pairing needs the Wear OS companion app (Play Store image + Google sign-in).

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

Two modules: `:app` (phone, min SDK 26) and `:wear` (Wear OS, min SDK 30). Always build both —
`:app` compiling proves nothing about `:wear`.

```bash
./gradlew :app:assembleDebug :wear:assembleDebug   # build both (do this first)
./gradlew :app:installDebug                        # install phone app
./gradlew :app:testDebugUnitTest                   # JVM unit tests (136)
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
