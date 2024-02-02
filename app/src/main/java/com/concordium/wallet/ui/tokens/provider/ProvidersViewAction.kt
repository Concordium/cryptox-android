package com.concordium.wallet.ui.tokens.provider

sealed class ProvidersViewAction {

    object GetTokens : ProvidersViewAction()
    object GetAccount : ProvidersViewAction()
    data class GetProviders(val providers: List<ProviderMeta>) : ProvidersViewAction()
}