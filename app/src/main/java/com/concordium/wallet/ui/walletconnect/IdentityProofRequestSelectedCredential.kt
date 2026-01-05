package com.concordium.wallet.ui.walletconnect

sealed interface IdentityProofRequestSelectedCredential {

    val identity: com.concordium.wallet.data.room.Identity

    class Account(
        val account: com.concordium.wallet.data.room.Account,
        override val identity: com.concordium.wallet.data.room.Identity,
    ) :
        IdentityProofRequestSelectedCredential

    class Identity(override val identity: com.concordium.wallet.data.room.Identity) :
        IdentityProofRequestSelectedCredential
}
