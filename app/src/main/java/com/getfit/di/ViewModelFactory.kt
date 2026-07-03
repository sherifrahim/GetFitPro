package com.getfit.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.getfit.ui.AppViewModel

/** Manual DI ViewModel factory. */
class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AppViewModel::class.java) -> AppViewModel(container) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
