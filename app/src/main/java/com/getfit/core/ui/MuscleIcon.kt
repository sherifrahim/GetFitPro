package com.getfit.core.ui

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Muscle group -> icon. The prototype's MICON map uses Material Symbols names ("exercise",
 * "sprint") that have no exact equivalent in Forge's icon set, so they resolve to the nearest
 * available vector here.
 */
object MuscleIcon {
    fun of(muscle: String): ImageVector = when (muscle) {
        "Legs", "Glutes", "Cardio" -> ForgeIcons.DirectionsRun
        "Core" -> ForgeIcons.SelfImprovement
        else -> ForgeIcons.FitnessCenter // Chest, Back, Shoulders, Arms, fallback
    }
}
