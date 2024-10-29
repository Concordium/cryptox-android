package com.concordium.wallet.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.concordium.wallet.data.room.Identity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingSharedViewModel(application: Application) : AndroidViewModel(application) {

    private val _identityFlow = MutableSharedFlow<Identity>()
    val identityFlow = _identityFlow.asSharedFlow()

    private val _animatedButtonFlow = MutableStateFlow(false)
    val animatedButtonFlow = _animatedButtonFlow.asStateFlow()

    suspend fun setIdentity(identity: Identity) {
        _identityFlow.emit(identity)
    }

    suspend fun setAnimatedButton(isAnimated: Boolean) {
        _animatedButtonFlow.emit(isAnimated)
    }
}