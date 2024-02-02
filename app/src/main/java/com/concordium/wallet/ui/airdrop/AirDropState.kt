package com.concordium.wallet.ui.airdrop

sealed class AirDropState {

    object ConnectConfirm : AirDropState()
    object SelectWallet : AirDropState()
    object ShowResult : AirDropState()
}