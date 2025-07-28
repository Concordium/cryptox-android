package com.concordium.wallet.core

import android.os.Handler
import com.concordium.sdk.ClientV2
import com.concordium.wallet.App
import com.concordium.wallet.core.crypto.CryptoLibrary
import com.concordium.wallet.core.crypto.CryptoLibraryReal
import com.concordium.wallet.core.gson.BigIntegerTypeAdapter
import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.core.migration.TwoWalletsMigration
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.core.tracking.AppTracker
import com.concordium.wallet.core.tracking.NoOpAppTracker
import com.concordium.wallet.data.AppWalletRepository
import com.concordium.wallet.data.backend.GrpcBackendConfig
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
import com.concordium.wallet.data.backend.wert.WertBackend
import com.concordium.wallet.data.backend.wert.WertBackendConfig
import com.concordium.wallet.data.model.RawJson
import com.concordium.wallet.data.preferences.AppSetupPreferences
import com.concordium.wallet.data.preferences.AppTrackingPreferences
import com.concordium.wallet.data.room.app.AppDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import java.math.BigInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AppCore(val app: App) {

    val gson: Gson = getGson()
    val proxyBackendConfig = ProxyBackendConfig(gson)
    private val grpcBackendConfig = GrpcBackendConfig()
    private val tokenBackendConfig = TokensBackendConfig(gson)
    private val airdropBackendConfig = AirDropBackendConfig(gson)
    private val newsfeedRssBackendConfig: NewsfeedRssBackendConfig by lazy(::NewsfeedRssBackendConfig)
    private val notificationsBackendConfig: NotificationsBackendConfig =
        NotificationsBackendConfig(gson)
    private val wertBackendConfig = WertBackendConfig(gson)
    val cryptoLibrary: CryptoLibrary = CryptoLibraryReal(gson)
    val appTrackingPreferences = AppTrackingPreferences(App.appContext)
    private val noOpAppTracker: AppTracker = NoOpAppTracker()

    val tracker: AppTracker
        get() = noOpAppTracker

    // Migrations are invoked once vital core components are initialized:
    // Gson, crypto lib, etc.
    init {
        with(TwoWalletsMigration(app, gson)) {
            if (isPreferenceMigrationNeeded()) {
                migratePreferencesOnce()
            }

            runBlocking {
                if (isAppDatabaseMigrationNeeded()) {
                    migrateAppDatabaseOnce()
                }
            }
        }
    }

    val database = AppDatabase.getDatabase(app)
    val walletRepository = AppWalletRepository(database.appWalletDao())

    var session: Session =
        runBlocking {
            Session(
                context = app,
                activeWallet = walletRepository.getActiveWallet(),
            )
        }
        private set

    val setup = AppSetup(
        appSetupPreferences = AppSetupPreferences(
            context = App.appContext,
            gson = gson,
        ),
        getSession = { session },
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

    fun getWertBackend(): WertBackend {
        return wertBackendConfig.backend
    }

    @Deprecated("It's better to use ProxyBackend, as it is backed by a reliable node")
    fun getGrpcClient(): ClientV2 {
        return grpcBackendConfig.client
    }

    suspend fun startNewSession(
        activeWallet: AppWallet,
        isLoggedIn: Boolean = session.isLoggedIn.value == true,
    ) = suspendCoroutine { continuation ->
        // Session must be created in the main thread as it contains logout timer.
        Handler(app.mainLooper).post {
            continuation.context.ensureActive()

            session.inactivityCountDownTimer.cancel()
            session = Session(
                context = app,
                activeWallet = activeWallet,
                isLoggedIn = isLoggedIn,
            )

            continuation.resume(Unit)
        }
    }

    companion object {
        fun getGson(): Gson {
            val gsonBuilder = GsonBuilder()
            gsonBuilder.registerTypeAdapter(RawJson::class.java, RawJsonTypeAdapter())
            gsonBuilder.registerTypeAdapter(BigInteger::class.java, BigIntegerTypeAdapter())
            return gsonBuilder.create()
        }
    }
}
