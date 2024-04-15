package com.concordium.wallet.data.model

import java.math.BigInteger

data class TransactionCost(
    val energy: Long,
    val cost: BigInteger,
) {
    constructor(
        energy: Long,
        euroPerEnergy: SimpleFraction,
        microGTUPerEuro: SimpleFraction,
    ) : this(
        energy = energy,
        cost = (energy.toBigInteger() * euroPerEnergy.numerator * microGTUPerEuro.numerator)
            .divide(euroPerEnergy.denominator * microGTUPerEuro.denominator)
    )
}
