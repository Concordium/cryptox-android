package com.concordium.wallet.data.walletconnect

import com.concordium.wallet.App

data class SignMessageParams(
    val message: String,
) {
    companion object {
        fun fromSessionRequestParams(signMessageParams: String): SignMessageParams {
            return App.appCore.gson.fromJson(signMessageParams, SignMessageParams::class.java)
        }
    }
}
