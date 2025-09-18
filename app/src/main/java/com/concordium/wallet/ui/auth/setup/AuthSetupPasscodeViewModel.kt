package com.concordium.wallet.ui.auth.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.ui.seed.setup.GenerateSeedPhraseUseCase
import com.concordium.wallet.ui.seed.setup.SetUpSeedPhraseWalletUseCase
import com.concordium.wallet.util.BiometricsUtil
import com.concordium.wallet.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AuthSetupPasscodeViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableStateFlow = MutableStateFlow<State>(
        State.Create(hasError = false)
    )
    val stateFlow: Flow<State> = mutableStateFlow
    val state: State
        get() = mutableStateFlow.value

    private val mutableEventsFlow = MutableSharedFlow<Event>(extraBufferCapacity = 10)
    val eventsFlow: Flow<Event> = mutableEventsFlow

    val passcodeLength = 6
    private var createdPasscode: String? = null
    var isSeedPhraseWalletSetupRequired = false

    fun onPasscodeEntered(passcode: String) = viewModelScope.launch {
        require(passcode.length == passcodeLength) {
            "The entered passcode doesn't have the required length"
        }

        when (state) {
            is State.Create -> {
                App.appCore.tracker.passcodeSetupEntered()
                createdPasscode = passcode
                mutableStateFlow.tryEmit(State.Repeat)
            }

            State.Repeat -> {
                if (passcode == createdPasscode) {
                    App.appCore.tracker.passcodeSetupConfirmationEntered()
                    proceedWithConfirmedCreatedPasscode()
                } else {
                    mutableStateFlow.tryEmit(
                        State.Create(hasError = true)
                    )
                }
            }
        }
    }

    private suspend fun proceedWithConfirmedCreatedPasscode() {
        val confirmedPasscode = checkNotNull(createdPasscode) {
            "The passcode must be created at this point"
        }

        App.appCore.setup.beginAuthSetup(confirmedPasscode)

        val isSetUpSuccessfully = runCatching {
            val authResetMasterKey = App.appCore.setup.authResetMasterKey
            if (authResetMasterKey != null) {
                App.appCore.auth.initPasswordAuth(
                    password = confirmedPasscode,
                    isPasscode = true,
                    masterKey = authResetMasterKey,
                )
            } else {
                App.appCore.auth.initPasswordAuth(
                    password = confirmedPasscode,
                    isPasscode = true,
                )
            }
        }.isSuccess

        if (isSetUpSuccessfully) {
            onSetUpSuccessfully()
        } else {
            App.appCore.setup.finishAuthSetup()
            mutableEventsFlow.tryEmit(Event.ShowFatalError)
        }
    }

    private suspend fun onSetUpSuccessfully() {
        if (isSeedPhraseWalletSetupRequired) {
            trySettingUpSeedPhraseWallet()
        }

        if (BiometricsUtil.isBiometricsAvailable()) {
            mutableEventsFlow.tryEmit(Event.SuggestBiometricsSetup)
        } else {
            App.appCore.setup.finishAuthSetup()
            mutableEventsFlow.tryEmit(Event.FinishWithSuccess)
        }
    }

    /**
     * Sets up a seed phrase wallet while there is the password in the memory.
     * If for any reason this fails, there's a fallback way to explicitly set up a phrase
     * through the onboarding flow on the main screen.
     */
    private suspend fun trySettingUpSeedPhraseWallet() {

        check(!App.appCore.session.walletStorage.setupPreferences.hasEncryptedSeed()) {
            "Seed phrase wallet setup required, yet there already is an encrypted seed " +
                    "which is not expected"
        }

        val phraseString =
            GenerateSeedPhraseUseCase()
                .invoke()
                .joinToString(
                    separator = " ",
                )

        Log.d(
            "phrase_generated:" +
                    "\nphrase=$phraseString"
        )

        val isSetUpSuccessfully = SetUpSeedPhraseWalletUseCase()
            .invoke(
                seedPhraseString = phraseString,
                password = checkNotNull(App.appCore.setup.authSetupPassword) {
                    "The set up password must be available at this moment"
                },
            )

        if (isSetUpSuccessfully) {
            App.appCore.setup.finishInitialSetup()
        }
    }

    fun onBiometricsSuggestionReviewed() {
        App.appCore.setup.finishAuthSetup()
        mutableEventsFlow.tryEmit(Event.FinishWithSuccess)
    }

    fun onUseFullPasswordClicked() {
        mutableEventsFlow.tryEmit(Event.OpenFullPasswordSetUp)
    }

    fun onFullPasswordSetUpResult(isSetUpSuccessfully: Boolean) {
        mutableStateFlow.tryEmit(
            State.Create(hasError = false)
        )

        if (isSetUpSuccessfully) {
            viewModelScope.launch {
                onSetUpSuccessfully()
            }
        } else {
            App.appCore.setup.finishAuthSetup()
            mutableEventsFlow.tryEmit(Event.ShowFatalError)
        }
    }

    sealed interface State {
        class Create(val hasError: Boolean) : State
        object Repeat : State
    }

    sealed interface Event {
        object SuggestBiometricsSetup : Event
        object ShowFatalError : Event
        object FinishWithSuccess : Event

        /**
         * Call [onFullPasswordSetUpResult] on result.
         */
        object OpenFullPasswordSetUp : Event
    }
}
