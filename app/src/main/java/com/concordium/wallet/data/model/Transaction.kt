package com.concordium.wallet.data.model

import java.io.Serializable
import java.math.BigInteger
import java.util.Date


class Transaction(
    val source: TransactionSource,
    val timeStamp: Date,
    val type: TransactionType,
    var title: String = "",
    val subtotal: BigInteger?,
    val cost: BigInteger?,
    val total: BigInteger,
    var status: TransactionStatus,
    var outcome: TransactionOutcome,
    var blockHashes: List<String>?,
    val hash: String?,
    var rejectReason: String?,
    val events: List<String>?,
    val fromAddress: String?,
    val toAddress: String?,
    val origin: TransactionOrigin?,
    val memoText: String?,
    val tokenTransferAmount: TokenAmount?,
    val tokenId: String?,
) : Serializable {

    fun isRemoteTransaction(): Boolean {
        return status == TransactionStatus.FINALIZED
    }

    fun isBakerTransfer(): Boolean {
        return type == TransactionType.LOCAL_BAKER
    }

    fun isDelegationTransfer(): Boolean {
        return type == TransactionType.LOCAL_DELEGATION
    }

    fun isEncryptedTransfer(): Boolean {
        return type == TransactionType.ENCRYPTEDAMOUNTTRANSFER || type == TransactionType.ENCRYPTEDAMOUNTTRANSFERWITHMEMO
    }

    fun isSmartContractUpdate(): Boolean {
        return type == TransactionType.UPDATE
    }

    fun isOriginSelf(): Boolean {
        return origin?.type == TransactionOriginType.Self
    }

    fun isBakerSuspension(): Boolean {
        return type == TransactionType.VALIDATOR_SUSPENDED
    }

    fun isBakerPrimingForSuspension(): Boolean {
        return type == TransactionType.VALIDATOR_PRIMED_FOR_SUSPENSION
    }

    fun isTokenUpdate(): Boolean {
        return type == TransactionType.TOKEN_UPDATE
    }
}
