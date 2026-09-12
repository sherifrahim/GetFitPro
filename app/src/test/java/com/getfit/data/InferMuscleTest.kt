package com.getfit.data

import com.getfit.data.db.inferMuscle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InferMuscleTest {
    @Test fun common_hevy_names_map_to_the_right_groups() {
        val cases = mapOf(
            "Seated Leg Curl (Machine)" to "Legs",
            "Hack Squat (Machine)" to "Legs",
            "Romanian Deadlift (Dumbbell)" to "Legs",
            "Hammer Curl (Dumbbell)" to "Arms",
            "Decline Bench Press (Machine)" to "Chest",
            "Single Arm Cable Row" to "Back",
            "Pull Up (Assisted)" to "Back",
            "Shoulder Press (Dumbbell)" to "Shoulders",
            "Lateral Raise (Dumbbell)" to "Shoulders",
            "Face Pull" to "Shoulders",
            "Hip Thrust (Barbell)" to "Glutes",
            "Triceps Pushdown" to "Arms",
            "Cable Crunch" to "Core",
            "Hanging Leg Raise" to "Core",
            "Treadmill" to "Cardio",
            "Incline Bench Press (Barbell)" to "Chest",
            "Chest Fly (Machine)" to "Chest",
            "Something Odd" to "Core",
        )
        cases.forEach { (name, expected) -> assertThat("$name -> ${inferMuscle(name)}").isEqualTo("$name -> $expected") }
    }
}
