package com.neurotube.app.ui.screens.settings.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neurotube.app.account.AccountImportState
import com.neurotube.app.account.AccountImportStep
import com.neurotube.app.account.AccountLibraryImport
import com.neurotube.app.account.YouTubeAccount
import com.neurotube.app.account.YouTubeAccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the account page is doing right now. */
sealed interface YouTubeSignInState {
    data object Idle : YouTubeSignInState

    data object Working : YouTubeSignInState

    data object Failed : YouTubeSignInState
}

@HiltViewModel
class YouTubeAccountViewModel
    @Inject
    constructor(
        private val manager: YouTubeAccountManager,
        private val libraryImport: AccountLibraryImport,
    ) : ViewModel() {
        val account: StateFlow<YouTubeAccount?> = manager.account

        private val _signInState = MutableStateFlow<YouTubeSignInState>(YouTubeSignInState.Idle)
        val signInState: StateFlow<YouTubeSignInState> = _signInState.asStateFlow()

        private val _importState = MutableStateFlow<AccountImportState>(AccountImportState.Idle)
        val importState: StateFlow<AccountImportState> = _importState.asStateFlow()

        fun signIn(activity: android.app.Activity?) {
            if (_signInState.value == YouTubeSignInState.Working) return
            _signInState.value = YouTubeSignInState.Working
            viewModelScope.launch {
                _signInState.value =
                    manager.signIn(activity).fold(
                        onSuccess = { YouTubeSignInState.Idle },
                        onFailure = { YouTubeSignInState.Failed },
                    )
            }
        }

        fun consumeFailure() {
            _signInState.value = YouTubeSignInState.Idle
        }

        fun signOut() {
            viewModelScope.launch { manager.signOut() }
        }

        /** Brings the signed-in account's library in, one part at a time. */
        fun importLibrary() {
            if (_importState.value is AccountImportState.Running) return
            _importState.value = AccountImportState.Running(AccountImportStep.SUBSCRIPTIONS)
            viewModelScope.launch {
                val report =
                    runCatching {
                        libraryImport.run { step -> _importState.value = AccountImportState.Running(step) }
                    }.getOrNull()
                _importState.value = report?.let(AccountImportState::Done) ?: AccountImportState.Failed
            }
        }

        /** Clears the last import's result once it has been shown. */
        fun consumeImportResult() {
            val state = _importState.value
            if (state is AccountImportState.Done || state is AccountImportState.Failed) {
                _importState.value = AccountImportState.Idle
            }
        }
    }
