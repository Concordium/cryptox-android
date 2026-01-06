package com.concordium.wallet.data.cryptolib

import com.concordium.wallet.data.room.Identity

/**
 * Decrypted contents of [Identity.privateIdObjectDataEncrypted],
 * for file-based identities.
 */
class PrivateIdObjectData(
    val randomness: String,
    val aci: Aci,
) {
    class Aci(
        val prfKey: String,
        val credentialHolderInformation: CredentialHolderInformation,
    ) {
        class CredentialHolderInformation(
            val idCredSecret: String,
        )
    }
}
