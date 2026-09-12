# Forge Wear OS Companion — Design

> **2026-09 update:** superseded in two places. The protocol and the session engine now live in the
> shared pure-Kotlin `:engine` module (no duplication, no drift test), and the watch runs workouts on
> its own when the phone is out of reach (`LocalSession` + `WearSessionUpload`, see CLAUDE.md). The
> rest of this doc — MessageClient over DataClient, the HR batching, the snapshot shape — still holds.

Scope confirmed: full workout mirror on the watch (active exercise, set/rep target, rest timer,
done/skip taps) plus live heart rate streamed back to the phone. This doc is for review before any
build files are touched, since a new Gradle module is the highest blind-build-risk change made in
this project so far — nothing here has been compiled.

## 1. Module structure

A new Gradle module, `wear`, alongside `app`:

```
GetFitPro/
  app/                 (existing phone app, untouched)
  wear/                (new)
    src/main/
      AndroidManifest.xml
      java/com/getfit/wear/...
      res/
    build.gradle.kts
  settings.gradle.kts   (add `include(":wear")`)
  gradle/libs.versions.toml  (add wear-specific deps)
```

The watch app is a **separate installable APK** bundled into the phone app's release as a Wear
module (`wearApp project(':wear')` in `app/build.gradle.kts`), which is how Play/manual installs
push it to a paired watch automatically. It shares no code with `app` at the source level in v1 —
duplicating the handful of small data classes it needs (session item, set data) is safer than
extracting a shared module blind, given zero build verification. If this proves annoying in
practice once you're building it yourself, a `core` module split is a clean follow-up.

New dependencies (all from Google's Maven, which is why this can't be verified from this session):
- `androidx.wear.compose:compose-material` + `compose-foundation` — Wear's Compose components
  (curved text, rotary input, `ScalingLazyColumn`) — not the phone's Material3, a different library
  built for round/square small screens.
- `com.google.android.gms:play-services-wearable` — the Data Layer API (`MessageClient`,
  `DataClient`, `CapabilityClient`) for phone↔watch communication.
- `androidx.health:health-services-client` — live heart rate during a tracked exercise.
- `androidx.wear:wear-tooling-preview` (dev-only, for `@Preview` on round screens).

## 2. Phone↔watch communication

Two different jobs, two different parts of the Data Layer API — using the right one for each
matters for battery:

**Session mirror (phone → watch, and watch taps → phone): `MessageClient`.**
One-shot, fire-and-forget messages, not a continuous stream. The phone sends a compact JSON
snapshot (current exercise name, target reps, current weight/rep values, phase, rest-seconds-left)
every time `SessionController`'s state actually changes — not on every 1-second tick, since the
watch can derive the same wall-clock-anchored countdown locally from a `restEndAtMs` timestamp
exactly like the phone does (reusing the wall-clock pattern from this session's timer fix, so the
watch doesn't need its own 1Hz link and doesn't drain either device's radio). The watch sends a
message back on "done set" / "skip rest" taps; the phone treats these exactly like a tap on its own
session screen (`SessionController.doneSet()` / `.skip()`), so there's one source of truth and no
new sync logic to get wrong.

**Heart rate (watch → phone): also `MessageClient`, batched.**
The watch samples heart rate via Health Services during an active exercise and sends a small batch
(e.g. every 5-10s, or on session end) rather than pushing every individual sample — continuous
per-beat streaming over the radio is the actual battery cost in most watch apps, so batching is a
deliberate choice, not a shortcut.

**Why not `DataClient`:** `DataClient` is built for durable, syncable state (last-write-wins,
survives being offline) — appropriate for something like "current plan," not for a fast-changing
in-progress session. `MessageClient` is simpler, matches this project's already-established
pattern of doing the minimum viable thing (see: `HttpURLConnection` instead of Retrofit), and is
easier for me to get right without a compiler checking my work.

## 3. Heart rate + calories

`androidx.health.services.client.ExerciseClient` — start an exercise (`ExerciseType.STRENGTH_TRAINING`,
which doesn't require GPS), request `DataType.HEART_RATE_BPM` in the config, and collect
`ExerciseUpdate`s via a callback for live BPM. Requires the `BODY_SENSORS` runtime permission on
the watch.

**Calories: an honest limitation, not a promise.** Health Services doesn't give a real calorie
figure for strength training the way it does for a GPS-tracked run — there's no reliable
first-party number to surface. v1 shows a rough MET-based estimate (bodyweight × a fixed MET value
for resistance training × elapsed time) labeled clearly as an estimate, not a precise reading. This
is what most apps actually do here; I'm not going to fake more precision than the underlying data
supports.

## 4. Watch screens (Wear Compose, round-screen-first)

1. **Idle** — "Open Forge on your phone to start a workout." No session active.
2. **Active exercise** — exercise name (curved top text), current set index/total, target reps,
   current weight (display-unit-aware, mirrors the phone's unit setting), big "Done" tap target,
   live BPM in a corner.
3. **Rest** — countdown ring (reusing the existing `restEndAtMs` wall-clock pattern), +15s / skip
   taps, BPM still visible.
4. **Session end** — brief summary (duration, sets, any PRs), auto-returns to Idle.

No settings screen, no exercise browsing, no history on the watch in v1 — it's a mirror + remote,
not a second copy of the app. Keeping the watch's own state machine minimal is itself a
battery/complexity safeguard, given I can't test it.

## 5. Risk plan, given no build verification

- Every API surface used above (`ExerciseClient`, `MessageClient`, Wear Compose components) is
  stable, long-shipped AndroidX API — not guessing at anything preview/experimental.
- I'll write the watch module's state machine as a near-duplicate of `SessionEngine.kt`'s existing,
  already-tested wall-clock-anchored logic (rest countdown, elapsed time) rather than inventing new
  timing logic for the watch — one less thing to get subtly wrong a second time.
- I'll flag any call site where I'm not fully certain of an exact method signature rather than
  guessing silently.
- Once this is scaffolded, the actual build/run/pair-test can only happen on your machine in
  Android Studio with a paired (or emulated) Wear OS device — that's true of everything in this
  project, but doubly true here since a wrong Gradle module wire-up (`settings.gradle.kts`,
  `wearApp` dependency line) is the kind of mistake that shows up immediately on your first build
  attempt, which is exactly why this doc exists before I touch those files.

## 6. What I need from you before/at build time

- A physical Wear OS watch or an emulator profile to actually pair-test with — I can't verify any
  of this myself.
- Confirmation you're fine with a second APK being added to the release build (`wearApp` wiring) —
  standard for a Wear companion, just flagging the build output changes.

## Next step

Once you've reviewed this, I'll scaffold the `wear` module: `settings.gradle.kts`, `wear/build.gradle.kts`,
manifest, the state-machine port, and the four screens above.
