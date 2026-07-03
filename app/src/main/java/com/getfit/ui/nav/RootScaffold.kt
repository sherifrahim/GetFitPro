package com.getfit.ui.nav

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.getfit.core.theme.GfColor
import com.getfit.core.ui.GfToast
import com.getfit.core.ui.msIcon
import com.getfit.ui.AppViewModel
import com.getfit.ui.TAB_BUILD
import com.getfit.ui.TAB_EXERCISES
import com.getfit.ui.TAB_PROGRESS
import com.getfit.ui.builder.BuilderScreen
import com.getfit.ui.exercises.ExercisesScreen
import com.getfit.ui.home.HomeScreen
import com.getfit.ui.progress.ProgressScreen
import com.getfit.ui.splash.SplashScreen

/** Tab shell + full-screen overlays + toast host. */
@Composable
fun RootScaffold(vm: AppViewModel) {
    val nav by vm.nav.collectAsState()

    Box(Modifier.fillMaxSize().background(GfColor.Background)) {

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = nav.tab,
                    transitionSpec = {
                        (fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 14 })
                            .togetherWith(fadeOut(tween(180)))
                    },
                    label = "tab",
                ) { tab ->
                    when (tab) {
                        TAB_EXERCISES -> ExercisesScreen(vm)
                        TAB_BUILD -> BuilderScreen(vm)
                        TAB_PROGRESS -> ProgressScreen(vm)
                        else -> HomeScreen(vm)
                    }
                }
            }
            BottomBar(nav.tab, vm::selectTab, Modifier.navigationBarsPadding())
        }

        // Toast
        Box(Modifier.fillMaxSize().padding(bottom = 96.dp), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(visible = nav.toast != null) {
                nav.toast?.let { GfToast(it.text, msIcon(it.icon)) }
            }
        }

        // Splash
        AnimatedVisibility(visible = !nav.booted, enter = fadeIn(), exit = fadeOut()) {
            SplashScreen()
        }
    }
}
