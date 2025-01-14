package com.concordium.wallet

import android.app.Application
import android.content.Context
import com.concordium.wallet.core.AppCore
import com.concordium.wallet.core.notifications.AnnouncementNotificationManager
import com.concordium.wallet.data.backend.ws.WsCreds
import com.concordium.wallet.util.Log
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.android.relay.NetworkClientTimeout
import com.reown.sign.client.Sign
import com.reown.sign.client.SignClient
import java.util.concurrent.TimeUnit

class App : Application() {
    companion object {
        lateinit var appContext: Context
        lateinit var appCore: AppCore
        var wsCreds: WsCreds? = null
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("App starting - setting Log silent if release")
        Log.setSilent(!BuildConfig.DEBUG)
        Log.d("Log is not silent")

        initialize()
    }

    private fun initialize() {
        appContext = this
        initAppCore()
        initWalletConnect()
        appCore.tracker.installation(this)
        AnnouncementNotificationManager(this).ensureChannel()
    }

    fun initAppCore() {
        appCore = AppCore(this@App)
    }

    private fun initWalletConnect() {
        println("WalletConnect -> CALL INIT")

        // Account - oleg.koretsky, project – CryptoX Android
        val projectId = "f6dea1cab6223d05f64c0c418527368b"
        val relayServerUrl = "wss://relay.walletconnect.com?projectId=$projectId"
        val appMetaData = Core.Model.AppMetaData(
            name = getString(R.string.app_name),
            description = "CryptoX - Blockchain Wallet",
            url = "https://play.google.com/store/apps/details?id=com.pioneeringtechventures.wallet",
            icons = listOf(),
            redirect = "${getString(R.string.wc_scheme)}://r"
        )

        CoreClient.initialize(
            relayServerUrl = relayServerUrl,
            connectionType = ConnectionType.AUTOMATIC,
            networkClientTimeout = NetworkClientTimeout(40, TimeUnit.SECONDS),
            application = this,
            metaData = appMetaData,
            telemetryEnabled = false,
            onError = { error ->
                println("WalletConnect -> CORE ERROR ${error.throwable.stackTraceToString()}")
            }
        )

        SignClient.initialize(
            init = Sign.Params.Init(core = CoreClient),
            onError = { error ->
                println("WalletConnect -> SIGN ERROR ${error.throwable.stackTraceToString()}")
            }
        )
    }
}
