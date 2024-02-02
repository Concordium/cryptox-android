package com.concordium.wallet

import android.app.Application
import android.content.Context
import com.concordium.wallet.data.backend.ws.WsCreds
import com.concordium.wallet.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.android.relay.ConnectionType
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient

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
        initFirebase()
    }

    fun initAppCore() {
        appCore = AppCore(this.applicationContext)
    }

    private fun initWalletConnect() {
        println("LC -> CALL INIT")

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
            application = this,
            metaData = appMetaData
        )

        SignClient.initialize(Sign.Params.Init(core = CoreClient)) { modelError ->
            println("LC -> INIT ERROR ${modelError.throwable.stackTraceToString()}")
        }
    }

    private fun initFirebase() {
        with(FirebaseCrashlytics.getInstance()) {
            setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            if (BuildConfig.DEBUG) {
                deleteUnsentReports()
            }
        }

        with(FirebaseAnalytics.getInstance(this)) {
            setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)
        }
    }
}
