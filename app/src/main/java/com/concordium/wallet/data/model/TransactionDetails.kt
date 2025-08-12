package com.concordium.wallet.data.model

import java.io.Serializable

class TransactionDetails(
    /**
     * Known transaction type or null if it is unknown.
     */
    val type: TransactionType?,
    val description: String,
    val outcome: TransactionOutcome,
    val rejectReason: String?,
    val events: List<String>?,
    val transferSource: String?,
    val transferDestination: String?,
    /**
     * CBOR-encoded memo as a hex string.
     */
    val memo: String?,
    val tokenId: String?,
    val tokenTransferAmount: TokenAmount?,
) : Serializable
