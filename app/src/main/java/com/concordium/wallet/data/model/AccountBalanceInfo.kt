package com.concordium.wallet.data.model

import java.io.Serializable
import java.math.BigInteger

data class AccountBalanceInfo(
    val accountAmount: BigInteger,
    val accountAtDisposal: BigInteger,
    val accountEncryptedAmount: AccountEncryptedAmount,
    val accountNonce: Int,
    val accountReleaseSchedule: AccountReleaseSchedule,
    val accountCooldowns: List<AccountCooldown>,
    val accountBaker: AccountBaker?,
    val accountDelegation: AccountDelegation?,
    val accountIndex: Int,
    val accountTokens: List<PLTInfoWithAccountState>?,
) : Serializable
