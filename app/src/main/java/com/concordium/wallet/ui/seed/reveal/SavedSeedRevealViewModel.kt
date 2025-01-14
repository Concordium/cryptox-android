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
import kotlinx.coroutines.launch

class SavedSeedRevealViewModel(application: Application) : AndroidViewModel(application) {
    // The initial value is a random seed to be shown under a blur.
    private val mutableSeedFlow: MutableStateFlow<String> = MutableStateFlow(
        "575f0f919c99ed4b7d858df2aea68112292da4eae98e2e69410cd5283f3c727b282caeba754f815dc876d8b84d3339c6f74c4127f238a391891dd23c74892943"
    )
    val seedFlow: Flow<String> = mutableSeedFlow
    val seedString: String
        get() = mutableSeedFlow.value

    private val mutableStateFlow: MutableStateFlow<State> = MutableStateFlow(State.Hidden)
    val stateFlow: Flow<State> = mutableStateFlow
    val state: State
        get() = mutableStateFlow.value

    private val mutableEventsFlow =
        MutableSharedFlow<Event>(extraBufferCapacity = 10)
    val eventsFlow: Flow<Event> = mutableEventsFlow

    fun onShowSeedClicked() {
        mutableEventsFlow.tryEmit(Event.Authenticate)
    }

    fun onAuthenticated(password: String) {
        decryptAndRevealSeed(password)
    }

    private fun decryptAndRevealSeed(password: String) = viewModelScope.launch(Dispatchers.IO) {
        val seedHex = try {
            App.appCore.session.walletStorage.setupPreferences.getSeedHex(password)
        } catch (e: Exception) {
            Log.e("seed_decrypt_failed", e)

            mutableEventsFlow.emit(Event.ShowFatalError)
            return@launch
        }

        mutableSeedFlow.emit(seedHex)
        mutableStateFlow.emit(State.Revealed)
    }

    sealed interface State {
        object Hidden : State
        object Revealed : State
    }

    sealed interface Event {
        object Authenticate : Event
        object ShowFatalError : Event
    }
}
