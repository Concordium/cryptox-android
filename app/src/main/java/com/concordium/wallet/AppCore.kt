package com.concordium.wallet

import android.content.Context
import com.concordium.wallet.core.authentication.AuthenticationManager
import com.concordium.wallet.core.authentication.Session
import com.concordium.wallet.core.crypto.CryptoLibrary
import com.concordium.wallet.core.crypto.CryptoLibraryReal
import com.concordium.wallet.core.gson.BigIntegerTypeAdapter
import com.concordium.wallet.core.gson.RawJsonTypeAdapter
import com.concordium.wallet.core.tracking.AppTracker
import com.concordium.wallet.core.tracking.MatomoAppTracker
import com.concordium.wallet.core.tracking.NoOpAppTracker
import com.concordium.wallet.data.backend.ProxyBackend
import com.concordium.wallet.data.backend.ProxyBackendConfig
import com.concordium.wallet.data.backend.airdrop.AirDropBackend
import com.concordium.wallet.data.backend.airdrop.AirDropBackendConfig
import com.concordium.wallet.data.backend.news.NewsfeedRssBackend
import com.concordium.wallet.data.backend.news.NewsfeedRssBackendConfig
import com.concordium.wallet.data.backend.tokens.TokensBackend
import com.concordium.wallet.data.backend.tokens.TokensBackendConfig
import com.concordium.wallet.data.model.RawJson
import com.concordium.wallet.data.preferences.TrackingPreferences
import com.concordium.wallet.data.room.Identity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.matomo.sdk.Matomo
import org.matomo.sdk.TrackerBuilder
import java.math.BigInteger

class AppCore(val context: Context) {

    val gson: Gson = initializeGson()
    val proxyBackendConfig = ProxyBackendConfig(gson)
    private val tokenBackendConfig = TokensBackendConfig(gson)
    private val airdropBackendConfig = AirDropBackendConfig(gson)
    private val newsfeedRssBackendConfig: NewsfeedRssBackendConfig by lazy(::NewsfeedRssBackendConfig)
    val cryptoLibrary: CryptoLibrary = CryptoLibraryReal(gson)

    private val trackingPreferences = TrackingPreferences(context)
    private val noOpAppTracker: AppTracker = NoOpAppTracker()
    private val matomoAppTracker: AppTracker by lazy {
        TrackerBuilder.createDefault("https://concordium.matomo.cloud/matomo.php", 8)
            .build(Matomo.getInstance(context))
            .let(::MatomoAppTracker)
    }
    val tracker: AppTracker
        get() =
            if (trackingPreferences.isTrackingEnabled)
                matomoAppTracker
            else
                noOpAppTracker

    val session: Session = Session(App.appContext)
    var newIdentities = mutableMapOf<Int, Identity>()

    private val authenticationManagerGeneric: AuthenticationManager =
        AuthenticationManager(session.getBiometricAuthKeyName())
    private var authenticationManagerReset: AuthenticationManager = authenticationManagerGeneric
    private var authenticationManager: AuthenticationManager = authenticationManagerGeneric
    private var resetBiometricKeyNameAppendix: String = ""

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

    fun getOriginalAuthenticationManager(): AuthenticationManager {
        return authenticationManagerReset
    }

    fun getCurrentAuthenticationManager(): AuthenticationManager {
        return authenticationManager
    }

    fun startResetAuthFlow() {
        resetBiometricKeyNameAppendix = System.currentTimeMillis().toString()
        authenticationManagerReset = AuthenticationManager(resetBiometricKeyNameAppendix)
        authenticationManager = authenticationManagerReset
    }

    fun finalizeResetAuthFlow() {
        session.setBiometricAuthKeyName(resetBiometricKeyNameAppendix)
        session.hasFinishedSetupPassword()
    }

    fun cancelResetAuthFlow() {
        authenticationManagerReset = authenticationManagerGeneric
        authenticationManager = authenticationManagerReset
        session.hasFinishedSetupPassword()
    }
}
