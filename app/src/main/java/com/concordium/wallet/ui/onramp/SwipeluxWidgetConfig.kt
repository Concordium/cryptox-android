package com.concordium.wallet.ui.onramp

data class WidgetConfig(
    val apiKey: String,
    val colors: Colors
)

data class Colors(
    val main: String,
    val background: String,
    val processing: String,
    val warning: String,
    val success: String,
    val link: String
)