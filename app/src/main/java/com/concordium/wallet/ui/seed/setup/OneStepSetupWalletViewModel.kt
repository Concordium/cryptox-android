package com.concordium.wallet.ui.seed.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OneStepSetupWalletViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val mutablePhraseFlow = MutableStateFlow<List<String>?>(savedStateHandle[PHRASE_KEY])
    val phraseFlow: Flow<List<String>?> = mutablePhraseFlow.asStateFlow()
    val phraseString: String?
        get() = mutablePhraseFlow.value?.joinToString(" ")

    private val mutableEventsFlow = MutableSharedFlow<Event>(extraBufferCapacity = 10)
    val eventsFlow: Flow<Event> = mutableEventsFlow

    init {
        if (mutablePhraseFlow.value.isNullOrEmpty())
            generatePhrase()
    }

    private fun generatePhrase() = viewModelScope.launch {
        val phrase = GenerateSeedPhraseUseCase().invoke()

        if (BuildConfig.DEBUG) {
            Log.d(
                "phrase_generated:" +
                        "\nphrase=${phrase.joinToString(" ")}"
            )
        }

        mutablePhraseFlow.emit(phrase)
        savedStateHandle[PHRASE_KEY] = phrase
    }

    fun onContinueClicked() {
        mutableEventsFlow.tryEmit(Event.Authenticate)
    }

    fun onAuthenticated(password: String) {
        setUpPhrase(password)
    }

    private fun setUpPhrase(password: String) = viewModelScope.launch(Dispatchers.IO) {

        val isSetUpSuccessfully =
            SetUpSeedPhraseWalletUseCase()
                .invoke(
                    seedPhraseString = checkNotNull(phraseString) {
                        "The phrase must be generated at this moment"
                    },
                    password = password,
                )

        if (isSetUpSuccessfully) {
            App.appCore.setup.finishInitialSetup()
            App.appCore.session.walletStorage.setupPreferences
                .setRequireSeedPhraseBackupConfirmation(false)
            mutableEventsFlow.emit(Event.GoToAccountOverview)
        } else {
            mutableEventsFlow.emit(Event.ShowFatalError)
        }
    }

    sealed interface Event {
        object Authenticate : Event
        object ShowFatalError : Event
        object GoToAccountOverview : Event
    }

    private companion object {
        private const val PHRASE_KEY = "PHRASE_KEY"
    }
}
