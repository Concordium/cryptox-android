package com.concordium.wallet.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.concordium.wallet.App
import com.concordium.wallet.data.model.AccountBaker
import com.concordium.wallet.data.model.AccountCooldown
import com.concordium.wallet.data.model.AccountDelegation
import com.concordium.wallet.data.model.AccountEncryptedAmount
import com.concordium.wallet.data.model.AccountReleaseSchedule
import com.concordium.wallet.data.model.CredentialWrapper
import com.concordium.wallet.data.model.EncryptedData
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
    var id: Int = 0,

    @ColumnInfo(name = "identity_id")
    val identityId: Int,

    @ColumnInfo("name")
    var name: String,

    @ColumnInfo("address")
    var address: String,

    @ColumnInfo(name = "submission_id")
    var submissionId: String,

    @ColumnInfo(name = "transaction_status")
    var transactionStatus: TransactionStatus,

    @ColumnInfo(name = "encrypted_account_data")
    var encryptedAccountData: EncryptedData?,

    @ColumnInfo("credential")
    var credential: CredentialWrapper?,

    @ColumnInfo(name = "cred_number")
    var credNumber: Int,

    @ColumnInfo(name = "revealed_attributes")
    var revealedAttributes: List<IdentityAttribute> = emptyList(),

    @ColumnInfo(name = "finalized_balance")
    var balance: BigInteger = BigInteger.ZERO,

    @ColumnInfo(name = "balance_at_disposal")
    var balanceAtDisposal: BigInteger = BigInteger.ZERO,

    @ColumnInfo(name = "total_shielded_balance")
    var shieldedBalance: BigInteger = BigInteger.ZERO,

    @ColumnInfo(name = "finalized_encrypted_balance")
    var encryptedBalance: AccountEncryptedAmount? = null,

    @ColumnInfo(name = "current_balance_status")
    var encryptedBalanceStatus: ShieldedAccountEncryptionStatus = ShieldedAccountEncryptionStatus.DECRYPTED,

    @ColumnInfo(name = "read_only")
    var readOnly: Boolean = false,

    @ColumnInfo(name = "finalized_account_release_schedule")
    var releaseSchedule: AccountReleaseSchedule? = null,

    @ColumnInfo(name = "cooldowns")
    var cooldowns: List<AccountCooldown> = emptyList(),

    @ColumnInfo(name = "account_delegation")
    var delegation: AccountDelegation? = null,

    @ColumnInfo(name = "account_baker")
    var baker: AccountBaker? = null,

    @ColumnInfo(name = "accountIndex")
    var index: Int? = null,

    @ColumnInfo(name = "is_active", defaultValue = "0")
    var isActive: Boolean = false,

    @ColumnInfo(name = "icon_id")
    var iconId: Int,
) : Serializable {

    companion object {
        fun getDefaultName(address: String): String {
            if (address.length >= 8) {
                return "${address.subSequence(0, 4)}…${address.substring(address.length - 4)}"
            }
            return address
        }
    }

    val stakedAmount: BigInteger
        get() = baker?.stakedAmount ?: BigInteger.ZERO

    val delegatedAmount: BigInteger
        get() = delegation?.stakedAmount ?: BigInteger.ZERO

    val cooldownAmount: BigInteger
        get() = cooldowns.sumOf(AccountCooldown::amount)

    val isDelegationBakerSuspended: Boolean
        get() = delegation?.isSuspended == true

    val isBakerSuspended: Boolean
        get() = baker?.isSuspended == true

    val isBakerPrimedForSuspension: Boolean
        get() = baker?.isPrimedForSuspension == true

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

    fun getAccountName(): String =
        if (readOnly) {
            getDefaultName(address)
        } else {
            name.takeUnless(String::isEmpty)
                ?: getDefaultName(address)
        }

    fun isBaking(): Boolean =
        baker != null

    fun isDelegating(): Boolean =
        delegation != null

    fun hasCooldowns(): Boolean =
        cooldowns.isNotEmpty()

    fun mayNeedUnshielding(): Boolean {
        if (encryptedBalance == null || readOnly) {
            return false
        }

        val isShieldedBalanceUnknown =
            encryptedBalanceStatus == ShieldedAccountEncryptionStatus.ENCRYPTED
                    || encryptedBalanceStatus == ShieldedAccountEncryptionStatus.PARTIALLYDECRYPTED
        val isShieldedBalancePositive =
            encryptedBalanceStatus == ShieldedAccountEncryptionStatus.DECRYPTED
                    && shieldedBalance.signum() > 0
        return isShieldedBalanceUnknown || isShieldedBalancePositive
    }
}
