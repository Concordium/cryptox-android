package com.concordium.wallet.ui.auth.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.authentication.Session
import com.concordium.wallet.util.BiometricsUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AuthSetupPasscodeViewModel(application: Application) : AndroidViewModel(application) {
    private val session: Session = App.appCore.session

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

    init {
        App.appCore.tracker.welcomePasscodeScreen()
    }

    fun onPasscodeEntered(passcode: String) = viewModelScope.launch {
        require(passcode.length == passcodeLength) {
            "The entered passcode doesn't have the required length"
        }

        when (state) {
            is State.Create -> {
                App.appCore.tracker.welcomePasscodeEntered()
                createdPasscode = passcode
                mutableStateFlow.tryEmit(State.Repeat)
            }

            State.Repeat -> {
                if (passcode == createdPasscode) {
                    App.appCore.tracker.welcomePasscodeConfirmationEntered()
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

        session.startPasswordSetup(confirmedPasscode)

        val isSetUpSuccessfully = runCatching {
            val authResetMasterKey = App.appCore.authResetMasterKey
            if (authResetMasterKey != null) {
                App.appCore.authManager.initPasswordAuth(
                    password = confirmedPasscode,
                    masterKey = authResetMasterKey,
                )
            } else {
                App.appCore.authManager.initPasswordAuth(
                    password = confirmedPasscode,
                )
            }
        }.isSuccess

        if (isSetUpSuccessfully) {
            onSetUpSuccessfully(usedPasscode = true)
        } else {
            session.hasFinishedSetupPassword()
            mutableEventsFlow.tryEmit(Event.ShowFatalError)
        }
    }

    private fun onSetUpSuccessfully(usedPasscode: Boolean) {
        // Setting up password is done, so login screen should be shown next time app is opened
        session.hasSetupPassword(usedPasscode)

        if (BiometricsUtil.isBiometricsAvailable()) {
            mutableEventsFlow.tryEmit(Event.SuggestBiometricsSetup)
        } else {
            session.hasFinishedSetupPassword()
            mutableEventsFlow.tryEmit(Event.FinishWithSuccess)
        }
    }

    fun onBiometricsSuggestionReviewed() {
        session.hasFinishedSetupPassword()
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
            onSetUpSuccessfully(usedPasscode = false)
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
