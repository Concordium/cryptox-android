package com.concordium.wallet.core

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.core.multinetwork.AppNetwork
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.data.WalletStorage
import com.concordium.wallet.data.preferences.Preferences
import com.concordium.wallet.data.room.Identity
import okhttp3.HttpUrl.Companion.toHttpUrl

class Session(
    context: Context,
    val activeWallet: AppWallet,
    isLoggedIn: Boolean = false,
) {
    val network = AppNetwork(
        genesisHash = "4221332d34e1694168c2a0c0b3fd0f273809612cb13d000d5c2e00e85f50f796",
        name = "Concordium Testnet",
        walletProxyUrl = "https://wallet-proxy.testnet.concordium.com".toHttpUrl(),
        ccdScanFrontendUrl = "https://testnet.ccdscan.io".toHttpUrl(),
        ccdScanBackendUrl = "https://api-ccdscan.testnet.concordium.com/rest".toHttpUrl(),
        notificationsServiceUrl = "https://notification-api.testnet.concordium.com/api".toHttpUrl(),
        spacesevenUrl = "https://stage.spaceseven.cloud".toHttpUrl(),
    )

    val walletStorage = WalletStorage(
        activeWallet = activeWallet,
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

    fun areAccountsBackedUp(): Boolean {
        return walletStorage.setupPreferences.areAccountsBackedUp()
    }

    fun setAccountsBackedUp(value: Boolean) {
        return walletStorage.setupPreferences.setAccountsBackedUp(value)
    }

    fun addAccountsBackedUpListener(listener: Preferences.Listener) {
        walletStorage.setupPreferences.addAccountsBackedUpListener(listener)
    }

    fun removeAccountsBackedUpListener(listener: Preferences.Listener) {
        walletStorage.setupPreferences.removeListener(listener)
    }
}
