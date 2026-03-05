package com.concordium.wallet.core.multinetwork

import com.concordium.sdk.crypto.wallet.Network
import com.concordium.wallet.BuildConfig
import okhttp3.HttpUrl
import java.util.Date

class AppNetwork(
    val genesisHash: String,
    val name: String,
    val createdAt: Date,
    val walletProxyUrl: HttpUrl,
    val ccdScanFrontendUrl: HttpUrl?,
    val ccdScanBackendUrl: HttpUrl?,
    val notificationsServiceUrl: HttpUrl?,
) {
    val isMainnet: Boolean
        get() = genesisHash == "9dd9ca4d19e9393877d2c44b70f89acbfc0883c2243e5eeaecc0d1cd0503f478"

    val isTestnet: Boolean
        get() = genesisHash == "4221332d34e1694168c2a0c0b3fd0f273809612cb13d000d5c2e00e85f50f796"

    val isStagenet: Boolean
        get() = genesisHash == "853288fa5a45554d3cbbf8a756b85abcbfddf28e752b13223eb747209a4d0d3c"

    val hdWalletNetwork: Network
        get() =
            // For the HD wallet, everything that's not the Mainnet is Testnet.
            if (isMainnet)
                Network.MAINNET
            else
                Network.TESTNET

    val isPermanent: Boolean
        get() = isMainnet || isTestnet

    val isDefault: Boolean
        get() = genesisHash == BuildConfig.DEFAULT_NETWORK_GENESIS_HASH

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppNetwork) return false

        if (genesisHash != other.genesisHash) return false

        return true
    }

    override fun hashCode(): Int {
        return genesisHash.hashCode()
    }

    override fun toString(): String {
        return "AppNetwork(name='$name', genesisHash='$genesisHash')"
    }
}
