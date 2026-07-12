package com.getfit.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.getfit.ui.TAB_HOME
import com.getfit.ui.TAB_PROGRESS
import com.getfit.ui.builder.BuilderScreen
import com.getfit.ui.detail.DetailScreen
import com.getfit.ui.exercises.ExercisesScreen
import com.getfit.ui.home.HomeScreen
import com.getfit.ui.onboarding.OnboardingScreen
import com.getfit.ui.progress.GoalSheet
import com.getfit.ui.progress.ProgressScreen
import com.getfit.ui.session.SessionScreen
import com.getfit.ui.settings.SettingsScreen
import com.getfit.ui.splash.SplashScreen

/** Tab shell + full-screen overlays + toast host. */
@Composable
fun RootScaffold(vm: AppViewModel) {
    val nav by vm.nav.collectAsState()
    val settings by vm.settings.collectAsState()

    // Lowest priority: any non-Home tab goes back to Home before the system handles back (exit).
    BackHandler(enabled = nav.tab != TAB_HOME) { vm.selectTab(TAB_HOME) }

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

        // Exercise detail overlay
        BackHandler(enabled = nav.detailId != null) { vm.closeDetail() }
        AnimatedVisibility(
            visible = nav.detailId != null,
            enter = slideInVertically(tween(400)) { it },
            exit = slideOutVertically(tween(300)) { it },
        ) {
            nav.detailId?.let { DetailScreen(vm, it) }
        }

        // Settings overlay
        BackHandler(enabled = nav.settingsOpen) { vm.closeSettings() }
        AnimatedVisibility(
            visible = nav.settingsOpen,
            enter = slideInVertically(tween(350)) { it },
            exit = slideOutVertically(tween(300)) { it },
        ) {
            SettingsScreen(vm)
        }

        // Goal bottom sheet
        BackHandler(enabled = nav.goalOpen) { vm.closeGoal() }
        AnimatedVisibility(visible = nav.goalOpen, enter = fadeIn(tween(250)), exit = fadeOut(tween(200))) {
            GoalSheet(vm)
        }

        // Guided session (top-most)
        val session by vm.session.collectAsState()
        BackHandler(enabled = session != null) { vm.endSession() }
        AnimatedVisibility(visible = session != null, enter = fadeIn(tween(300)), exit = fadeOut(tween(250))) {
            SessionScreen(vm)
        }

        // Toast
        Box(Modifier.fillMaxSize().padding(bottom = 96.dp), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(visible = nav.toast != null) {
                nav.toast?.let { GfToast(it.text, msIcon(it.icon)) }
            }
        }

        // Onboarding (first run)
        AnimatedVisibility(visible = nav.booted && !settings.onboarded, enter = fadeIn(tween(400)), exit = fadeOut(tween(300))) {
            OnboardingScreen(vm)
        }

        // Splash
        AnimatedVisibility(visible = !nav.booted, enter = fadeIn(), exit = fadeOut()) {
            SplashScreen()
        }
    }
}
