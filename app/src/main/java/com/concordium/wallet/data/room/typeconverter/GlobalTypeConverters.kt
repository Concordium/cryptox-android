package com.concordium.wallet.data.room.typeconverter

import androidx.room.TypeConverter
import com.concordium.wallet.core.AppCore
import com.concordium.wallet.data.model.EncryptedData
import com.concordium.wallet.data.model.ShieldedAccountEncryptionStatus
import com.concordium.wallet.data.model.TransactionOutcome
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.util.toBigInteger
import java.math.BigInteger

class GlobalTypeConverters {
    private val gson = AppCore.getGson()

    @TypeConverter
    fun intToTransactionStatus(value: Int): TransactionStatus {
        return when (value) {
            0 -> TransactionStatus.RECEIVED
            1 -> TransactionStatus.ABSENT
            2 -> TransactionStatus.COMMITTED
            3 -> TransactionStatus.FINALIZED
            else -> TransactionStatus.UNKNOWN
        }
    }

    @TypeConverter
    fun transactionStatusToInt(transactionStatus: TransactionStatus): Int {
        return transactionStatus.code
    }

    @TypeConverter
    fun intToTransactionOutcome(value: Int): TransactionOutcome {
        return when (value) {
            0 -> TransactionOutcome.Success
            1 -> TransactionOutcome.Reject
            2 -> TransactionOutcome.Ambiguous
            else -> TransactionOutcome.UNKNOWN
        }
    }

    @TypeConverter
    fun transactionOutcomeToInt(transactionOutcome: TransactionOutcome): Int {
        return transactionOutcome.code
    }

    @TypeConverter
    fun intToShieldedAccountEncryptionStatus(value: Int): ShieldedAccountEncryptionStatus {
        return when (value) {
            ShieldedAccountEncryptionStatus.ENCRYPTED.code -> ShieldedAccountEncryptionStatus.ENCRYPTED
            ShieldedAccountEncryptionStatus.PARTIALLYDECRYPTED.code -> ShieldedAccountEncryptionStatus.PARTIALLYDECRYPTED
            ShieldedAccountEncryptionStatus.DECRYPTED.code -> ShieldedAccountEncryptionStatus.DECRYPTED
            else -> ShieldedAccountEncryptionStatus.DECRYPTED
        }
    }

    @TypeConverter
    fun shieldedAccountEncryptionStatusToInt(status: ShieldedAccountEncryptionStatus): Int {
        return status.code
    }

    @TypeConverter
    fun bigIntegerToString(value: BigInteger?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun stringToBigInteger(value: String?): BigInteger? {
        return value?.toBigInteger()
    }

    @TypeConverter
    fun encryptedDataToJson(value: EncryptedData?): String? {
        return value?.let(gson::toJson)
    }

    @TypeConverter
    fun jsonToEncryptedData(value: String?): EncryptedData? {
        return value?.takeIf(String::isNotEmpty)?.let {
            gson.fromJson(it, EncryptedData::class.java)
        }
    }
}
