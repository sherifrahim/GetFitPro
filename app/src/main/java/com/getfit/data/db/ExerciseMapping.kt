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
