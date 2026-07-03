# GetFit — Native Android Design Spec

**Date:** 2026-07-03
**Status:** Approved (architecture) — ready for implementation planning
**Source of truth:** `GetFit.dc.html` (HTML/JS prototype), `warpirate/exercises-dataset`

---

## 1. Goal

Ship a production-quality native Android app that looks and feels **identical** to the
`GetFit.dc.html` prototype: high fidelity, animation-heavy, built for retention (fast onboarding →
first logged set, streaks, PR celebrations, motivating empty states). The prototype is authoritative
for every visual value, all copy, all interactions, and — most importantly — all state/business logic.

## 2. Tech stack (fixed)

- Kotlin + Jetpack Compose, Material 3
- Navigation Compose (bottom-tab nav + full-screen overlay routes)
- Compose Animation (`animate*AsState`, `AnimatedContent`, `updateTransition`, `Animatable` springs)
- Coil (`io.coil-kt:coil-compose` + `coil-gif`) for demo GIFs
- Room (workout logs / history / targets / seeded exercises), DataStore Preferences (settings + plan)
- **Manual DI** (AppContainer + ViewModel factory) — no Hilt
- Single Activity, Compose-only. Min SDK 26, target latest, Gradle KTS, Compose BOM.

## 3. Decisions locked

| Decision | Choice |
|---|---|
| Exercise scope | Seed **all 1,324** from `exercises.json`; keep prototype's 18 curated ids as constants for defaults/seed |
| Dependency injection | Manual (AppContainer) |
| Demo animation source | **Deferred** — build behind `Constants.MEDIA_BASE` (empty), Coil + shimmer + animated-icon fallback |
| Deliverable order | Design doc → implementation plan → build screen by screen |

## 4. Package structure

```
com.getfit/
├─ GetFitApp.kt            Application; builds AppContainer
├─ MainActivity.kt         single Activity; setContent { GetFitTheme { AppNav() } }
├─ di/AppContainer.kt      Room, DataStore, repos, ViewModelFactory
├─ core/
│  ├─ theme/               Color, Type, Shape, Theme, Tokens (all prototype values)
│  ├─ anim/                spring specs, count-up, PR celebration, stagger helpers
│  └─ ui/                  atoms: Pill, StatCard, ProgressRing, Stepper, Segmented, Toast, ShimmerBox
├─ data/
│  ├─ local/exercises.json bundled asset (1,324 records)
│  ├─ db/                  entities, DAOs, GetFitDatabase, Seeder
│  ├─ prefs/               SettingsStore, PlanStore (DataStore)
│  └─ repo/                ExerciseRepo, WorkoutRepo, ProgressRepo
├─ domain/                 Pr, Streak, WeekAgg, Est1RM, SessionEngine, targets math (pure Kotlin)
└─ ui/
   ├─ nav/                 AppNav (NavHost), BottomBar (sliding indicator), overlay routing
   └─ home/ exercises/ detail/ builder/ session/ progress/ settings/ onboarding/ splash/
      (each: Screen.kt + ViewModel.kt + UiState)
```

## 5. Data model

### Room entities
- `ExerciseEntity(id, name, category, bodyPart, equipment, target, muscleGroup,
  secondaryMuscles, instructionsEn, mediaId?)` — seeded once from `exercises.json`.
- `SetLogEntity(id, exerciseId, weight, reps, dateMs)` — one row per logged set (replaces prototype
  `logs:{id:[{w,reps,d}]}`). Best/PR derived from these.
- `SessionEntity(id, dateMs, name, durationSec, totalSets, volume, prs)` + `SessionSetEntity`
  relation — completed-session history.
- `TargetEntity(id, exId, target, start, startDMs, weeks)`.

### DataStore (Preferences)
`units (kg/lb)`, `sound`, `haptics`, `autorest`, `restDefault`, `intensity`, `onboarded`,
`plan` (serialized JSON list of `{id, sets, reps}`).

### Seed on first launch (and on clear-all)
Port `SEED` (per-exercise historical sets), `DEFAULTW` (weight prefill), `DEFAULT_PLAN` (Push Day),
and two demo targets (bench 100→110 / squat 140→160). Exact numbers from prototype lines 715–723, 754.

## 6. Load-bearing logic (port exactly; unit-test against prototype)

- **PR / best** (`bestFor`): weighted → max weight, tiebreak reps, `e1rm = round(w*(1+reps/30))`;
  bodyweight → max reps.
- **`isBW`**: `equipment=="Bodyweight" || reps.endsWith("s")`.
- **Streak** (`streakCount`): consecutive days back from today (today optional).
- **Week**: Monday start `(getDay()+6)%7`; per-day volume; weekly goal = 5.
- **Session engine**: work/rest, 1s tick, PR per set, volume (weighted only), history on end,
  weight prefill = lastWeight ?? DEFAULTW ?? 20; rep step 5 for time-based (`"…s"`) else 1;
  weight step = 5 (lb) / 2.5 (kg).
- **Targets**: progress `clamp((cur-start)/(target-start))`, `daysLeft` from `startD + weeks*7d`.
- **Formatters**: `fmtVol` (k/M), `fmtDur` (m/h), `relDay` (Today/Yesterday/weekday/Nd ago).

## 7. Screens & behavior

Each mirrors the prototype section noted.

1. **Splash → Onboarding** (proto 651–660, 625–649): logo pop + glow; 3-slide pager, dots, Skip /
   Continue / Get started. `onboarded` persisted.
2. **Home** (56–144): time-aware greeting, hero Push Day card (intensity+RPE chip, exercise/set/minute
   stats, Start), 7-day streak strip, quick stats (workouts/time/volume this week), browse-by-muscle chips.
3. **Exercises** (146–193): live search (name/muscle/equipment), horizontal muscle filter chips, rows
   (icon + name + muscle·equipment + personal-best badge + quick-add `+`), staggered entrance, empty state.
4. **Exercise Detail** (377–443): demo stage (Coil GIF via `MEDIA_BASE` + shimmer + fallback),
   muscle/equipment/level chips, Personal best + Last trained, target progress **or** "Set a target"
   CTA, numbered how-to cues, Add-to-workout + Start.
5. **Workout Builder** (195–247): reorderable list (up/down), per-exercise set stepper (1–8),
   remove, Intensity segmented (Light/Moderate/Hard → sets rest + RPE), Add exercise, Start.
6. **Guided Session** (445–539): work/rest state machine, animated progress ring, weight −/+ stepper
   (kg/lb, prefilled) beside reps stepper, live "PR" pace badge, "Log set & rest", rest countdown
   ring with −15/Skip/+15 + pause, completion summary (time, sets, volume, new PRs) + celebration.
7. **Progress** (249–355): weekly goal ring (sessions vs 5) w/ dynamic message, weekly volume bars,
   Personal Records list, Goals/Targets (+ New-goal bottom sheet), Recent sessions. Real zeros +
   friendly empty states.
8. **Settings** (541–590): units kg/lb, sound/haptics/auto-rest switches, default-rest chips,
   "Clear all data" two-tap confirm, "private by design" note.

## 8. Navigation

Bottom-tab `NavHost`: Home / Exercises / Build / Progress. Sliding lime pill indicator via
`animateDpAsState` with `spring(dampingRatio≈0.55)` to reproduce the prototype's overshoot
`cubic-bezier(.34,1.4,.5,1)`. Top-level routes above the scaffold: Splash, Onboarding, Detail,
Session, Settings. State-driven overlays: GoalSheet, Toast. Spring screen/tab transitions.

## 9. Animation mapping (CSS keyframe → Compose)

| Prototype | Compose |
|---|---|
| `splashPop`, `prPop` | `Animatable` scale spring (overshoot) |
| `fadeUp`, `scaleIn`, staggered `animation-delay` | `AnimatedVisibility` + `slideInVertically/fadeIn`, per-index delay |
| `sheetUp` | slide-up spring (bottom sheets / overlays) |
| ring `stroke-dashoffset` (goal/session/rest) | `animateFloatAsState` sweep on Canvas arc |
| stat numbers | count-up `Animatable` |
| `glowPulse`, `trailA`, `ringSpin` | `rememberInfiniteTransition` |
| tab indicator | `animateDpAsState` offset |
| stepper tick | `HapticFeedback`, gated on haptics setting |
| PR celebration | scale+glow spring + lightweight confetti |

Target 60fps: `Modifier.animateContentSize`, `AnimatedVisibility`, `Animatable` throughout.

## 10. Design system

Colors, spacing, radii lifted verbatim into `Tokens.kt` (bg `#17120D`, surface `#211A12`,
elevated `#2B2118`, text `#F6F0E6`/`#A99C8A`/`#6E6353`, accent lime `#CBF25C`→`#A9D63E`,
amber `#F3B24A`, coral `#F0774E`, mint `#86D6B4`; difficulty Beginner `#8FD694` / Intermediate
`#F3B24A` / Advanced `#F0774E`; cards 18–26dp, pills fully rounded). Fonts: Space Grotesk
(headings/numbers), Manrope (body) bundled in `res/font`. Material Symbols Rounded → Material Icons
Extended; unmapped names (`exercise`, `sprint`) resolved in a small `MuscleIcon` table.

## 11. Error handling & edge cases

- GIF load empty/fail → shimmer then animated-icon fallback; never a broken image.
- Empty data (fresh clear-all) → real zeros + empty states (no fake data). Streak/charts/records/recents
  all hydrate from Room and degrade gracefully.
- Session with all-bodyweight exercises → hide weight stepper, PR on reps.
- Clear-all is a two-tap confirm (3s window) to prevent accidental wipes.

## 12. Testing

- **Unit (JVM)**: `domain/` — PR/best, isBW, streak, week aggregation, est-1RM, target progress,
  formatters. Assert against the prototype's exact seed-derived outputs.
- **Instrumented/Compose**: nav (tab switch, open/close overlays), session happy path (log set → rest →
  next → complete), clear-all reset, search/filter.

## 13. Out of scope (YAGNI)

No accounts, no cloud, no analytics, no multiple workout plans (single "Push Day" plan like prototype),
no i18n beyond bundling English cues (dataset ships other languages; not surfaced), no wearables.
Media hosting is a separate later decision.
