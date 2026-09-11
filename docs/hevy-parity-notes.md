# Hevy walkthrough → Forge parity notes

Captured from Hevy (com.hevy) on the user's OPPO, 2026-09-11. Source of ideas for Routines,
Settings/Profile parity, the per-day workout feed, and the AI coach. Not a spec to copy pixel-for-
pixel — Forge keeps the prototype's layout language and the black + blue theme.

## Workout tab (= our Build tab)
- "Start Empty Workout" row at top.
- "Routines" header + folder-add icon; "New Routine" / "Explore" buttons.
- "My Routines (N)" collapsible list. Each card: name, comma-joined exercise preview (1–2 lines,
  ellipsised), full-width blue **Start Routine**, and a "⋯" sheet: Share / Duplicate / Edit / Delete.
- Routine editor: title field; per exercise → notes, per-exercise rest timer, set table with
  columns SET | KG | REP RANGE (min "to" max), "Add Set"; bodyweight exercises drop the KG column.

## Profile tab
- Header: username, avatar, edit / share / settings icons. Stats row: Workouts · Followers · Following.
- "N hours this week" + bar chart with Duration / Volume / Reps toggles and a range picker
  (Last 3 months).
- Dashboard grid: Statistics · Exercises · Measures · Calendar.
- **Workouts feed** (per-day): card per workout with name, date, Time · Volume · Records · Avg bpm,
  first 3 exercises as "N sets <name>" with the exercise GIF thumbnail, "See N more", like/comment/
  share row.
- Workout Detail: Time · Volume · Sets · Records · Avg bpm · Calories; Muscle Split bars (% per
  muscle); Avg HR + HR-over-time graph; "Logged with WearOS Watch"; per-exercise SET | WEIGHT & REPS
  tables; "⋯": Save as Routine / Copy Workout / Edit Workout / Delete Workout.
- Edit Profile: picture, Name, Bio, Link; Private: Sex, Birthday.
- Measurements: empty state → "Add Measurement" / "Add Progress Picture".

## Settings
- Account: Profile · Account · Manage Subscription · Notifications.
- Preferences: Workouts · Privacy & Social · Units · Language · Integrations · Theme ·
  Export & Import Data.
- Guides: Getting Started · Routine Help. Help: FAQ · Contact Us · Review.
- **Workout Settings**: Sounds; Default Rest Timer; First day of the week; Previous Workout Values;
  Warm-up Calculator (PRO); Warm-up Sets; Keep Awake During Workout; Plate Calculator;
  RPE Tracking; Smart Superset Scrolling; Inline Timer; Live Personal Record Notification.

## What Forge takes from this (in build order)
1. Routines (phone + watch): ordered list, current + up-next, per-exercise sets/reps, start from
   watch or phone, session named after the routine, rotation advances on finish.
2. Settings/Profile parity: real name + avatar initials, weekly goal, first day of week, keep awake,
   PR notifications, rest timer default, measurements (weight/body-fat), about/help links.
3. Per-day workout feed with Time · Volume · Sets · Records · Avg bpm · est. Calories, and a
   workout detail with muscle split + per-exercise set tables.
4. AI coach: post-workout critique against the routine and history, next PR attempts, goal review.
