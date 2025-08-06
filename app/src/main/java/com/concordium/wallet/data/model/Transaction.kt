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
    var transactionStatus: TransactionStatus,
    var outcome: TransactionOutcome,
    var blockHashes: List<String>?,
    val hash: String?,
    var rejectReason: String?,
    val events: List<String>?,
    val fromAddress: String?,
    val toAddress: String?,
    var fromAddressTitle: String?,
    var toAddressTitle: String?,
    val origin: TransactionOrigin?,
    val memoText: String?,
) : Serializable {

    fun isRemoteTransaction(): Boolean {
        return transactionStatus == TransactionStatus.FINALIZED
    }

    fun isBakerTransfer(): Boolean {
        return type == TransactionType.LOCAL_BAKER
    }

    fun isDelegationTransfer(): Boolean {
        return type == TransactionType.LOCAL_DELEGATION
    }

    fun isSimpleTransfer(): Boolean {
        return type == TransactionType.TRANSFER || type == TransactionType.TRANSFERWITHMEMO
    }

    fun isTransferToSecret(): Boolean {
        return type == TransactionType.TRANSFERTOENCRYPTED
    }

    fun isTransferToPublic(): Boolean {
        return type == TransactionType.TRANSFERTOPUBLIC
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

    fun getTotalAmountForRegular(): BigInteger {
        if (transactionStatus == TransactionStatus.ABSENT) {
            return BigInteger.ZERO
        } else if (outcome == TransactionOutcome.Reject) {
            return if (cost == null) BigInteger.ZERO else -cost
        }
        return total
    }

    fun getTotalAmountForSmartContractUpdate(): BigInteger {
        return if (cost == null) BigInteger.ZERO else -cost
    }
}
