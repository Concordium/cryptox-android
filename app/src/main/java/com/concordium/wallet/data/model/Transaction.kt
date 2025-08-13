package com.concordium.wallet.data.model

import java.io.Serializable
import java.math.BigInteger
import java.util.Date


class Transaction(
    val source: TransactionSource,
    val timeStamp: Date,
    val knownType: TransactionType?,
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
    val tokenSymbol: String?,
) : Serializable {

    fun isRemoteTransaction(): Boolean {
        return status == TransactionStatus.FINALIZED
    }

    fun isBakerTransfer(): Boolean {
        return knownType == TransactionType.LOCAL_BAKER
    }

    fun isDelegationTransfer(): Boolean {
        return knownType == TransactionType.LOCAL_DELEGATION
    }

    fun isEncryptedTransfer(): Boolean {
        return knownType == TransactionType.ENCRYPTEDAMOUNTTRANSFER || knownType == TransactionType.ENCRYPTEDAMOUNTTRANSFERWITHMEMO
    }

    fun isSmartContractUpdate(): Boolean {
        return knownType == TransactionType.UPDATE
    }

    fun isOriginSelf(): Boolean {
        return origin?.type == TransactionOriginType.Self
    }

    fun isBakerSuspension(): Boolean {
        return knownType == TransactionType.VALIDATOR_SUSPENDED
    }

    fun isBakerPrimingForSuspension(): Boolean {
        return knownType == TransactionType.VALIDATOR_PRIMED_FOR_SUSPENSION
    }
}
