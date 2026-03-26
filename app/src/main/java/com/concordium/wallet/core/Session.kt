package com.concordium.wallet.core

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.core.multinetwork.AppNetwork
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.data.WalletStorage
import com.concordium.wallet.data.backend.AppBackends
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.util.Log
import com.google.gson.Gson

class Session(
    context: Context,
    gson: Gson,
    val activeWallet: AppWallet,
    val network: AppNetwork,
    isLoggedIn: Boolean = false,
) {
    val backends = AppBackends(
        gson = gson,
        network = network,
    )
    val walletStorage = WalletStorage(
        wallet = activeWallet,
        network = network,
        context = context,
    )
    var newIdentities = mutableMapOf<Int, Identity>()

    init {
        Log.d(
            "Session initialized:" +
                    "\nactiveWallet=$activeWallet," +
                    "\nnetwork=$network"
        )
    }

    private val _isLoggedIn = MutableLiveData(isLoggedIn)
    val isLoggedIn: LiveData<Boolean>
        get() = _isLoggedIn

    // The notice must be shown once per app start.
    private var isUnshieldingNoticeShown = false

    fun setHasShowRewards(id: Int, value: Boolean) {
        walletStorage.filterPreferences.setHasShowRewards(id, value)
    }

    fun getHasShowRewards(id: Int): Boolean {
        return walletStorage.filterPreferences.getHasShowRewards(id)
    }

    fun setHasShowFinalizationRewards(id: Int, value: Boolean) {
        walletStorage.filterPreferences.setHasShowFinalizationRewards(id, value)
    }

    fun getHasShowFinalizationRewards(id: Int): Boolean {
        return walletStorage.filterPreferences.getHasShowFinalizationRewards(id)
    }

    fun setUnshieldingNoticeShown() {
        isUnshieldingNoticeShown = true
    }

    fun isUnshieldingNoticeShown(): Boolean =
        isUnshieldingNoticeShown

    fun setUserLoggedIn() {
        _isLoggedIn.value = true
        resetLogoutTimeout()
    }

    fun resetLogoutTimeout() {
        if (_isLoggedIn.value!!) {
            inactivityCountDownTimer.cancel()
            inactivityCountDownTimer.start()
        }
    }

    var inactivityCountDownTimer =
        object : CountDownTimer(60 * 5 * 1000.toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {}

            override fun onFinish() {
                _isLoggedIn.value = false
            }
        }

    fun isAccountsBackupPossible(): Boolean {
        return activeWallet.type == AppWallet.Type.FILE
    }

    fun isOpeningExplorerPossible(): Boolean {
        return network.ccdScanFrontendUrl != null
    }

    fun isTransactionLogsExportPossible(): Boolean {
        return network.ccdScanBackendUrl != null
    }
}
