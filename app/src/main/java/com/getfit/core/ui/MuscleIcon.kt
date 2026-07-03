package com.getfit.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Muscle group -> icon. The prototype's MICON map uses Material Symbols names ("exercise",
 * "sprint") that have no exact Material Icons Extended equivalent, so they resolve to the nearest
 * available vector here.
 */
object MuscleIcon {
    fun of(muscle: String): ImageVector = when (muscle) {
        "Legs", "Glutes", "Cardio" -> Icons.AutoMirrored.Filled.DirectionsRun
        "Core" -> Icons.Filled.SelfImprovement
        else -> Icons.Filled.FitnessCenter // Chest, Back, Shoulders, Arms, fallback
    }
}
