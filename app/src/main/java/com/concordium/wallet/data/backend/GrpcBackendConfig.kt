package com.concordium.wallet.data.backend

import com.concordium.sdk.ClientV2
import com.concordium.sdk.Connection
import com.concordium.sdk.TLSConfig
import com.concordium.wallet.BuildConfig

class GrpcBackendConfig {
    val client: ClientV2 by lazy {
        ClientV2.from(
            Connection.builder()
                .host(BuildConfig.HOSTNAME_GRPC)
                .port(PORT)
                .useTLS(TLSConfig.auto())
                .timeout(TIMEOUT_MS)
                .build()
        )
    }

    private companion object {
        private const val PORT = 20000
        private const val TIMEOUT_MS = 30_000
    }
}
