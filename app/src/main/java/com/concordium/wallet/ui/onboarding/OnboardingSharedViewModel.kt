package com.concordium.wallet.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.concordium.wallet.data.room.Identity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingSharedViewModel(application: Application) : AndroidViewModel(application) {

    private val _updateState = MutableStateFlow(false)
    val updateState = _updateState.asStateFlow()

    private val _identityFlow = MutableSharedFlow<Identity>()
    val identityFlow = _identityFlow.asSharedFlow()

    suspend fun setUpdateState(update: Boolean) = _updateState.emit(update)
    suspend fun setIdentity(identity: Identity) = _identityFlow.emit(identity)
}