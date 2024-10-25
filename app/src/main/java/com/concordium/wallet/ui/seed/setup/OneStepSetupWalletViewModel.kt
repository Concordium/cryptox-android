package com.concordium.wallet.ui.seed.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.bip39.Mnemonics
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.core.authentication.Session
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class OneStepSetupWalletViewModel(application: Application) : AndroidViewModel(application) {
    private val session: Session = App.appCore.session

    private val mutablePhraseFlow = MutableStateFlow<List<String>?>(null)
    val phraseFlow: Flow<List<String>?> = mutablePhraseFlow
    val phraseString: String?
        get() = mutablePhraseFlow.value?.joinToString(" ")

    private val mutableEventsFlow = MutableSharedFlow<Event>(extraBufferCapacity = 10)
    val eventsFlow: Flow<Event> = mutableEventsFlow

    init {
        App.appCore.tracker.welcomePhrase()
        generatePhrase()
    }

    private fun generatePhrase() = viewModelScope.launch(Dispatchers.IO) {
        val mnemonicCode: Mnemonics.MnemonicCode = Mnemonics.MnemonicCode(PHRASE_WORD_COUNT)
        val phrase = mnemonicCode.words.map(CharArray::concatToString)

        if (BuildConfig.DEBUG) {
            Log.d(
                "phrase_generated:" +
                        "\nphrase=${phrase.joinToString(" ")}"
            )
        }

        mutablePhraseFlow.emit(phrase)
    }

    fun onContinueClicked() {
        mutableEventsFlow.tryEmit(Event.Authenticate)
    }

    fun onAuthenticated(password: String) {
        setUpPhrase(password)
    }

    private fun setUpPhrase(password: String) = viewModelScope.launch(Dispatchers.IO) {
        val isSavedSuccessfully = AuthPreferences(getApplication())
            .tryToSetEncryptedSeedPhrase(
                seedPhraseString = checkNotNull(phraseString) {
                    "The phrase must be generated at this moment"
                },
                password = password,
            )

        if (isSavedSuccessfully) {
            mutableEventsFlow.emit(Event.GoToAccountOverview)
        } else {
            mutableEventsFlow.emit(Event.ShowFatalError)
        }
    }

    sealed interface Event {
        object Authenticate : Event
        object ShowFatalError: Event
        object GoToAccountOverview : Event
    }

    private companion object {
        private val PHRASE_WORD_COUNT = Mnemonics.WordCount.COUNT_24
    }
}
