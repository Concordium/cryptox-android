package com.concordium.wallet.data.model

import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.CONFIGURE_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REGISTER_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REMOVE_BAKER
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_KEYS
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_POOL
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_STAKE
import com.concordium.wallet.data.room.Account
import java.io.Serializable
import java.math.BigInteger

/**
 * Class used for collecting data from AccountDetails all the way to submission
 */
data class BakerDelegationData(
    val account: Account,
    var isLPool: Boolean = false,
    var isBakerPool: Boolean = true,
    var isTransactionInProgress: Boolean = false,
    var bakerKeys: BakerKeys? = null,
    var type: String,
) : Serializable {

    var poolId: String = account.delegation?.delegationTarget?.bakerId?.toString() ?: ""
    var amount: BigInteger? = account.baker?.stakedAmount
        ?: account.delegation?.stakedAmount
    var restake: Boolean =
        account.baker?.restakeEarnings
            ?: account.delegation?.restakeEarnings
            ?: true
    var bakerPoolInfo: BakerPoolInfo? = account.baker?.bakerPoolInfo
        ?: BakerPoolInfo(BakerPoolInfo.OPEN_STATUS_OPEN_FOR_ALL)
    var submissionId: String? = null
    var energy: Long? = null
    var accountNonce: AccountNonce? = null
    var finalizationCommissionRate: Double? = null
    var bakingCommissionRate: Double? = null
    var transactionCommissionRate: Double? = null
    var chainParameters: ChainParameters? = null
        set(value) {
            field = value
            finalizationCommissionRate = value?.finalizationCommissionRange?.max
            bakingCommissionRate = value?.bakingCommissionRange?.max
            transactionCommissionRate = value?.transactionCommissionRange?.max
        }
    var bakerPoolStatus: BakerPoolStatus? = null
    var passiveDelegation: PassiveDelegation? = null
    var cost: BigInteger? = null
    var metadataUrl: String? = account.baker?.bakerPoolInfo?.metadataUrl
    var toSetBakerSuspended: Boolean? = null

    fun isUpdateBaker(): Boolean {
        return type == UPDATE_BAKER_STAKE || type == UPDATE_BAKER_POOL || type == UPDATE_BAKER_KEYS || type == CONFIGURE_BAKER
    }

    val oldStakedAmount: BigInteger? = account.baker?.stakedAmount
        ?: account.delegation?.stakedAmount
    val oldRestake: Boolean? = account.baker?.restakeEarnings
        ?: account.delegation?.restakeEarnings
    val oldDelegationIsBaker: Boolean =
        account.delegation?.delegationTarget?.delegateType == DelegationTarget.TYPE_DELEGATE_TO_BAKER
    val oldDelegationTargetPoolId: Long? = account.delegation?.delegationTarget?.bakerId
    val oldMetadataUrl: String? = account.baker?.bakerPoolInfo?.metadataUrl
    val oldOpenStatus: String? = account.baker?.bakerPoolInfo?.openStatus
    val oldCommissionRates: CommissionRates? = account.baker?.bakerPoolInfo?.commissionRates

    fun isBakerFlow(): Boolean {
        return type == REGISTER_BAKER || type == UPDATE_BAKER_STAKE || type == UPDATE_BAKER_POOL || type == UPDATE_BAKER_KEYS || type == REMOVE_BAKER || type == CONFIGURE_BAKER
    }
}
