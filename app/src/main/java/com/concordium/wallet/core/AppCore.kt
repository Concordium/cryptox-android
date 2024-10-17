package com.concordium.wallet.core

import com.concordium.wallet.App
import com.concordium.wallet.core.crypto.CryptoLibrary
import com.concordium.wallet.core.crypto.CryptoLibraryReal
import com.concordium.wallet.core.gson.BigIntegerTypeAdapter
import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.core.tracking.AppTracker
import com.concordium.wallet.core.tracking.MatomoAppTracker
import com.concordium.wallet.core.tracking.NoOpAppTracker
import com.concordium.wallet.data.AppWalletRepository
import com.concordium.wallet.data.backend.ProxyBackend
import com.concordium.wallet.data.backend.ProxyBackendConfig
import com.concordium.wallet.data.backend.airdrop.AirDropBackend
import com.concordium.wallet.data.backend.airdrop.AirDropBackendConfig
import com.concordium.wallet.data.backend.news.NewsfeedRssBackend
import com.concordium.wallet.data.backend.news.NewsfeedRssBackendConfig
import com.concordium.wallet.data.backend.notifications.NotificationsBackend
import com.concordium.wallet.data.backend.notifications.NotificationsBackendConfig
import com.concordium.wallet.data.backend.tokens.TokensBackend
import com.concordium.wallet.data.backend.tokens.TokensBackendConfig
import com.concordium.wallet.data.model.RawJson
import com.concordium.wallet.data.preferences.AppSetupPreferences
import com.concordium.wallet.data.preferences.AppTrackingPreferences
import com.concordium.wallet.data.room.app.AppDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import org.matomo.sdk.Matomo
import org.matomo.sdk.TrackerBuilder
import java.math.BigInteger

class AppCore(val app: App) {

    val gson: Gson = initializeGson()
    val proxyBackendConfig = ProxyBackendConfig(gson)
    private val tokenBackendConfig = TokensBackendConfig(gson)
    private val airdropBackendConfig = AirDropBackendConfig(gson)
    private val newsfeedRssBackendConfig: NewsfeedRssBackendConfig by lazy(::NewsfeedRssBackendConfig)
    private val notificationsBackendConfig: NotificationsBackendConfig =
        NotificationsBackendConfig(gson)
    val cryptoLibrary: CryptoLibrary = CryptoLibraryReal(gson)
    val database = AppDatabase.getDatabase(app)
    val walletRepository = AppWalletRepository(database.appWalletDao())

    private val appTrackingPreferences = AppTrackingPreferences(App.appContext)
    private val noOpAppTracker: AppTracker = NoOpAppTracker()
    private val matomoAppTracker: AppTracker by lazy {
        TrackerBuilder.createDefault("https://concordium.matomo.cloud/matomo.php", 8)
            .build(Matomo.getInstance(App.appContext))
            .let(::MatomoAppTracker)
    }
    val tracker: AppTracker
        get() =
            if (appTrackingPreferences.isTrackingEnabled)
                matomoAppTracker
            else
                noOpAppTracker

    val session = runBlocking {
        Session(
            context = app,
            activeWallet = walletRepository.getActiveWallet(),
        )
    }
    val setup = AppSetup(
        appSetupPreferences = AppSetupPreferences(App.appContext),
        session = session,
    )
    val auth: AppAuth
        get() = setup.auth

    fun getNotificationsBackend(): NotificationsBackend {
        return notificationsBackendConfig.backend
    }

    fun getNewsfeedRssBackend(): NewsfeedRssBackend {
        return newsfeedRssBackendConfig.backend
    }

    fun getTokensBackend(): TokensBackend {
        return tokenBackendConfig.backend
    }

    fun getAirdropBackend(): AirDropBackend {
        return airdropBackendConfig.backend
    }

    fun getProxyBackend(): ProxyBackend {
        return proxyBackendConfig.backend
    }

    private fun initializeGson(): Gson {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(RawJson::class.java, RawJsonTypeAdapter())
        gsonBuilder.registerTypeAdapter(BigInteger::class.java, BigIntegerTypeAdapter())
        return gsonBuilder.create()
    }
}
