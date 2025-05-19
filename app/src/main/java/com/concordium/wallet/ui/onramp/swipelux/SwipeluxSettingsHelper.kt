package com.concordium.wallet.ui.onramp.swipelux

import com.concordium.wallet.App
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object SwipeluxSettingsHelper {
    private const val BASE_URL = "https://track.swipelux.com"

    fun getWidgetSettings(walletAddress: String): String {

        val widgetConfig = WidgetConfig(
            apiKey = "a71770a8-07ce-4f46-8085-373229bee148", //CCD API Key
            colors = Colors(
                main = "#48A2AE",
                background = "#182022",
                processing = "#FFA400",
                warning = "#ED0A34",
                success = "#58CB4E",
                link = "#F24F21"
            ),
            defaultValues = DefaultValues(
                targetAddress = DefaultValue(
                    value = walletAddress,
                    editable = true
                ),
                phone = DefaultValue(
                    value = "",
                    editable = true
                ),
                email = DefaultValue(
                    value = "",
                    editable = true
                ),
                fiatAmount = 100
            )
        )

        val jsonString = App.appCore.gson.toJson(widgetConfig)
        val encodedString = URLEncoder.encode(jsonString, StandardCharsets.UTF_8.toString())
        val url = "$BASE_URL/?specificSettings=${encodedString}"

        return url
    }
}
