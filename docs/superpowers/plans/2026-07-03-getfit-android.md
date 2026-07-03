# GetFit Android Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a native Android (Kotlin + Jetpack Compose) app that is visually and behaviorally identical to the `GetFit.dc.html` prototype.

**Architecture:** Single-activity, Compose-only, clean layering (data → domain → ui) with manual DI via an `AppContainer`. Room + DataStore for persistence, seeded from a bundled 1,324-exercise dataset. All business logic is ported verbatim from the prototype's `DCLogic` component and covered by JVM unit tests.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, Navigation Compose, Compose Animation, Coil + coil-gif, Room, DataStore Preferences, JUnit4 + Truth/Robolectric, Compose UI test.

---

## How to read this plan

- **Phases are ordered and dependent.** Complete a phase (all tasks + its Definition of Done) before the next.
- Every phase ends by **committing** and pushing to `origin/main`.
- The prototype `GetFit.dc.html` is the UI spec. Where a phase says "port proto L###–L###", open those lines and reproduce values/copy/animation exactly. **Do not invent values.**
- **Phase 3 (domain) is fully TDD-coded here** — it is the load-bearing math. Screen phases list files + tasks + exact prototype anchors; each screen phase should be expanded into its own detailed sub-plan (`docs/superpowers/plans/2026-07-03-getfit-<screen>.md`) at build time.
- Data used by tests comes from the prototype seed (proto L715–723 `SEED`/`DEFAULTW`, L754 demo targets).

---

## File structure (locked)

```
app/
├─ build.gradle.kts, src/main/AndroidManifest.xml
├─ src/main/assets/exercises.json
├─ src/main/res/font/{space_grotesk_*, manrope_*}.ttf
└─ src/main/java/com/getfit/
   ├─ GetFitApp.kt, MainActivity.kt
   ├─ di/AppContainer.kt, di/ViewModelFactory.kt
   ├─ core/theme/{Color,Type,Shape,Theme,Tokens}.kt
   ├─ core/anim/{Springs,CountUp,PrCelebration,Stagger}.kt
   ├─ core/ui/{Pill,StatCard,ProgressRing,Stepper,Segmented,Toast,ShimmerBox,MuscleIcon}.kt
   ├─ core/Constants.kt
   ├─ data/db/{entities,Daos,GetFitDatabase,Seeder}.kt
   ├─ data/prefs/{SettingsStore,PlanStore}.kt
   ├─ data/repo/{ExerciseRepo,WorkoutRepo,ProgressRepo}.kt
   ├─ domain/{Models,Pr,Streak,WeekAgg,Formatters,TargetMath,SessionEngine}.kt
   └─ ui/nav/{AppNav,BottomBar}.kt
      ui/{home,exercises,detail,builder,session,progress,settings,onboarding,splash}/…
app/src/test/java/com/getfit/domain/…      (JVM unit tests)
app/src/androidTest/java/com/getfit/…      (Compose UI tests)
```

---

## Phase 0 — Repo & tooling sanity

**Files:** `.gitignore` (exists), root `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`.

- [ ] **Step 1:** Confirm JDK 17 + Android SDK available: `java -version` (expect 17.x), `sdkmanager --list | grep "platforms;android-3"`.
- [ ] **Step 2:** Create `settings.gradle.kts` with `pluginManagement` + `dependencyResolutionManagement` (google(), mavenCentral()) and `include(":app")`.
- [ ] **Step 3:** Create `gradle/libs.versions.toml` version catalog: AGP (latest stable), Kotlin 2.x, Compose BOM (latest), Coil 2.x (`coil-compose`, `coil-gif`), Room 2.x (+ KSP), DataStore-preferences, androidx.navigation-compose, material-icons-extended, junit4, truth, robolectric, androidx.compose.ui:ui-test-junit4.
- [ ] **Step 4:** Root `build.gradle.kts`: declare AGP + Kotlin + KSP plugins `apply false`.
- [ ] **Step 5:** `gradle.properties`: `android.useAndroidX=true`, `org.gradle.jvmargs=-Xmx4g`, `kotlin.code.style=official`.
- [ ] **Step 6:** Commit: `chore: gradle scaffold + version catalog`.

**Definition of Done:** `./gradlew help` runs clean.

---

## Phase 1 — App module, theme, design tokens

**Files:** `app/build.gradle.kts`, `AndroidManifest.xml`, `GetFitApp.kt`, `MainActivity.kt`, `core/Constants.kt`, `core/theme/*`, fonts in `res/font/`.

- [ ] **Step 1:** `app/build.gradle.kts`: `com.android.application` + kotlin-android + KSP; `minSdk 26`, `targetSdk` latest, `compileSdk` latest; `buildFeatures { compose = true }`; wire catalog deps; enable `testOptions.unitTests.isIncludeAndroidResources = true` (Robolectric).
- [ ] **Step 2:** Download & bundle **Space Grotesk** (400/500/600/700) and **Manrope** (400/500/600/700/800) TTFs into `res/font/`; build `Type.kt` `FontFamily`s. Space Grotesk = display/numbers, Manrope = body.
- [ ] **Step 3:** `core/theme/Tokens.kt` — every prototype color as `Color` constants (bg `#17120D`, page `#0C0906`, surface `#211A12`, elevated `#2B2118`, hairline alphas, text `#F6F0E6`/`#A99C8A`/`#6E6353`, lime `#CBF25C`/`#A9D63E`, amber `#F3B24A`, coral `#F0774E`, mint `#86D6B4`, difficulty trio) + spacing/radii scale.
- [ ] **Step 4:** `core/theme/{Color,Shape,Theme}.kt` — M3 dark `ColorScheme` mapped to tokens; `Shapes` (18–26dp cards, fully-rounded pills); `GetFitTheme { }` wrapper. Force dark.
- [ ] **Step 5:** `core/Constants.kt` — `const val MEDIA_BASE = ""` + `fun gifUrl(mediaId: String?)` returning `""` when base or id blank.
- [ ] **Step 6:** `MainActivity.kt` (edge-to-edge, `setContent { GetFitTheme { AppNavPlaceholder() } }`) + `GetFitApp : Application`; register app in manifest; status-bar/nav-bar transparent.
- [ ] **Step 7:** Build + install: `./gradlew installDebug`; verify dark background renders.
- [ ] **Step 8:** Commit: `feat: app module, dark theme, design tokens, fonts`.

**Definition of Done:** app launches to a dark `#17120D` screen using bundled fonts.

---

## Phase 2 — Reusable UI atoms

**Files:** `core/ui/{Pill,StatCard,ProgressRing,Stepper,Segmented,Toast,ShimmerBox,MuscleIcon}.kt`, `core/anim/{Springs,CountUp,Stagger,PrCelebration}.kt`.

- [ ] **Step 1:** `core/anim/Springs.kt` — named spec: `OvershootSpring = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)` (reproduces `cubic-bezier(.34,1.4,.5,1)`), plus standard `EnterSpring`.
- [ ] **Step 2:** `ProgressRing` — Canvas arc, `sweep` driven by `animateFloatAsState`; params: progress, color, trackColor, strokeWidth, size. Used by goal/session/rest rings.
- [ ] **Step 3:** `Stepper` (−/+ circular buttons + value), `Segmented` (Light/Moderate/Hard style), `Pill`, `StatCard` (icon + value + label), `ShimmerBox` (infinite shimmer for GIF slot), `Toast` (rounded snackbar per proto L663–665).
- [ ] **Step 4:** `core/anim/CountUp.kt` — `@Composable fun countUp(target: Int): Int` via `Animatable`; `Stagger.kt` — helper for per-index entrance delay.
- [ ] **Step 5:** `MuscleIcon.kt` — map muscle → `ImageVector` (Material Icons Extended: `LocalFireDepartment`, `Monitoring`, `FitnessCenter`, `DirectionsRun`, `SelfImprovement`…); resolve prototype names `exercise`/`sprint` to nearest icon (proto L682 `MICON`).
- [ ] **Step 6:** Compose preview each atom; commit: `feat: reusable UI atoms + animation helpers`.

**Definition of Done:** each atom renders in a `@Preview` matching prototype styling.

---

## Phase 3 — Domain logic (TDD, fully specified)

The load-bearing math. Port from proto L764–900. Pure Kotlin, no Android deps → fast JVM tests.

**Files:**
- Create: `domain/Models.kt`, `domain/Pr.kt`, `domain/Streak.kt`, `domain/WeekAgg.kt`, `domain/Formatters.kt`, `domain/TargetMath.kt`
- Test: `app/src/test/java/com/getfit/domain/{PrTest,StreakTest,WeekAggTest,FormattersTest,TargetMathTest}.kt`

### Task 3.1 — Models

- [ ] **Step 1:** Create `domain/Models.kt`:

```kotlin
package com.getfit.domain

data class LoggedSet(val exerciseId: String, val weight: Double, val reps: Int, val dateMs: Long)
data class Best(val bodyweight: Boolean, val weight: Double, val reps: Int, val e1rm: Int, val dateMs: Long)
data class SessionRecord(val id: String, val dateMs: Long, val name: String, val durationSec: Int,
                         val totalSets: Int, val volume: Int, val prs: Int)
```

- [ ] **Step 2:** Commit: `feat(domain): core models`.

### Task 3.2 — isBW + best/PR

- [ ] **Step 1: Write failing test** `PrTest.kt`:

```kotlin
package com.getfit.domain
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrTest {
  private val benchSets = listOf(           // proto SEED.bench: [[85,8],[92.5,6],[100,5]]
    LoggedSet("bench", 85.0, 8, 0), LoggedSet("bench", 92.5, 6, 0), LoggedSet("bench", 100.0, 5, 0))

  @Test fun isBW_true_for_bodyweight_equipment() {
    assertThat(isBW(equipment = "Bodyweight", reps = "12")).isTrue()
  }
  @Test fun isBW_true_for_time_based_reps() {
    assertThat(isBW(equipment = "Barbell", reps = "45s")).isTrue()
  }
  @Test fun isBW_false_for_weighted() {
    assertThat(isBW(equipment = "Barbell", reps = "8")).isFalse()
  }
  @Test fun best_weighted_picks_max_weight_and_e1rm() {
    val b = bestFor(benchSets, bodyweight = false)!!
    assertThat(b.weight).isEqualTo(100.0)
    assertThat(b.reps).isEqualTo(5)
    assertThat(b.e1rm).isEqualTo(117)         // round(100*(1+5/30)) = 117
  }
  @Test fun best_bodyweight_picks_max_reps() {
    val sets = listOf(LoggedSet("dip",0.0,12,1), LoggedSet("dip",0.0,15,2))
    val b = bestFor(sets, bodyweight = true)!!
    assertThat(b.reps).isEqualTo(15)
    assertThat(b.weight).isEqualTo(0.0)
    assertThat(b.e1rm).isEqualTo(0)
  }
  @Test fun best_empty_is_null() { assertThat(bestFor(emptyList(), false)).isNull() }
}
```

- [ ] **Step 2: Run, expect FAIL** (unresolved `isBW`/`bestFor`): `./gradlew testDebugUnitTest --tests "com.getfit.domain.PrTest"`.
- [ ] **Step 3: Implement** `domain/Pr.kt` (port proto L765, L769–774):

```kotlin
package com.getfit.domain
import kotlin.math.roundToInt

fun isBW(equipment: String, reps: String): Boolean =
  equipment == "Bodyweight" || reps.endsWith("s")

fun bestFor(sets: List<LoggedSet>, bodyweight: Boolean): Best? {
  if (sets.isEmpty()) return null
  return if (bodyweight) {
    var b = sets[0]
    for (x in sets) if (x.reps > b.reps || (x.reps == b.reps && x.dateMs > b.dateMs)) b = x
    Best(true, 0.0, b.reps, 0, b.dateMs)
  } else {
    var b = sets[0]
    for (x in sets) if (x.weight > b.weight || (x.weight == b.weight && x.reps > b.reps)) b = x
    Best(false, b.weight, b.reps, (b.weight * (1 + b.reps / 30.0)).roundToInt(), b.dateMs)
  }
}
```

- [ ] **Step 4: Run, expect PASS.**
- [ ] **Step 5: Commit** `feat(domain): isBW + bestFor (PR/1RM)`.

### Task 3.3 — Streak

- [ ] **Step 1: Failing test** `StreakTest.kt`:

```kotlin
package com.getfit.domain
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StreakTest {
  private val DAY = 86_400_000L
  @Test fun empty_is_zero() { assertThat(streakCount(emptyList(), now = 10*DAY)).isEqualTo(0) }
  @Test fun today_and_yesterday_is_two() {
    val now = 10*DAY
    assertThat(streakCount(listOf(now, now-DAY), now)).isEqualTo(2)
  }
  @Test fun missing_today_but_yesterday_counts() {
    val now = 10*DAY
    assertThat(streakCount(listOf(now-DAY, now-2*DAY), now)).isEqualTo(2)
  }
  @Test fun gap_breaks_streak() {
    val now = 10*DAY
    assertThat(streakCount(listOf(now, now-2*DAY), now)).isEqualTo(1)
  }
}
```

- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement** `domain/Streak.kt` (port proto L777, L780):

```kotlin
package com.getfit.domain

private const val DAY = 86_400_000L
fun floorDay(ts: Long): Long = ts - (ts % DAY)   // UTC-day floor; sufficient for streak counting

fun streakCount(sessionDates: List<Long>, now: Long): Int {
  if (sessionDates.isEmpty()) return 0
  val days = sessionDates.map { floorDay(it) }.toHashSet()
  var s = 0
  var d = floorDay(now)
  if (!days.contains(d)) d -= DAY
  while (days.contains(d)) { s++; d -= DAY }
  return s
}
```

- [ ] **Step 4: Run, expect PASS.** *(Note: prototype uses local-midnight via `setHours(0,0,0,0)`; when wiring real timestamps use a `java.time` local-day floor. The UTC floor here keeps the unit test deterministic — replace with `LocalDate`-based floor in `WeekAgg` integration.)*
- [ ] **Step 5: Commit** `feat(domain): streak count`.

### Task 3.4 — Week aggregation

- [ ] **Step 1: Failing test** `WeekAggTest.kt`:

```kotlin
package com.getfit.domain
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WeekAggTest {
  private val DAY = 86_400_000L
  @Test fun buckets_volume_by_day_from_monday() {
    val weekStart = 0L                       // Monday 00:00
    val sessions = listOf(
      SessionRecord("a", weekStart + 0*DAY + 3600_000, "Push", 1800, 4, 1000, 0),  // Mon
      SessionRecord("b", weekStart + 2*DAY + 3600_000, "Push", 1800, 4, 500, 0))   // Wed
    val agg = weekAgg(sessions, weekStart)
    assertThat(agg.dayVolume[0]).isEqualTo(1000)
    assertThat(agg.dayVolume[2]).isEqualTo(500)
    assertThat(agg.workouts).isEqualTo(2)
    assertThat(agg.totalVolume).isEqualTo(1500)
  }
}
```

- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement** `domain/WeekAgg.kt` (port proto L921–925):

```kotlin
package com.getfit.domain

data class WeekResult(val dayVolume: IntArray, val dayHit: BooleanArray,
                      val workouts: Int, val durationSec: Int, val totalVolume: Int)

fun weekAgg(sessions: List<SessionRecord>, weekStartMs: Long): WeekResult {
  val vol = IntArray(7); val hit = BooleanArray(7)
  var workouts = 0; var dur = 0; var total = 0
  for (h in sessions) if (h.dateMs >= weekStartMs) {
    val i = ((h.dateMs - weekStartMs) / 86_400_000L).toInt()
    if (i in 0..6) { vol[i] += h.volume; hit[i] = true }
    workouts++; dur += h.durationSec; total += h.volume
  }
  return WeekResult(vol, hit, workouts, dur, total)
}
```

- [ ] **Step 4: Run, expect PASS.** **Step 5: Commit** `feat(domain): weekly aggregation`.

### Task 3.5 — Formatters + target math

- [ ] **Step 1: Failing test** `FormattersTest.kt` + `TargetMathTest.kt`:

```kotlin
// FormattersTest.kt
package com.getfit.domain
import com.google.common.truth.Truth.assertThat
import org.junit.Test
class FormattersTest {
  @Test fun vol() { assertThat(fmtVol(0)).isEqualTo("0"); assertThat(fmtVol(12000)).isEqualTo("12k")
    assertThat(fmtVol(1_500_000)).isEqualTo("1.5M") }
  @Test fun dur() { assertThat(fmtDur(0)).isEqualTo("0m"); assertThat(fmtDur(1800)).isEqualTo("30m")
    assertThat(fmtDur(7200)).isEqualTo("2h") }
  @Test fun clock() { assertThat(fmtClock(65)).isEqualTo("1:05") }
}
// TargetMathTest.kt
class TargetMathTest {
  @Test fun progress_clamps_0_1() {
    assertThat(targetPct(cur = 105.0, start = 100.0, target = 110.0)).isWithin(0.001).of(0.5)
    assertThat(targetPct(cur = 130.0, start = 100.0, target = 110.0)).isEqualTo(1.0)
    assertThat(targetPct(cur = 90.0, start = 100.0, target = 110.0)).isEqualTo(0.0)
  }
}
```

- [ ] **Step 2: Run, expect FAIL.**
- [ ] **Step 3: Implement** `domain/Formatters.kt` + `domain/TargetMath.kt` (port proto L766, L781–782, L951):

```kotlin
// Formatters.kt
package com.getfit.domain
fun fmtVol(v: Int): String = when {
  v <= 0 -> "0"; v >= 1_000_000 -> "%.1f".format(v/1_000_000.0).removeSuffix(".0") + "M"
  v >= 10_000 -> (Math.round(v/1000.0)).toString() + "k"; else -> "%,d".format(v)
}
fun fmtDur(sec: Int): String { val m = Math.round(sec/60.0).toInt()
  return when { m <= 0 -> "0m"; m < 60 -> "${m}m"; else -> { val h = m/60.0
    (if (h % 1.0 == 0.0) h.toInt().toString() else "%.1f".format(h)) + "h" } } }
fun fmtClock(sec: Int): String = "${sec/60}:${(sec%60).toString().padStart(2,'0')}"
// TargetMath.kt
fun targetPct(cur: Double, start: Double, target: Double): Double {
  val denom = (target - start).let { if (it == 0.0) 1.0 else it }
  return ((cur - start) / denom).coerceIn(0.0, 1.0)
}
```

- [ ] **Step 4: Run all domain tests, expect PASS:** `./gradlew testDebugUnitTest`.
- [ ] **Step 5: Commit** `feat(domain): formatters + target math`.

**Definition of Done:** `./gradlew testDebugUnitTest` green; every function in proto L764–900 covered.

---

## Phase 4 — Data layer (Room + DataStore + seed)

**Files:** `data/db/*`, `data/prefs/*`, `data/repo/*`, `assets/exercises.json`.

- [ ] **Step 1:** Download `data/exercises.json` from `warpirate/exercises-dataset` into `app/src/main/assets/exercises.json`; verify 1,324 records.
- [ ] **Step 2:** Entities: `ExerciseEntity`, `SetLogEntity`, `SessionEntity` + `SessionSetEntity` (FK), `TargetEntity`. DAOs with `Flow` queries (all logs for exercise, all sessions desc, targets, exercise search/filter). `GetFitDatabase` (v1).
- [ ] **Step 3:** `Seeder` — on first launch parse `exercises.json` (kotlinx-serialization or Moshi) → insert `ExerciseEntity` rows; then insert prototype seed (`SEED` sets L719–723, two demo targets L754) if DB empty. Idempotent flag in DataStore.
- [ ] **Step 4:** `data/prefs/SettingsStore` + `PlanStore` (DataStore Preferences): units, sound, haptics, autorest, restDefault, intensity, onboarded, seeded-flag, plan (JSON list `{id,sets,reps}`, default = proto `DEFAULT_PLAN` L717).
- [ ] **Step 5:** Repos expose domain-shaped `Flow`s: `ExerciseRepo` (list/search/filter/byId + `bestFor` per exercise), `WorkoutRepo` (plan CRUD, log session, history), `ProgressRepo` (streak/week/records/targets by composing domain fns).
- [ ] **Step 6:** Robolectric test: seed into in-memory Room, assert count 1324 and that `bestFor(bench)` over seeded logs == 100kg/117 e1rm. `./gradlew testDebugUnitTest --tests "com.getfit.data.*"`.
- [ ] **Step 7:** `di/AppContainer.kt` builds db + stores + repos; `ViewModelFactory`. Wire into `GetFitApp`.
- [ ] **Step 8:** Commit: `feat(data): room + datastore + seed + repos`.

**Definition of Done:** fresh install seeds 1,324 exercises + demo history; repos emit correct derived data.

---

## Phase 5 — Navigation shell

**Files:** `ui/nav/AppNav.kt`, `ui/nav/BottomBar.kt`, 4 stub tab screens, overlay route wiring.

- [ ] **Step 1:** `AppNav` `NavHost`: tab routes `home/exercises/build/progress`; top-level routes `splash/onboarding/detail/{id}/session/settings`. Scaffold with `BottomBar` shown only on tab routes.
- [ ] **Step 2:** `BottomBar` (port proto L359–373): 4 items (Home/Exercises/Build/Progress icons + labels), lime pill indicator position via `animateDpAsState(OvershootSpring)`; FILL icon on active; blurred/translucent surface.
- [ ] **Step 3:** Spring `enter/exit` transitions on tab + overlay navigation.
- [ ] **Step 4:** Stub each tab screen with its title; verify tab switch animates the indicator.
- [ ] **Step 5:** Commit: `feat(nav): tab shell + sliding indicator + overlay routes`.

**Definition of Done:** all four tabs reachable; indicator slides with overshoot; overlays open/close.

---

## Phase 6 — Home screen

**Files:** `ui/home/{HomeScreen,HomeViewModel,HomeUiState}.kt`. **Port:** proto L56–144, view-model values L1029–1033.
Tasks: greeting (time-aware), hero Push Day card (intensity+RPE chip, exCount/sets/est from `planStats` L895–900, Start → session), 7-day streak strip (`weekDots` L926), quick stats (workouts/time/volume, count-up), browse-by-muscle chips (`MUSCLE_COUNT` L713). Hydrate from `ProgressRepo`+`WorkoutRepo`. Entrance animations `scaleIn`/`fadeUp`. Commit `feat(ui): home`.

**DoD:** Home matches prototype with real seeded data; Start launches session.

---

## Phase 7 — Exercises screen

**Files:** `ui/exercises/{ExercisesScreen,ExercisesViewModel,ExercisesUiState}.kt`. **Port:** proto L146–193, values L910–914, L1033–1035.
Tasks: search field (debounced, filters name/muscle/equipment L911), muscle filter chips (`MUSCLES` L712), list rows (icon+name+`muscle·equipment`+PB badge+quick-add `+`), staggered entrance (`Stagger`, cap delay at index 8 L912), empty state (L185–190). Quick-add → `WorkoutRepo.addToPlan` + toast. Commit `feat(ui): exercises list + search + filter`.

**DoD:** searching/filtering 1,324 exercises is smooth; quick-add updates plan + toasts.

---

## Phase 8 — Exercise detail overlay

**Files:** `ui/detail/{DetailScreen,DetailViewModel,DetailUiState}.kt`. **Port:** proto L377–443, values L969–986.
Tasks: demo stage (spinning dashed ring + glow + Coil `AsyncImage(gifUrl)` with `ShimmerBox` placeholder + animated `MuscleIcon` fallback when `MEDIA_BASE`/media_id blank; media_id caption), muscle/equip/level chips, Personal best + Last trained cards, target progress block **or** "Set a target" CTA (opens GoalSheet), numbered cues (`instructions.en` → steps), bottom Add + Start bar. `sheetUp` entrance. Commit `feat(ui): exercise detail`.

**DoD:** detail opens with shared-element-style expand; GIF slot shimmers then shows fallback; Start launches single-exercise session.

---

## Phase 9 — Workout builder

**Files:** `ui/builder/{BuilderScreen,BuilderViewModel,BuilderUiState}.kt`. **Port:** proto L195–247, values L916–918.
Tasks: header stats, Intensity segmented (Light/Moderate/Hard → `INT` map L683 sets restDefault+RPE), reorderable rows (up/down `movePlan`), set stepper 1–8 (`setSets`), remove, "Add exercise" → Exercises tab, sticky Start button. Persist plan via `PlanStore`. `scaleIn` on rows. Commit `feat(ui): workout builder`.

**DoD:** plan reorder/edit persists; intensity changes rest+RPE; Start launches full plan.

---

## Phase 10 — Guided session (core)

**Files:** `ui/session/{SessionScreen,SessionViewModel,SessionUiState}.kt`, `domain/SessionEngine.kt`. **Port:** proto L445–539, engine L836–893, values L989–1015.

- [ ] **Step 1:** TDD `SessionEngine` (pure state machine) — tests: work→rest→advance, last set/last exercise → done, PR detection per set (weighted vs BW), volume accrual (weighted only), weight prefill (last ?? DEFAULTW ?? 20), rep step (5 for `"…s"` else 1), weight step (5 lb / 2.5 kg). Implement to pass.
- [ ] **Step 2:** `SessionViewModel` drives engine with a 1s ticker (coroutine), pause/resume, −15/Skip/+15 rest, respects `autorest`.
- [ ] **Step 3:** UI: top bar (close/progress/pause), segment bar, progress ring (`ProgressRing`, lime work / amber rest), big reps or rest clock, weight+reps steppers with live PR pace badge (L999), "Log set & rest", rest controls, completion summary (time/sets/volume + new-PR list, `prPop` + confetti). Haptic tick on steppers (gated).
- [ ] **Step 4:** On finish → `WorkoutRepo.saveSession` (writes history + logs, updates bests), navigate to Progress, toast.
- [ ] **Step 5:** Commit `feat(ui+domain): guided session engine`.

**DoD:** full session loggable end-to-end; PRs detected + celebrated; history/bests persist.

---

## Phase 11 — Progress screen

**Files:** `ui/progress/{ProgressScreen,ProgressViewModel,ProgressUiState}.kt`, `ui/progress/GoalSheet.kt`. **Port:** proto L249–355 (+ goal sheet L592–623), values L920–965.
Tasks: weekly goal ring (sessions vs 5 + dynamic message L1037), weekly volume bar chart (animated heights L933–934), stat grid (streak/total workouts/total volume/PRs-this-month L946), Personal Records list (sorted by e1rm/reps L939–941), Goals & targets (progress bars + delete + "New goal" → `GoalSheet` bottom sheet L955–966), Recent sessions (L947) with empty state (L346–352). Commit `feat(ui): progress + goals`.

**DoD:** all widgets reflect real data; new target flow works; charts animate on first paint (`charted` gate L758).

---

## Phase 12 — Settings, onboarding, splash + polish

**Files:** `ui/settings/*`, `ui/onboarding/*`, `ui/splash/*`.
Tasks:
- **Settings** (proto L541–590, L1017–1024): profile card, units kg/lb, sound/haptics/auto-rest switches (animated knob), rest-default chips, **Clear all data** two-tap confirm (3s window L816–822), privacy note. Persist via `SettingsStore`.
- **Onboarding** (proto L625–649): 3-slide pager (`OB` L724–728), dots, Skip/Continue/Get started, floaty icon anim; set `onboarded`.
- **Splash** (proto L651–660): logo `splashPop` + `glowPulse`; auto-advance after ~1s to onboarding (if not onboarded) or home.
- **Polish pass:** verify every animation vs prototype (springs, staggers, count-ups, ring ticks, PR celebration), 60fps profiling, haptics honor setting.
- Commit `feat(ui): settings + onboarding + splash`; then `chore: animation/perf polish pass`.

**DoD:** first-run = splash → onboarding → home → first logged set with minimal taps; clear-all resets to seed; app feels identical to prototype.

---

## Global Definition of Done

- `./gradlew testDebugUnitTest` and `./gradlew connectedDebugAndroidTest` green.
- Every prototype screen/overlay reproduced (values, copy, motion).
- Fresh install seeds 1,324 exercises + demo data; empty states show real zeros after clear-all.
- `MEDIA_BASE` empty → graceful fallbacks everywhere; setting it later loads GIFs with no code change.
```
