package com.getfit.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GfDarkColors = darkColorScheme(
    primary = GfColor.Lime,
    onPrimary = GfColor.OnAccent,
    secondary = GfColor.Amber,
    background = GfColor.Background,
    onBackground = GfColor.Text,
    surface = GfColor.Surface,
    onSurface = GfColor.Text,
    surfaceVariant = GfColor.SurfaceElevated,
    onSurfaceVariant = GfColor.TextDim,
    error = GfColor.Coral,
    outline = GfColor.Hairline10,
)

@Composable
fun GetFitTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // GetFit is dark-only by design (warm dark palette from the prototype).
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = GfDarkColors,
        typography = GfType,
        shapes = GfShapes,
        content = content,
    )
}
