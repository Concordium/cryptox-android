package com.concordium.wallet.ui.onramp.swipelux

data class WidgetConfig(
    val apiKey: String,
    val colors: Colors,
    val defaultValues: DefaultValues
)

data class Colors(
    val main: String,
    val background: String,
    val processing: String,
    val warning: String,
    val success: String,
    val link: String
)

data class DefaultValue(
    val value: String,
    val editable: Boolean
)

data class DefaultValues(
    val targetAddress: DefaultValue?,
    val phone: DefaultValue?,
    val email: DefaultValue?,
    val fiatAmount: Int?
)