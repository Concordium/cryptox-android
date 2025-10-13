package com.concordium.wallet.ui.seed.reveal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SavedSeedPhraseRevealViewModel(application: Application) : AndroidViewModel(application) {
    // The initial value is a random phrase to be shown under a blur.
    // The following phrase(s) is guaranteed to be randomly generated 7 years ago.
    private val mutablePhraseFlow: MutableStateFlow<List<String>> = MutableStateFlow(
        ("tonight fat have keen intact happy social powder tired shaft length cram " +
                "buyer need midnight amateur mix jungle top odor mouse exotic master strong")
            .split(SEED_PHRASE_WORDS_DELIMITER)
    )
    val phraseFlow: Flow<List<String>> = mutablePhraseFlow
    val phraseString: String
        get() = mutablePhraseFlow.value.joinToString(" ")

    private val mutableStateFlow: MutableStateFlow<State> = MutableStateFlow(State.Hidden)
    val stateFlow: Flow<State> = mutableStateFlow
    val state: State
        get() = mutableStateFlow.value
    val isBackupConfirmationVisible: Boolean
        get() = App.appCore.session.walletStorage.setupPreferences.getRequireSeedPhraseBackupConfirmation()
    val isConsentCheckBoxEnabledFlow: Flow<Boolean> =
        stateFlow.map { state ->
            state is State.Revealed
        }
    private val _isContinueButtonEnabledFlow: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val isContinueButtonEnabledFlow: Flow<Boolean> =
        _isContinueButtonEnabledFlow

    private val mutableEventsFlow =
        MutableSharedFlow<Event>(extraBufferCapacity = 10)
    val eventsFlow: Flow<Event> = mutableEventsFlow

    fun onShowPhraseClicked() {
        mutableEventsFlow.tryEmit(Event.Authenticate)
    }

    fun onAuthenticated(password: String) {
        decryptAndRevealPhrase(password)
    }

    private fun decryptAndRevealPhrase(password: String) = viewModelScope.launch(Dispatchers.IO) {
        val seedPhrase = try {
            App.appCore.session.walletStorage.setupPreferences.getSeedPhrase(password)
        } catch (e: Exception) {
            Log.e("phrase_decrypt_failed", e)

            mutableEventsFlow.emit(Event.ShowFatalError)
            return@launch
        }

        mutablePhraseFlow.emit(seedPhrase.split(SEED_PHRASE_WORDS_DELIMITER))
        mutableStateFlow.emit(State.Revealed)
    }

    fun onConsentCheckboxClicked(isChecked: Boolean) {
        _isContinueButtonEnabledFlow.tryEmit(isChecked)
    }

    fun onContinueClicked() {
        App.appCore.session.walletStorage.setupPreferences.setRequireSeedPhraseBackupConfirmation(false)
        mutableEventsFlow.tryEmit(Event.Finish)
    }

    sealed interface State {
        object Hidden : State
        object Revealed : State
    }

    sealed interface Event {
        object Authenticate : Event
        object ShowFatalError : Event
        object Finish : Event
    }

    private companion object {
        private const val SEED_PHRASE_WORDS_DELIMITER = ' '
    }
}
