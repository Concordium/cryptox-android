package com.concordium.wallet.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.math.BigInteger

data class ChainParameters(
    val mintPerPayday: Double,
    val poolOwnerCooldown: Long,
    val capitalBound: Double,
    val rewardPeriodLength: Int,
    val transactionCommissionLPool: Double,
    val foundationAccountIndex: Int,
    val finalizationCommissionLPool: Double,
    val delegatorCooldown: Long,
    val bakingCommissionLPool: Double,
    val accountCreationLimit: Int,
    val minimumEquityCapital: BigInteger,
    val bakingCommissionRange: BakingCommissionRange,
    val bakingCommissionRate: Double? = null,
    val transactionCommissionRange: TransactionCommissionRange,
    val transactionCommissionRate: Double? = null,
    val finalizationCommissionRange: FinalizationCommissionRange,
    val euroPerEnergy: SimpleFraction,
    @SerializedName("microGTUPerEuro")
    val microGtuPerEuro: SimpleFraction,
) : Serializable
