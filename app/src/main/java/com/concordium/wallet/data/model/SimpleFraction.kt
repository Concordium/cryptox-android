package com.concordium.wallet.data.model

import java.math.BigInteger
import java.math.MathContext

/**
 * A fraction defined as 2 integers:
 * ```
 *  N
 *  –
 *  D
 * ```
 *
 * @see toBigDecimal
 */
data class SimpleFraction(
    val numerator: BigInteger,
    val denominator: BigInteger,
) {
    fun toBigDecimal() =
        numerator.toBigDecimal().divide(denominator.toBigDecimal(), MathContext.UNLIMITED)
}
