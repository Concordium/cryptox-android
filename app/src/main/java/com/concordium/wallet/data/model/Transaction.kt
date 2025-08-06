package com.concordium.wallet.data.model

import com.concordium.sdk.serializing.CborMapper
import com.reown.util.hexToBytes
import java.io.Serializable
import java.math.BigInteger
import java.util.Date


class Transaction(
    val source: TransactionSource,
    val timeStamp: Date,
    var title: String = "",
    val subtotal: BigInteger?,
    val cost: BigInteger?,
    val total: BigInteger,
    var transactionStatus: TransactionStatus,
    var outcome: TransactionOutcome,
    var blockHashes: List<String>?,
    var transactionHash: String?,
    var rejectReason: String?,
    val events: List<String>?,
    val fromAddress: String?,
    val toAddress: String?,
    var fromAddressTitle: String?,
    var toAddressTitle: String?,
    val submissionId: String?,
    val origin: TransactionOrigin?,
    val details: TransactionDetails,
) : Serializable {

    fun isRemoteTransaction(): Boolean {
        return transactionStatus == TransactionStatus.FINALIZED
    }

    fun isBakerTransfer(): Boolean {
        return details.type == TransactionType.LOCAL_BAKER
    }

    fun isDelegationTransfer(): Boolean {
        return details.type == TransactionType.LOCAL_DELEGATION
    }

    fun isSimpleTransfer(): Boolean {
        return details.type == TransactionType.TRANSFER || details.type == TransactionType.TRANSFERWITHMEMO
    }

    fun isTransferToSecret(): Boolean {
        return details.type == TransactionType.TRANSFERTOENCRYPTED
    }

    fun isTransferToPublic(): Boolean {
        return details.type == TransactionType.TRANSFERTOPUBLIC
    }

    fun isEncryptedTransfer(): Boolean {
        return details.type == TransactionType.ENCRYPTEDAMOUNTTRANSFER || details.type == TransactionType.ENCRYPTEDAMOUNTTRANSFERWITHMEMO
    }

    fun isSmartContractUpdate(): Boolean {
        return details.type == TransactionType.UPDATE
    }

    fun isOriginSelf(): Boolean {
        return origin?.type == TransactionOriginType.Self
    }

    fun isBakerSuspension(): Boolean {
        return details.type == TransactionType.VALIDATOR_SUSPENDED
    }

    fun isBakerPrimingForSuspension(): Boolean {
        return details.type == TransactionType.VALIDATOR_PRIMED_FOR_SUSPENSION
    }

    fun isTokenUpdate(): Boolean {
        return details.type == TransactionType.TOKEN_UPDATE
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

    fun getDecodedMemo(): String? =
        try {
            details
                .memo
                ?.hexToBytes()
                ?.let { CborMapper.INSTANCE.readValue(it, String::class.java) }
        } catch (e: Exception){
            details.memo
        }

    fun hasMemo(): Boolean {
        return !details.memo.isNullOrEmpty()
    }
}
