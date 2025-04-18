package com.concordium.wallet.data.model

import java.io.Serializable
import java.math.BigInteger

data class AccountBaker(
    val restakeEarnings: Boolean,
    val bakerId: Int,
    val bakerPoolInfo: BakerPoolInfo,
    val stakedAmount: BigInteger,
    val pendingChange: PendingChange?,
    val bakerAggregationVerifyKey: String,
    val bakerElectionVerifyKey: String,
    val bakerSignatureVerifyKey: String,
    val isSuspended: Boolean,
    val isPrimedForSuspension: Boolean?, // Missing effectively means it is not primed for suspension.
) : Serializable
