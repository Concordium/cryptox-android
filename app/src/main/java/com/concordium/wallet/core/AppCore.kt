package com.concordium.wallet.core

import android.os.Handler
import com.concordium.wallet.App
import com.concordium.wallet.core.crypto.CryptoLibrary
import com.concordium.wallet.core.crypto.CryptoLibraryReal
import com.concordium.wallet.core.gson.BigIntegerTypeAdapter
import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.core.migration.NetworkSwitchMigration
import com.concordium.wallet.core.migration.TwoWalletsMigration
import com.concordium.wallet.core.multinetwork.AppNetwork
import com.concordium.wallet.core.multiwallet.AppWallet
import com.concordium.wallet.core.tracking.AppTracker
import com.concordium.wallet.core.tracking.NoOpAppTracker
import com.concordium.wallet.data.AppNetworkRepository
import com.concordium.wallet.data.AppWalletRepository
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

        with(NetworkSwitchMigration(app)) {
            runBlocking {
                if (isAppDatabaseMigrationNeeded()) {
                    migrateAppDatabaseOnce()
                }
            }
        }
    }

    val database = AppDatabase.getDatabase(app)
    val walletRepository = AppWalletRepository(database.appWalletDao())
    val networkRepository = AppNetworkRepository(database.appNetworkDao())

    var session: Session =
        runBlocking {
            Session(
                context = app,
                gson = gson,
                activeWallet = walletRepository.getActiveWallet(),
                network = networkRepository.getActiveNetwork(),
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

    suspend fun startNewSession(
        activeWallet: AppWallet = session.activeWallet,
        network: AppNetwork = session.network,
        isLoggedIn: Boolean = session.isLoggedIn.value == true,
    ) = suspendCoroutine { continuation ->
        // Session must be created in the main thread as it contains logout timer.
        Handler(app.mainLooper).post {
            continuation.context.ensureActive()

            session.inactivityCountDownTimer.cancel()
            session = Session(
                context = app,
                gson = gson,
                activeWallet = activeWallet,
                network = network,
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
