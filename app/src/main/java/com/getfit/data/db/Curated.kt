package com.getfit.data.db

/**
 * The 18 curated exercises + seed data ported verbatim from GetFit.dc.html (L692-728, L754).
 * These power the default Push Day plan, demo history, PR records and targets on first launch,
 * so a fresh install looks exactly like the prototype. Curated ids ("bench") never collide with
 * dataset ids ("0001").
 */
data class CuratedExercise(
    val id: String,
    val mediaId: String?,
    val name: String,
    val muscle: String,
    val equip: String,
    val level: String,
    val reps: String,
    val cues: List<String>,
)

object Curated {

    val MUSCLES = listOf("All", "Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Cardio", "Glutes")

    /** Intensity -> rest seconds + RPE label + accent color hex (proto INT, L683). */
    data class Intensity(val rest: Int, val rpe: String, val colorArgb: Long)
    val INTENSITY = linkedMapOf(
        "Light" to Intensity(45, "RPE 6", 0xFF86D6B4),
        "Moderate" to Intensity(60, "RPE 7–8", 0xFFF3B24A),
        "Hard" to Intensity(90, "RPE 9+", 0xFFF0774E),
    )

    val EXERCISES = listOf(
        CuratedExercise("bench", "EIeI8Vf", "Barbell Bench Press", "Chest", "Barbell", "Intermediate", "8",
            listOf("Set your feet, arch slightly and pin your shoulder blades back.",
                "Lower the bar under control to your mid-chest.",
                "Drive up and slightly back, locking out over the shoulders.")),
        CuratedExercise("pushup", null, "Push-Up", "Chest", "Bodyweight", "Beginner", "12",
            listOf("Start in a tight plank, hands just wider than shoulders.",
                "Lower until your chest nearly touches the floor.",
                "Press up and squeeze, keeping a straight line head to heel.")),
        CuratedExercise("incline", null, "Incline Dumbbell Press", "Chest", "Dumbbell", "Intermediate", "10",
            listOf("Set the bench to about 30 degrees.",
                "Press the dumbbells up until they nearly touch.",
                "Lower slowly to a deep stretch at chest level.")),
        CuratedExercise("pullup", "lBDjFxJ", "Pull-Up", "Back", "Bodyweight", "Advanced", "6",
            listOf("Hang with a full grip, shoulders slightly wider.",
                "Pull your chest toward the bar, driving elbows down.",
                "Lower with control to a full hang.")),
        CuratedExercise("row", null, "Bent-Over Barbell Row", "Back", "Barbell", "Intermediate", "8",
            listOf("Hinge to about 45 degrees with a flat back.",
                "Row the bar to your lower ribs, squeezing the lats.",
                "Lower under control without rounding.")),
        CuratedExercise("pulldown", null, "Lat Pulldown", "Back", "Cable", "Beginner", "10",
            listOf("Grip wider than shoulders and sit tall.",
                "Pull the bar to your upper chest, elbows down.",
                "Return slowly, letting the lats stretch.")),
        CuratedExercise("squat", "qXTaZnJ", "Barbell Back Squat", "Legs", "Barbell", "Intermediate", "6",
            listOf("Bar on your upper traps, brace your core hard.",
                "Sit down and back until hips pass the knees.",
                "Drive through mid-foot to stand tall.")),
        CuratedExercise("rdl", null, "Romanian Deadlift", "Legs", "Barbell", "Intermediate", "8",
            listOf("Soft knees, push your hips back.",
                "Slide the bar down your thighs to a stretch.",
                "Squeeze the glutes to stand up straight.")),
        CuratedExercise("lunge", null, "Walking Lunge", "Legs", "Dumbbell", "Beginner", "10",
            listOf("Step forward into a long stride.",
                "Drop the back knee toward the floor.",
                "Push off the front heel into the next step.")),
        CuratedExercise("legpress", null, "Leg Press", "Legs", "Machine", "Beginner", "12",
            listOf("Feet shoulder-width on the platform.",
                "Lower until knees reach about 90 degrees.",
                "Press away without locking the knees hard.")),
        CuratedExercise("ohp", null, "Overhead Press", "Shoulders", "Barbell", "Intermediate", "8",
            listOf("Bar on your front delts, elbows under.",
                "Brace and press straight overhead.",
                "Lock out with the bar over mid-foot.")),
        CuratedExercise("lateral", "DsgkuIt", "Lateral Raise", "Shoulders", "Dumbbell", "Beginner", "12",
            listOf("Slight bend in the elbows.",
                "Raise out to the sides to shoulder height.",
                "Lower slowly, leading with the elbows.")),
        CuratedExercise("curl", "NbVPDMW", "Dumbbell Bicep Curl", "Arms", "Dumbbell", "Beginner", "12",
            listOf("Elbows tucked at your sides.",
                "Curl up, turning the pinkies slightly out.",
                "Lower fully for a complete stretch.")),
        CuratedExercise("dip", null, "Tricep Dip", "Arms", "Bodyweight", "Intermediate", "10",
            listOf("Support yourself with arms locked.",
                "Lower until elbows reach 90 degrees.",
                "Press back up and squeeze the triceps.")),
        CuratedExercise("plank", null, "Plank", "Core", "Bodyweight", "Beginner", "45s",
            listOf("Forearms down, elbows under shoulders.",
                "Squeeze glutes and brace your abs.",
                "Hold a straight line and breathe steadily.")),
        CuratedExercise("legraise", null, "Hanging Leg Raise", "Core", "Bodyweight", "Advanced", "10",
            listOf("Hang tall from a bar.",
                "Raise your legs to horizontal, no swing.",
                "Lower with control back to the hang.")),
        CuratedExercise("swing", null, "Kettlebell Swing", "Glutes", "Kettlebell", "Intermediate", "15",
            listOf("Hinge and hike the bell back.",
                "Snap the hips forward to float it up.",
                "Let it fall and load the next rep.")),
        CuratedExercise("burpee", null, "Burpee", "Cardio", "Bodyweight", "Advanced", "12",
            listOf("Drop to a plank and touch your chest down.",
                "Jump the feet back in.",
                "Explode up into a jump and repeat.")),
    )

    /** Default weight prefill (proto DEFAULTW, L715). */
    val DEFAULT_WEIGHT = mapOf(
        "bench" to 60.0, "pushup" to 0.0, "incline" to 24.0, "pullup" to 0.0, "row" to 50.0,
        "pulldown" to 45.0, "squat" to 80.0, "rdl" to 70.0, "lunge" to 16.0, "legpress" to 120.0,
        "ohp" to 35.0, "lateral" to 10.0, "curl" to 12.0, "dip" to 0.0, "plank" to 0.0,
        "legraise" to 0.0, "swing" to 20.0, "burpee" to 0.0,
    )

    /** Default Push Day plan (proto DEFAULT_PLAN, L717): id -> sets x reps. */
    data class PlanItem(val id: String, val sets: Int, val reps: String)
    val DEFAULT_PLAN = listOf(
        PlanItem("bench", 4, "8"), PlanItem("incline", 3, "10"), PlanItem("ohp", 3, "8"),
        PlanItem("lateral", 3, "12"), PlanItem("dip", 3, "12"),
    )

    /**
     * The routines a fresh install starts with. Push Day is the prototype's DEFAULT_PLAN verbatim;
     * Pull and Legs complete a classic three-day rotation from the same curated exercises, so every
     * default routine has demo GIFs, cues and prefilled weights on day one.
     */
    data class DefaultRoutine(val id: String, val name: String, val items: List<PlanItem>)
    const val DEFAULT_ROUTINE_ID = "r_push"
    val DEFAULT_ROUTINES = listOf(
        DefaultRoutine(DEFAULT_ROUTINE_ID, "Push Day", DEFAULT_PLAN),
        DefaultRoutine(
            "r_pull", "Pull Day",
            listOf(PlanItem("pullup", 4, "6"), PlanItem("row", 4, "8"), PlanItem("pulldown", 3, "10"), PlanItem("curl", 3, "12")),
        ),
        DefaultRoutine(
            "r_legs", "Leg Day",
            listOf(PlanItem("squat", 4, "6"), PlanItem("rdl", 3, "8"), PlanItem("legpress", 3, "12"), PlanItem("lunge", 3, "10"), PlanItem("plank", 3, "45s")),
        ),
    )

    /**
     * Program templates: named sets of routines built from the curated exercises, so "run PPL" or
     * "run GZCLP" is one tap. Percentages/progression schemes (5/3/1's training-max maths, GZCLP's
     * stage drops) are deliberately NOT modelled — Forge tracks what was lifted, the user picks the
     * load; the templates give the structure and rep targets only.
     */
    data class Program(val id: String, val name: String, val blurb: String, val days: List<DefaultRoutine>)
    val PROGRAMS = listOf(
        Program("ppl", "Push / Pull / Legs", "The classic 3-day split. Rotate through all three, rest when you need.", DEFAULT_ROUTINES),
        Program(
            "ul", "Upper / Lower", "Two days, alternate them. Four sessions a week hits everything twice.",
            listOf(
                DefaultRoutine("p_ul_upper", "Upper", listOf(PlanItem("bench", 4, "6"), PlanItem("row", 4, "8"), PlanItem("ohp", 3, "8"), PlanItem("pulldown", 3, "10"), PlanItem("curl", 3, "12"), PlanItem("dip", 3, "10"))),
                DefaultRoutine("p_ul_lower", "Lower", listOf(PlanItem("squat", 4, "6"), PlanItem("rdl", 3, "8"), PlanItem("legpress", 3, "12"), PlanItem("lunge", 3, "10"), PlanItem("legraise", 3, "12"), PlanItem("plank", 3, "45s"))),
            ),
        ),
        Program(
            "fb3", "Full body ×3", "Three different full-body days. Good for 3 sessions a week or a busy schedule.",
            listOf(
                DefaultRoutine("p_fb_a", "Full body A", listOf(PlanItem("squat", 3, "5"), PlanItem("bench", 3, "5"), PlanItem("row", 3, "8"), PlanItem("plank", 2, "45s"))),
                DefaultRoutine("p_fb_b", "Full body B", listOf(PlanItem("rdl", 3, "8"), PlanItem("ohp", 3, "5"), PlanItem("pullup", 3, "6"), PlanItem("legraise", 2, "12"))),
                DefaultRoutine("p_fb_c", "Full body C", listOf(PlanItem("legpress", 3, "10"), PlanItem("incline", 3, "8"), PlanItem("pulldown", 3, "10"), PlanItem("curl", 2, "12"))),
            ),
        ),
        Program(
            "531", "5/3/1-style", "One main lift a day, 5×5 on it, then assistance. Add weight when all five sets move well.",
            listOf(
                DefaultRoutine("p_531_ohp", "Press day", listOf(PlanItem("ohp", 5, "5"), PlanItem("dip", 3, "10"), PlanItem("lateral", 3, "12"), PlanItem("pushup", 3, "12"))),
                DefaultRoutine("p_531_dl", "Pull day", listOf(PlanItem("rdl", 5, "5"), PlanItem("row", 4, "8"), PlanItem("pulldown", 3, "10"), PlanItem("plank", 3, "45s"))),
                DefaultRoutine("p_531_bench", "Bench day", listOf(PlanItem("bench", 5, "5"), PlanItem("incline", 3, "8"), PlanItem("curl", 3, "12"), PlanItem("pushup", 3, "12"))),
                DefaultRoutine("p_531_squat", "Squat day", listOf(PlanItem("squat", 5, "5"), PlanItem("lunge", 3, "10"), PlanItem("legpress", 3, "10"), PlanItem("legraise", 3, "12"))),
            ),
        ),
        Program(
            "gzclp", "GZCLP", "Linear progression: a heavy T1 (5×3), a moderate T2 (3×10) and a light T3 (3×15) each day, cycling A1 B1 A2 B2.",
            listOf(
                DefaultRoutine("p_gz_a1", "GZCLP A1", listOf(PlanItem("squat", 5, "3"), PlanItem("bench", 3, "10"), PlanItem("pulldown", 3, "15"))),
                DefaultRoutine("p_gz_b1", "GZCLP B1", listOf(PlanItem("ohp", 5, "3"), PlanItem("rdl", 3, "10"), PlanItem("row", 3, "15"))),
                DefaultRoutine("p_gz_a2", "GZCLP A2", listOf(PlanItem("bench", 5, "3"), PlanItem("squat", 3, "10"), PlanItem("pulldown", 3, "15"))),
                DefaultRoutine("p_gz_b2", "GZCLP B2", listOf(PlanItem("rdl", 5, "3"), PlanItem("ohp", 3, "10"), PlanItem("row", 3, "15"))),
            ),
        ),
        Program(
            "bw", "Bodyweight only", "No equipment beyond a bar to hang from. Two alternating days.",
            listOf(
                DefaultRoutine("p_bw_a", "Bodyweight A", listOf(PlanItem("pushup", 4, "12"), PlanItem("pullup", 4, "6"), PlanItem("lunge", 3, "12"), PlanItem("plank", 3, "45s"))),
                DefaultRoutine("p_bw_b", "Bodyweight B", listOf(PlanItem("dip", 3, "10"), PlanItem("burpee", 3, "12"), PlanItem("legraise", 3, "12"), PlanItem("pushup", 3, "15"))),
            ),
        ),
    )

    /** Exercises offered in the "New target" sheet (proto GOAL_EX, L718). */
    val GOAL_EX = listOf("bench", "squat", "ohp", "row", "rdl", "pulldown", "curl", "incline", "swing")

    /** Prototype demo history (proto SEED, L719-723). No longer seeded into the app — kept only as
     *  the reference values the PR unit tests are written against. */
    val SEED: Map<String, List<Triple<Double, Int, Int>>> = mapOf(
        "bench" to listOf(Triple(85.0, 8, 40), Triple(92.5, 6, 20), Triple(100.0, 5, 4)),
        "squat" to listOf(Triple(110.0, 6, 35), Triple(130.0, 5, 15), Triple(140.0, 3, 6)),
        "ohp" to listOf(Triple(45.0, 8, 30), Triple(55.0, 6, 12), Triple(60.0, 5, 5)),
        "row" to listOf(Triple(70.0, 8, 25), Triple(85.0, 6, 8)),
        "rdl" to listOf(Triple(100.0, 8, 22), Triple(120.0, 6, 7)),
        "curl" to listOf(Triple(16.0, 12, 18), Triple(20.0, 10, 3)),
        "pulldown" to listOf(Triple(60.0, 10, 20), Triple(70.0, 8, 9)),
        "dip" to listOf(Triple(0.0, 12, 14), Triple(0.0, 15, 2)),
        "incline" to listOf(Triple(28.0, 10, 16), Triple(34.0, 8, 5)),
    )

    /** Onboarding slides (proto OB, L724-728). */
    data class OnboardSlide(val icon: String, val iconArgb: Long, val glowArgb: Long, val title: String, val body: String)
    val ONBOARD = listOf(
        // Copy avoids promising animated demos until MEDIA_BASE is wired to a GIF host.
        // All three slides now carry the single accent blue; the old per-slide lime/amber/mint trio
        // was brand colour, not meaning. Count matches the shipped dataset (1,342) — it read 1,324
        // (the planned figure) while the Exercises screen derived the real one, so the two disagreed.
        OnboardSlide("exercise", 0xFF0B7BF7, 0x590B7BF7, "1,342 exercises, fully detailed",
            "Browse a huge library with step-by-step cues and filters for every muscle and piece of kit."),
        OnboardSlide("monitoring", 0xFF0B7BF7, 0x4C0B7BF7, "Log every set, chase every PR",
            "Track weight and reps as you train. Forge spots personal records the moment you hit them."),
        OnboardSlide("flag", 0xFF0B7BF7, 0x4C0B7BF7, "Set targets, watch them close",
            "Pick a goal weight and a deadline, then follow your progress. Everything stays private, on your device."),
    )

    /** Prototype demo targets (proto L754). No longer seeded; see SEED. */
    data class SeedTarget(val id: String, val exId: String, val target: Double, val start: Double, val startedDaysAgo: Int, val weeks: Int)
    val SEED_TARGETS = listOf(
        SeedTarget("t1", "bench", 110.0, 100.0, 4, 8),
        SeedTarget("t2", "squat", 160.0, 140.0, 6, 12),
    )
}
