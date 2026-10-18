package com.flowtube.app.ui.screens.settings.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.flowtube.app.BuildConfig
import com.flowtube.app.data.local.PlayerPreferences
import com.flowtube.app.data.recommendation.FlowNeuroEngine
import com.flowtube.app.data.recommendation.FlowPersona
import com.flowtube.app.data.update.AppRelease
import com.flowtube.app.data.update.UpdateRepository
import com.flowtube.app.player.DeepFocusManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeepFocusState(
    val active: Boolean = false,
    val activatedAt: Long = 0L,
    val expireHours: Int = DeepFocusDefaults.EXPIRE_HOURS,
    val saveToHistory: Boolean = false,
    val scrobble: Boolean = false,
)

private object DeepFocusDefaults {
    const val EXPIRE_HOURS = 4
}

/** The update check the settings list runs on request; github builds only. */
sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState

    data object Checking : UpdateCheckState

    data object UpToDate : UpdateCheckState

    data object Failed : UpdateCheckState

    data class Available(
        val release: AppRelease,
    ) : UpdateCheckState
}

@HiltViewModel
class SettingsHomeViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val playerPreferences: PlayerPreferences,
        private val updates: UpdateRepository,
    ) : ViewModel() {
        val deepFocus: StateFlow<DeepFocusState> =
            combine(
                playerPreferences.deepFocusActive,
                playerPreferences.deepFocusActivatedAt,
                playerPreferences.deepFocusExpireHours,
                playerPreferences.deepFocusSaveToHistory,
                playerPreferences.deepFocusScrobble,
            ) { active, activatedAt, expireHours, saveToHistory, scrobble ->
                DeepFocusState(active, activatedAt, expireHours, saveToHistory, scrobble)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DeepFocusState())

        private val _persona = MutableStateFlow<FlowPersona?>(null)
        val persona: StateFlow<FlowPersona?> = _persona.asStateFlow()

        private val _updateCheck = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
        val updateCheck: StateFlow<UpdateCheckState> = _updateCheck.asStateFlow()

        init {
            refreshPersona()
        }

        /** Re-reads the persona; the engine exposes no stream, so the list refreshes it when shown. */
        fun refreshPersona() {
            viewModelScope.launch {
                _persona.value = runCatching { FlowNeuroEngine.getPersona(FlowNeuroEngine.getBrainSnapshot()) }.getOrNull()
            }
        }

        fun setDeepFocusEnabled(enabled: Boolean) {
            viewModelScope.launch { DeepFocusManager.setEnabled(context, enabled) }
        }

        fun setDeepFocusExpireHours(hours: Int) {
            viewModelScope.launch { playerPreferences.setDeepFocusExpireHours(hours) }
        }

        fun setDeepFocusSaveToHistory(enabled: Boolean) {
            viewModelScope.launch { playerPreferences.setDeepFocusSaveToHistory(enabled) }
        }

        fun setDeepFocusScrobble(enabled: Boolean) {
            viewModelScope.launch { playerPreferences.setDeepFocusScrobble(enabled) }
        }

        fun checkForUpdates() {
            if (!BuildConfig.UPDATER_ENABLED || _updateCheck.value == UpdateCheckState.Checking) return
            _updateCheck.value = UpdateCheckState.Checking
            viewModelScope.launch {
                val result =
                    runCatching { updates.fetch() }
                        .fold(
                            onSuccess = { info -> info?.let { UpdateCheckState.Available(it) } ?: UpdateCheckState.UpToDate },
                            onFailure = { UpdateCheckState.Failed },
                        )
                _updateCheck.value = result
            }
        }

        fun consumeUpdateCheck() {
            _updateCheck.update { if (it == UpdateCheckState.Checking) it else UpdateCheckState.Idle }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
