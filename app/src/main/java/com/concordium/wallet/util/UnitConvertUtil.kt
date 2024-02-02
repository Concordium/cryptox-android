package com.concordium.wallet.util

import kotlin.math.floor

object UnitConvertUtil {
    fun secondsToDaysRoundedDown(seconds: Long): Int {
        return floor((seconds.toDouble() / 60 / 60 / 24)).toInt()
    }
}
