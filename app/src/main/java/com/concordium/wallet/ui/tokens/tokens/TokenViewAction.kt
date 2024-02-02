package com.concordium.wallet.ui.tokens.tokens

import com.concordium.wallet.data.model.WalletMeta

sealed class TokenViewAction {

    data class GetTokens(val walletMeta: WalletMeta, val offset: Int = 0) : TokenViewAction()
    data class GetNextTokens(val walletMeta: WalletMeta, val offset: Int = 0) : TokenViewAction()
}