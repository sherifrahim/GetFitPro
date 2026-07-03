package com.getfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.getfit.core.theme.GetFitTheme
import com.getfit.core.theme.GfColor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GetFitTheme {
                // Placeholder shell until the navigation shell lands in Phase 5.
                AppRootPlaceholder()
            }
        }
    }
}

@Composable
private fun AppRootPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize().background(GfColor.Background),
        contentAlignment = Alignment.Center,
    ) {
        Text("GetFit", color = Color(0xFFCBF25C))
    }
}
