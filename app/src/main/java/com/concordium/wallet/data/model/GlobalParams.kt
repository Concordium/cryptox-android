package com.concordium.wallet.data.model

import com.concordium.sdk.crypto.bulletproof.BulletproofGenerators
import com.concordium.sdk.crypto.pedersencommitment.PedersenCommitmentKey
import com.concordium.sdk.responses.cryptographicparameters.CryptographicParameters
import java.io.Serializable

data class GlobalParams(
    val onChainCommitmentKey: String,
    val bulletproofGenerators: String,
    val genesisString: String,
) : Serializable {

    fun toSdkCryptographicParameters(): CryptographicParameters =
        CryptographicParameters.builder()
            .genesisString(genesisString)
            .bulletproofGenerators(BulletproofGenerators.from(bulletproofGenerators))
            .onChainCommitmentKey(PedersenCommitmentKey.from(onChainCommitmentKey))
            .build()
}
