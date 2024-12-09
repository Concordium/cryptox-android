package com.concordium.wallet.data.util

import com.concordium.wallet.data.model.Token
import com.concordium.wallet.util.toBigInteger
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.regex.Pattern

object CurrencyUtil {
    /**
     * The maximum possible amount in Concordium,
     * which is the maximum value of an unsigned 256-bit integer.
     */
    val MAX_AMOUNT =
        "115792089237316195423570985008687907853269984665640564039457584007913129639935"
            .toBigInteger()

    private const val ZERO_AMOUNT = "0.00"
    private val separator: Char = DecimalFormatSymbols.getInstance().decimalSeparator
    private val patternGTU: Pattern = Pattern.compile("^-?[0-9]*(\\.[0-9]{0,77})?\$")

    //Format the Decimal value with comma separators for thousands
    private val formatter = DecimalFormat("#,###.######", DecimalFormatSymbols(Locale.US))

    fun formatGTU(value: String, withGStroke: Boolean = false, decimals: Int = 6): String =
        formatGTU(value.toBigInteger(), withGStroke, decimals)

    fun formatGTU(value: BigInteger, token: Token?): String {
        val decimals = token?.decimals ?: 0
        val withGStroke = token == null || token.isCcd
        return formatGTU(value, withGStroke, decimals)
    }

    fun formatGTU(value: BigInteger, withGStroke: Boolean = false, decimals: Int = 6): String {
        if (value == BigInteger.ZERO) return ZERO_AMOUNT
        if (decimals <= 0) return value.toString()

        val isNegative = value.signum() < 0
        val str =
            if (isNegative)
                value.toString().replace("-", "")
            else
                value.toString()
        val strBuilder = StringBuilder(str)
        if (strBuilder.length <= decimals - 1) {
            // Add zeroes in front of the value until there are four chars
            while (strBuilder.length < decimals) {
                strBuilder.insert(0, "0")
            }
            // Insert 0. in front of the four chars
            strBuilder.insert(0, separator)
            strBuilder.insert(0, "0")
        } else {
            // The string is at least decimals chars - insert separator in front of the last decimals
            strBuilder.insert(strBuilder.length - decimals, separator)
        }
        while (strBuilder.substring(strBuilder.indexOf(separator)).length > 3
            && strBuilder.endsWith("0")
        ) {
            strBuilder.delete(strBuilder.length - 1, strBuilder.length)
        }

        if (strBuilder.toString().startsWith(separator))
            strBuilder.insert(0, "0")

        if (isNegative) {
            strBuilder.insert(0, "-")
        }

        return formatGTUWithCommas(strBuilder.toString().removeSuffix(separator.toString()))
    }

    fun formatAndRoundGTU(value: BigInteger, roundDecimals: Int): String {
        if (value == BigInteger.ZERO) return ZERO_AMOUNT

        val bigDecimalValue = formatGTU(value)
            .replace(",", "")
            .toBigDecimal()
            .setScale(roundDecimals, RoundingMode.HALF_DOWN)

        //Format the small Decimal value for proper trailing zeros not omitted
        val decimalFormatter = DecimalFormat(
            "#,##0." + "0".repeat(roundDecimals),
            DecimalFormatSymbols(Locale.US)
        )

        return decimalFormatter.format(bigDecimalValue)
    }

    fun toGTUValue(stringValue: String, token: Token?): BigInteger? =
        toGTUValue(stringValue, token?.decimals ?: 0)

    fun toGTUValue(stringValue: String, decimals: Int = 6): BigInteger? {
        var str = stringValue.replace("Ͼ", "")
        if (str.isEmpty()) {
            return null
        }
        if (!checkGTUString(str)) {
            return null
        }
        val decimalSeparatorIndex = str.indexOf('.')
        // Ensure that there is the required number of decimals.
        if (decimalSeparatorIndex == -1) {
            str = "${str}." + String(CharArray(decimals) { '0' })
        } else {
            val missingZeros = decimals - ((str.length - 1) - decimalSeparatorIndex)
            for (i in 1..missingZeros) {
                str += "0"
            }
        }
        // Remove the separator to get the value (because there are four decimals)
        val noDecimalSeparatorString = str.replace(".", "")
        return noDecimalSeparatorString.toBigInteger()
    }

    private fun checkGTUString(stringValue: String): Boolean {
        return patternGTU.matcher(stringValue).matches()
    }

    private fun formatGTUWithCommas(value: String): String {
        val bigDecimalValue = value.replace(separator, '.').toBigDecimal()
        return formatter.format(bigDecimalValue)
    }
}
