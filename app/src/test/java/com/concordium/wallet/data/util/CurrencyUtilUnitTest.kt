package com.concordium.wallet.data.util

import com.concordium.wallet.ui.connect.WsMessageResponse
import com.concordium.wallet.util.HexUtil.toHexLE
import com.concordium.wallet.util.decodeBase58ToHex
import com.google.gson.Gson
import org.junit.Test

import org.junit.Assert.*
import java.text.DecimalFormatSymbols

class CurrencyUtilUnitTest {

    private val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator

    private fun replaceDecimalSep(str: String): String {
        if (decimalSeparator != '.') {
            return str.replace('.', decimalSeparator)
        }
        return str
    }

    @Test
    fun wsMessageResponseTest() {
        val q = 355777373255237640.toHexLE()
        val w = "4kAioGm4dujSVh18TdAh8R91wW3W4kaEJ7oSg7d6cgb7BsirJ2".decodeBase58ToHex()
        val z = "4kAioGm4dujSVh18TdAh8R91wW3W4kaEJ7oSg7d6cgb7BsirJ2".decodeBase58ToHex()
        val qq = q + w + z
        println(qq)

        "080000d0a2f9ef0432000000346b41696f476d3464756a5356683138546441683852393177573357346b61454a376f536737643663676237427369724a3232000000346b41696f476d3464756a5356683138546441683852393177573357346b61454a376f536737643663676237427369724a32"
        "080000d0a2f9ef04ed1b0863a5eadf6f80c4a77433f6a2b3a48ce7bf41a1c4bba38115d52923c637ed1b0863a5eadf6f80c4a77433f6a2b3a48ce7bf41a1c4bba38115d52923c637"
        "080000d0a2f9ef04ed1b0863a5eadf6f80c4a77433f6a2b3a48ce7bf41a1c4bba38115d52923c63701ed1b0863a5eadf6f80c4a77433f6a2b3a48ce7bf41a1c4bba38115d52923c637"
    }

    @Test
    fun testParseUpdateJson() {
        val jj =
            "{\"data\":{\"amount\":\"0\",\"contract_address\":{\"address\":\"\",\"index\":\"51\",\"sub_index\":\"0\"},\"contract_method\":\"create\",\"contract_name\":\"inventory\",\"contract_params\":[{\"param_type\":\"uint64\",\"param_value\":\"305261301993570307\"},{\"param_type\":\"accountaddress\",\"param_value\":\"\"},{\"param_type\":\"uint64\"}],\"expiry\":1629890799,\"from\":\"3YCF2vibCvhXZEpRxqZZ74RcyktRCX2RRVSswxhkFvN93gzvA8\",\"nonce\":\"\"},\"message_type\":\"Transaction\"}"

        val gson = Gson()
        val json = gson.fromJson(jj, WsMessageResponse::class.java)
        println()
    }

    @Test
    fun hexTest() {

        val pp = 305455883003887619.toHexLE()
        val pp2 = 1L.toHexLE()
        val pp3 = 0.toHexLE()

        assertEquals(pp, "030000d981323d04")
        assertEquals(pp2, "0000000000000000")
        assertEquals(pp3, "00")

        println()
    }

    // 80eddbdc1168f1daeadbd3e44c1e3f8f5a284c2029f78ad26af98583a499de5b1913a4f863
// 80eddbdc1168f1daeadbd3e44c1e3f8f5a284c2029f78ad26af98583a499de5b19
    @Test
    fun decodeBase58() {
        val res = "3YCF2vibCvhXZEpRxqZZ74RcyktRCX2RRVSswxhkFvN93gzvA8".decodeBase58ToHex()
        assertEquals(res, "4e38ba24a686ce26790ddfeac493f2ac7cbe97c85b6bbeb653edf906e0ffe5bf")

        println()
    }

    @Test
    fun formatGTU() {

        assertEquals(replaceDecimalSep("0.00"), CurrencyUtil.formatGTU(0))

        assertEquals(replaceDecimalSep("1.00"), CurrencyUtil.formatGTU(1000000))
        assertEquals(replaceDecimalSep("100.00"), CurrencyUtil.formatGTU(100000000))
        assertEquals(replaceDecimalSep("1.20"), CurrencyUtil.formatGTU(1200000))
        assertEquals(replaceDecimalSep("1.23"), CurrencyUtil.formatGTU(1230000))
        assertEquals(replaceDecimalSep("1.234"), CurrencyUtil.formatGTU(1234000))
        assertEquals(replaceDecimalSep("1.2345"), CurrencyUtil.formatGTU(1234500))
        assertEquals(replaceDecimalSep("123.4567"), CurrencyUtil.formatGTU(123456700))
        assertEquals(replaceDecimalSep("0.0001"), CurrencyUtil.formatGTU(100))
        assertEquals(replaceDecimalSep("0.0012"), CurrencyUtil.formatGTU(1200))
        assertEquals(replaceDecimalSep("0.01"), CurrencyUtil.formatGTU(10000))
        assertEquals(replaceDecimalSep("0.0123"), CurrencyUtil.formatGTU(12300))
        assertEquals(replaceDecimalSep("0.20"), CurrencyUtil.formatGTU(200000))
        assertEquals(replaceDecimalSep("0.23"), CurrencyUtil.formatGTU(230000))
        assertEquals(replaceDecimalSep("0.234"), CurrencyUtil.formatGTU(234000))
        assertEquals(replaceDecimalSep("0.2345"), CurrencyUtil.formatGTU(234500))

        assertEquals(replaceDecimalSep("-1.00"), CurrencyUtil.formatGTU(-1000000))
        assertEquals(replaceDecimalSep("-100.00"), CurrencyUtil.formatGTU(-100000000))
        assertEquals(replaceDecimalSep("-1.20"), CurrencyUtil.formatGTU(-1200000))
        assertEquals(replaceDecimalSep("-1.23"), CurrencyUtil.formatGTU(-1230000))
        assertEquals(replaceDecimalSep("-1.234"), CurrencyUtil.formatGTU(-1234000))
        assertEquals(replaceDecimalSep("-1.2345"), CurrencyUtil.formatGTU(-1234500))
        assertEquals(replaceDecimalSep("-123.4567"), CurrencyUtil.formatGTU(-123456700))
        assertEquals(replaceDecimalSep("-0.0001"), CurrencyUtil.formatGTU(-100))
        assertEquals(replaceDecimalSep("-0.0012"), CurrencyUtil.formatGTU(-1200))
        assertEquals(replaceDecimalSep("-0.01"), CurrencyUtil.formatGTU(-10000))
        assertEquals(replaceDecimalSep("-0.0123"), CurrencyUtil.formatGTU(-12300))
        assertEquals(replaceDecimalSep("-0.20"), CurrencyUtil.formatGTU(-200000))
        assertEquals(replaceDecimalSep("-0.23"), CurrencyUtil.formatGTU(-230000))
        assertEquals(replaceDecimalSep("-0.234"), CurrencyUtil.formatGTU(-234000))
        assertEquals(replaceDecimalSep("-0.2345"), CurrencyUtil.formatGTU(-234500))

        assertEquals(replaceDecimalSep("-100.234547"), CurrencyUtil.formatGTU(-100234547))
        assertEquals(replaceDecimalSep("-100.23454"), CurrencyUtil.formatGTU(-100234540))
        assertEquals(replaceDecimalSep("-100.2345"), CurrencyUtil.formatGTU(-100234500))
        assertEquals(replaceDecimalSep("-100.234"), CurrencyUtil.formatGTU(-100234000))
        assertEquals(replaceDecimalSep("-100.23"), CurrencyUtil.formatGTU(-100230000))
        assertEquals(replaceDecimalSep("-100.20"), CurrencyUtil.formatGTU(-100200000))
        assertEquals(replaceDecimalSep("-100.00"), CurrencyUtil.formatGTU(-100000000))

        // For the following to be tested it needs to be in an instumented test, or an
        // alternative solution for the resource string is needed: let the CurrencyUtil use a
        // ResourcecProvider.getString (that is set on the AppCore) instead of having reference to context
        // assertEquals(replaceDecimalSep("0.00"), CurrencyUtil.formatGTU(0, withGStroke = true))
        // assertEquals(replaceDecimalSep("1.00"), CurrencyUtil.formatGTU(10000, withGStroke = true))
        // assertEquals(replaceDecimalSep("-1.00"), CurrencyUtil.formatGTU(-10000, withGStroke = true))
    }

    @Test
    fun toGTUValue() {
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1.00")))
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1")))
        assertEquals(123456000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("123456")))
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1.0")))
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1.000")))
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1.0000")))
        assertEquals(1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("1.000000")))
        assertEquals(null, CurrencyUtil.toGTUValue(replaceDecimalSep("1.00.00.00")))
        assertEquals(null, CurrencyUtil.toGTUValue(replaceDecimalSep("")))
        assertEquals(null, CurrencyUtil.toGTUValue(replaceDecimalSep("abc")))

        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.00")))
        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1")))
        assertEquals(-123456000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-123456")))
        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.0")))
        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.000")))
        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.0000")))
        assertEquals(-1000000L, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.000000")))
        assertEquals(null, CurrencyUtil.toGTUValue(replaceDecimalSep("-1.00.00.00")))
    }
}
