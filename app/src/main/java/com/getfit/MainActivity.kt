package com.getfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.getfit.core.theme.GetFitTheme
import com.getfit.di.ViewModelFactory
import com.getfit.ui.AppViewModel
import com.getfit.ui.nav.RootScaffold

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
    }
}
