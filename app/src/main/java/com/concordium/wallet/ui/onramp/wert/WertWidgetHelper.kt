package com.concordium.wallet.ui.onramp.wert

import com.concordium.wallet.App
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object WertWidgetHelper {
    private const val BASE_URL = "https://widget.wert.io/01HM0W8FTFG4TEBRB0JPM18G5W/widget"

    fun getWidgetLink(sessionId: String): String {

        val sessionIdEncoded = encodeString(sessionId)
        val commodityEncoded = encodeString("CCD")
        val networkEncoded = encodeString("concordium")
        val layoutModeEncoded = encodeString("Modal")

        val commoditiesJson = App.appCore.gson.toJson(
            listOf(WertCommodityInfo(commodity = "CCD", network = "concordium"))
        )
        val commoditiesEncoded = encodeString(commoditiesJson)

        val url = "$BASE_URL?" +
                "session_id=$sessionIdEncoded&" +
                "commodity=$commodityEncoded&" +
                "network=$networkEncoded&" +
                "commodities=$commoditiesEncoded&" +
                "widget_layout_mode=$layoutModeEncoded"

        return url
    }

    private fun encodeString(input: String): String? {
        return URLEncoder.encode(input, StandardCharsets.UTF_8.toString())
    }
}