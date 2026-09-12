package com.getfit.data.db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Raw record from assets/exercises.json (ExerciseDB taxonomy). */
@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    @SerialName("body_part") val bodyPart: String? = null,
    val equipment: String? = null,
    val target: String? = null,
    @SerialName("secondary_muscles") val secondaryMuscles: List<String> = emptyList(),
    val instructions: Map<String, String> = emptyMap(),
    @SerialName("instruction_steps") val instructionSteps: Map<String, List<String>> = emptyMap(),
    @SerialName("media_id") val mediaId: String? = null,
)

/** Maps ExerciseDB body_part/target into the app's 8 muscle groups. */
fun mapMuscle(bodyPart: String?, target: String?): String {
    if (target?.lowercase() == "glutes") return "Glutes"
    return when (bodyPart?.lowercase()) {
        "chest" -> "Chest"
        "back" -> "Back"
        "shoulders", "neck" -> "Shoulders"
        "upper arms", "lower arms" -> "Arms"
        "upper legs", "lower legs" -> "Legs"
        "waist" -> "Core"
        "cardio" -> "Cardio"
        else -> "Core"
    }
}

fun normalizeEquipment(equip: String?): String {
    val e = equip?.trim()?.lowercase() ?: return "Bodyweight"
    return if (e == "body weight") "Bodyweight" else titleCase(e)
}

private val BEGINNER_EQUIP = setOf(
    "body weight", "assisted", "band", "resistance band", "stability ball",
    "bosu ball", "medicine ball", "roller", "wheel roller",
)
private val ADVANCED_EQUIP = setOf(
    "olympic barbell", "ez barbell", "trap bar", "weighted", "sled machine",
    "tire", "skierg machine",
)

fun deriveLevel(equip: String?): String {
    val e = equip?.trim()?.lowercase() ?: return "Beginner"
    return when {
        e in BEGINNER_EQUIP -> "Beginner"
        e in ADVANCED_EQUIP -> "Advanced"
        else -> "Intermediate"
    }
}

fun defaultReps(muscle: String, normalizedEquip: String): String = when {
    muscle == "Cardio" -> "15"
    normalizedEquip == "Bodyweight" -> "12"
    muscle == "Core" -> "12"
    else -> "10"
}

fun titleCase(s: String): String =
    s.split(" ").filter { it.isNotEmpty() }.joinToString(" ") { w ->
        w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

/** Convert a dataset record into an ExerciseEntity. */
fun ExerciseDto.toEntity(): ExerciseEntity {
    val muscle = mapMuscle(bodyPart, target)
    val equip = normalizeEquipment(equipment)
    val cues = instructionSteps["en"]
        ?: instructions["en"]?.split(Regex("(?<=\\.)\\s+"))?.filter { it.isNotBlank() }
        ?: emptyList()
    return ExerciseEntity(
        id = id,
        name = titleCase(name),
        muscle = muscle,
        equipment = equip,
        level = deriveLevel(equipment),
        target = target?.let { titleCase(it) } ?: muscle,
        secondaryMuscles = secondaryMuscles.joinToString(", ") { titleCase(it) },
        reps = defaultReps(muscle, equip),
        mediaId = mediaId,
        cues = cues.joinToString("||"),
        curated = false,
    )
}

fun CuratedExercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id, name = name, muscle = muscle, equipment = equip, level = level,
    target = muscle, secondaryMuscles = "", reps = reps, mediaId = mediaId,
    cues = cues.joinToString("||"), curated = true,
)

/**
 * Muscle group for an exercise the library doesn't know, from its name alone — used by the CSV
 * importer for the `imp_*` rows it invents. Before this every unmatched import landed in "Core",
 * which skewed the muscle-balance analysis and the recovery heuristic. Order matters: the more
 * specific cues (hip thrust → Glutes, face pull → Shoulders) are checked before the generic ones
 * (thrust, pull). Unknown names fall back to "Core" as before.
 */
fun inferMuscle(name: String): String {
    val n = name.lowercase()
    fun has(vararg k: String) = k.any { n.contains(it) }
    return when {
        has("hip thrust", "glute", "hip abduct", "hip adduct") -> "Glutes"
        has("romanian", "rdl", "stiff-leg", "stiff leg", "leg curl", "hamstring", "nordic") -> "Legs"
        has("treadmill", "running", " run", "cycling", "bike", "elliptical", "rower", "rowing machine", "jump rope", "skipping", "stair", "burpee", "sprint", "walking", "jog") -> "Cardio"
        has("face pull", "lateral raise", "front raise", "rear delt", "shoulder press", "overhead press", "military press", "arnold", "shrug", "upright row", "delt") -> "Shoulders"
        has("curl", "tricep", "bicep", "skull", "pushdown", "push down", "kickback", "preacher", "hammer", "overhead extension", "dip") -> "Arms"
        has("crunch", "plank", "sit-up", "situp", "sit up", "leg raise", "knee raise", "ab ", "abs", "abdominal", "russian twist", "dead bug", "hollow", "wood chop", "pallof", "hanging") -> "Core"
        has("bench", "chest", "pec", "push-up", "push up", "pushup", "fly", "flye", "incline press", "decline press") -> "Chest"
        has("row", "pulldown", "pull down", "pull-up", "pull up", "pullup", "chin-up", "chin up", "chinup", "lat ", "lats", "deadlift", "back extension", "hyperextension", "good morning", "rack pull", "pullover") ->
            if (has("upright row")) "Shoulders" else "Back"
        has("squat", "leg", "lunge", "calf", "hamstring", "quad", "hack", "step-up", "step up", "split", "sled", "thrust", "adductor", "abductor", "rdl", "romanian") -> "Legs"
        else -> "Core"
    }
}
