package com.getfit.ui.exercises

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.getfit.core.theme.GfColor
import com.getfit.ui.AppViewModel

// Fleshed out in Phase 7.
@Composable
fun ExercisesScreen(vm: AppViewModel) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Exercises", color = GfColor.Text)
    }
}
