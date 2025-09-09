package com.concordium.wallet.ui.account.earn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.core.arch.Event
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.ChainParameters
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.Log
import kotlinx.coroutines.launch

class EarnInfoViewModel(
    private val proxyRepository: ProxyRepository,
    application: Application
) : AndroidViewModel(application) {

    val chainParameters: MutableLiveData<ChainParameters> by lazy { MutableLiveData<ChainParameters>() }
    val error: MutableLiveData<Event<Int>> by lazy { MutableLiveData<Event<Int>>() }

    fun loadChainParameters() = viewModelScope.launch {
        try {
            chainParameters.value = proxyRepository.getChainParameters()
        } catch (e: Exception) {
            handleBackendError(e)
        }
    }

    private fun handleBackendError(throwable: Throwable) {
        Log.e("Backend request failed", throwable)
        error.value = Event(BackendErrorHandler.getExceptionStringRes(throwable))
    }
}
