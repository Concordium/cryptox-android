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
import com.google.gson.Gson
import okhttp3.HttpUrl.Companion.toHttpUrl

class Session(
    context: Context,
    gson: Gson,
    val activeWallet: AppWallet,
    isLoggedIn: Boolean = false,
) {
    val network = AppNetwork(
        genesisHash = "4221332d34e1694168c2a0c0b3fd0f273809612cb13d000d5c2e00e85f50f796",
        name = "Testnet",
        walletProxyUrl = "https://wallet-proxy.testnet.concordium.com/".toHttpUrl(),
        ccdScanFrontendUrl = "https://testnet.ccdscan.io/".toHttpUrl(),
        ccdScanBackendUrl = "https://api-ccdscan.testnet.concordium.com/rest/".toHttpUrl(),
        notificationsServiceUrl = "https://notification-api.testnet.concordium.com/api/".toHttpUrl(),
        spacesevenUrl = "https://stage.spaceseven.cloud/".toHttpUrl(),
    )
//    val network = AppNetwork(
//        genesisHash = "9dd9ca4d19e9393877d2c44b70f89acbfc0883c2243e5eeaecc0d1cd0503f478",
//        name = "Mainnet",
//        walletProxyUrl = "https://wallet-proxy.mainnet.concordium.software/".toHttpUrl(),
//        ccdScanFrontendUrl = "https://ccdscan.io/".toHttpUrl(),
//        ccdScanBackendUrl = "https://api-ccdscan.mainnet.concordium.software/rest/".toHttpUrl(),
//        notificationsServiceUrl = "https://notification-api.mainnet.concordium.software/api/".toHttpUrl(),
//        spacesevenUrl = "https://spaceseven.com/".toHttpUrl(),
//    )
//    val network = AppNetwork(
//        genesisHash = "38bf770b4c247f09e1b62982bb71000c516480c5a2c5214dadac6da4b1ad50e5",
//        name = "Stagenet",
//        walletProxyUrl = "https://wallet-proxy.stagenet.concordium.com/".toHttpUrl(),
//        ccdScanFrontendUrl = "https://stagenet.ccdscan.io/".toHttpUrl(),
//        ccdScanBackendUrl = null,
//        notificationsServiceUrl = null,
//        spacesevenUrl = null,
//    )
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
