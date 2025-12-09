package com.concordium.wallet.ui.walletconnect

import com.concordium.sdk.crypto.wallet.web3Id.Statement.AtomicStatement

/**
 * Some statements about identity which can be proven
 * by either account, identity, or both.
 */
data class IdentityProofRequestClaims(
    val statements: List<AtomicStatement>,
    val selectedCredential: IdentityProofRequestSelectedCredential,
)
