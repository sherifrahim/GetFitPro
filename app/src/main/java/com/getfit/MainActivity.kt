package com.getfit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.getfit.core.theme.GetFitTheme
import com.getfit.di.ViewModelFactory
import com.getfit.ui.AppViewModel
import com.getfit.ui.nav.RootScaffold
import com.getfit.widget.NextWorkoutWidget

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels {
        ViewModelFactory((application as GetFitApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            GetFitTheme {
                RootScaffold(vm)
            }
        }
        handleStartIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleStartIntent(intent)
    }

    /** The widget's Start pill: open straight into the routine (blank id = whichever is up next). */
    private fun handleStartIntent(intent: Intent?) {
        if (intent?.hasExtra(NextWorkoutWidget.EXTRA_START_ROUTINE) != true) return
        val id = intent.getStringExtra(NextWorkoutWidget.EXTRA_START_ROUTINE).orEmpty()
        intent.removeExtra(NextWorkoutWidget.EXTRA_START_ROUTINE)
        vm.startRoutineWhenLoaded(id)
    }
}
