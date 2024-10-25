package com.concordium.wallet.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.concordium.wallet.data.room.Identity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class OnboardingSharedViewModel(application: Application) : AndroidViewModel(application) {

    private val _identityFlow = MutableSharedFlow<Identity>()
    val identityFlow = _identityFlow.asSharedFlow()

    suspend fun setIdentity(identity: Identity) {
        _identityFlow.emit(identity)
    }
}