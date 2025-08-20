package com.concordium.wallet.data.util

import com.concordium.wallet.data.model.SimpleFraction
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.util.toBigInteger
import java.math.BigDecimal
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
    private val formatter = DecimalFormat().apply {
        decimalFormatSymbols = DecimalFormatSymbols(Locale.US)
        maximumIntegerDigits = Int.MAX_VALUE
        maximumFractionDigits = Int.MAX_VALUE
    }

    fun formatGTU(value: String, decimals: Int = 6): String =
        formatGTU(value.toBigInteger(), decimals)

    fun formatGTU(value: BigInteger, token: Token): String =
        formatGTU(value, token.decimals)

    fun formatGTU(
        value: BigInteger,
        decimals: Int = 6,
        withCommas: Boolean = true,
    ): String {
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

        val formattedString = strBuilder.toString().removeSuffix(separator.toString())

        return if (withCommas) formatGTUWithCommas(formattedString) else formattedString
    }

    fun formatAndRoundGTU(value: BigInteger, roundDecimals: Int, decimals: Int = 6): String {
        if (value == BigInteger.ZERO) return ZERO_AMOUNT

        val bigDecimalValue = formatGTU(value, decimals)
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

    fun formatCompactGTU(value: BigInteger, decimals: Int = 6): String {
        if (value == BigInteger.ZERO) return ZERO_AMOUNT

        val gtuValue = formatGTU(value, decimals)
            .replace(",", "")
            .toBigDecimal()

        val scaledValue: Pair<BigDecimal, String> = when {
            gtuValue < BigDecimal(1_000) -> gtuValue.setScale(2, RoundingMode.HALF_UP) to ""
            gtuValue < BigDecimal(1_000_000) -> (gtuValue.divide(BigDecimal(1_000)))
                .setScale(2, RoundingMode.HALF_DOWN) to "k"

            gtuValue < BigDecimal(1_000_000_000) -> (gtuValue.divide(BigDecimal(1_000_000)))
                .setScale(2, RoundingMode.HALF_DOWN) to "m"

            else -> (gtuValue.divide(BigDecimal(1_000_000_000)))
                .setScale(2, RoundingMode.HALF_DOWN) to "b"
        }

        return "${
            scaledValue.first.stripTrailingZeros().toPlainString()
        } ${scaledValue.second}".trim()
    }

    fun toGTUValue(stringValue: String, token: Token): BigInteger? =
        toGTUValue(stringValue, token.decimals)

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

    fun toEURRate(amount: BigInteger, eurPerUnit: SimpleFraction): String {
        val amountBigDecimal = BigDecimal(amount)
        val denominatorBigDecimal = BigDecimal(eurPerUnit.denominator)
        val numeratorBigDecimal = BigDecimal(eurPerUnit.numerator)

        return amountBigDecimal
            .multiply(numeratorBigDecimal)
            .divide(denominatorBigDecimal, 2, RoundingMode.HALF_UP)
            .toString()
    }

    private fun checkGTUString(stringValue: String): Boolean {
        return patternGTU.matcher(stringValue).matches()
    }

    private fun formatGTUWithCommas(value: String): String {
        val bigDecimalValue = value.replace(separator, '.').toBigDecimal()
        return formatter.format(bigDecimalValue)
    }
}
