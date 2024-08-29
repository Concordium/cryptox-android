package com.concordium.wallet.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.concordium.wallet.App
import com.concordium.wallet.data.model.AccountBaker
import com.concordium.wallet.data.model.AccountDelegation
import com.concordium.wallet.data.model.AccountEncryptedAmount
import com.concordium.wallet.data.model.AccountReleaseSchedule
import com.concordium.wallet.data.model.CredentialWrapper
import com.concordium.wallet.data.model.IdentityAttribute
import com.concordium.wallet.data.model.ShieldedAccountEncryptionStatus
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.typeconverter.AccountTypeConverters
import com.google.gson.JsonObject
import java.io.Serializable
import java.math.BigInteger

@Entity(tableName = "account_table", indices = [Index(value = ["address"], unique = true)])
@TypeConverters(AccountTypeConverters::class)
data class Account(
    @PrimaryKey(autoGenerate = true)
    var id: Int,

    @ColumnInfo(name = "identity_id")
    val identityId: Int,

    var name: String,

    var address: String,

    @ColumnInfo(name = "submission_id")
    var submissionId: String,

    @ColumnInfo(name = "transaction_status")
    var transactionStatus: TransactionStatus,

    @ColumnInfo(name = "encrypted_account_data")
    var encryptedAccountData: String,

    @ColumnInfo(name = "revealed_attributes")
    var revealedAttributes: List<IdentityAttribute>,

    var credential: CredentialWrapper?,

    @ColumnInfo(name = "finalized_balance")
    var balance: BigInteger = BigInteger.ZERO,

    // TODO bind in to accountAtDisposal and remove balanceAtDisposal() method
//    @ColumnInfo(name = "balance_at_disposal")
//    var balanceAtDisposal: BigInteger = BigInteger.ZERO,

    @ColumnInfo(name = "total_shielded_balance")
    var shieldedBalance: BigInteger = BigInteger.ZERO,

    @ColumnInfo(name = "finalized_encrypted_balance")
    var encryptedBalance: AccountEncryptedAmount?,

    @ColumnInfo(name = "current_balance_status")
    var encryptedBalanceStatus: ShieldedAccountEncryptionStatus = ShieldedAccountEncryptionStatus.DECRYPTED,

    @ColumnInfo(name = "total_staked")
    var totalStaked: BigInteger = BigInteger.ZERO,

    @ColumnInfo(name = "read_only")
    var readOnly: Boolean = false,

    @ColumnInfo(name = "finalized_account_release_schedule")
    var releaseSchedule: AccountReleaseSchedule?,

    @ColumnInfo(name = "baker_id")
    var bakerId: Long? = null,

    @ColumnInfo(name = "account_delegation")
    var accountDelegation: AccountDelegation? = null,

    @ColumnInfo(name = "account_baker")
    var accountBaker: AccountBaker? = null,

    @ColumnInfo(name = "accountIndex")
    var accountIndex: Int? = null,

    @ColumnInfo(name = "cred_number")
    var credNumber: Int
) : Serializable {

    companion object {
        fun getDefaultName(address: String): String {
            if (address.length >= 8) {
                return "${address.subSequence(0, 4)}…${address.substring(address.length - 4)}"
            }
            return address
        }
    }

    fun isInitial(): Boolean {
        if (readOnly || isBaking() || isDelegating()) {
            return false
        }
        val credential = this.credential ?: return true
        val gson = App.appCore.gson
        val credentialValueJsonObject = gson.fromJson(credential.value.json, JsonObject::class.java)
        if (credentialValueJsonObject["type"]?.asString == "initial") {
            return true
        }
        if (credentialValueJsonObject.getAsJsonObject("credential")
                ?.get("type")?.asString == "initial"
        )
            return true
        return false
    }

    fun getAccountName(): String {
        return if (readOnly) {
            getDefaultName(address)
        } else {
            name.takeUnless(String::isEmpty)
                ?: getDefaultName(address)
        }
    }

    fun isBaking(): Boolean {
        return accountBaker != null
    }

    fun isBaker(): Boolean {
        return bakerId != null
    }

    fun isDelegating(): Boolean {
        return accountDelegation != null
    }

    fun mayNeedUnshielding(): Boolean {
        if (encryptedBalance == null || readOnly) {
            return false
        }

        val isShieldedBalanceUnknown = encryptedBalanceStatus == ShieldedAccountEncryptionStatus.ENCRYPTED
                || encryptedBalanceStatus == ShieldedAccountEncryptionStatus.PARTIALLYDECRYPTED
        val isShieldedBalancePositive = encryptedBalanceStatus == ShieldedAccountEncryptionStatus.DECRYPTED
                && shieldedBalance.signum() > 0
        return isShieldedBalanceUnknown || isShieldedBalancePositive
    }

    fun balanceAtDisposal(): BigInteger {
        val stakedAmount: BigInteger = accountDelegation?.stakedAmount
            ?: accountBaker?.stakedAmount ?: BigInteger.ZERO
        val scheduledTotal: BigInteger = releaseSchedule?.total ?: BigInteger.ZERO
        val subtract: BigInteger = if (stakedAmount in BigInteger.ONE..scheduledTotal)
            scheduledTotal
        else if (stakedAmount.signum() > 0 && stakedAmount > scheduledTotal)
            stakedAmount
        else if (stakedAmount.signum() == 0 && scheduledTotal.signum() > 0)
            scheduledTotal
        else
            BigInteger.ZERO
        return BigInteger.ZERO.max(balance - subtract)
    }
}
