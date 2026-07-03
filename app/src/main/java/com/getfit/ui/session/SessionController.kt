package com.getfit.ui

import com.getfit.data.prefs.PlanItemData
import com.getfit.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Placeholder session controller. The real work/rest state machine, ticker and persistence land in
 * Phase 10 (with a TDD'd SessionEngine). For now the shell compiles and Start is a no-op toast.
 */
class SessionController(
    @Suppress("unused") private val container: AppContainer,
    @Suppress("unused") private val scope: CoroutineScope,
    private val toast: (String, String) -> Unit,
) {
    private val _state = MutableStateFlow<SessionUiState?>(null)
    val state: StateFlow<SessionUiState?> = _state.asStateFlow()

    fun start(items: List<PlanItemData>) {
        toast("Session engine lands in Phase 10", "play_arrow")
    }
}

/** Minimal placeholder; replaced in Phase 10. */
data class SessionUiState(val placeholder: Boolean = true)
