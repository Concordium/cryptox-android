package com.concordium.wallet.ui.airdrop

import com.concordium.wallet.data.room.Account

sealed class AirDropAction {

    data class Init(val airdropId: Long, val apiUrl: String) : AirDropAction()
    data class SendRegistration(val account: Account) : AirDropAction()
    object GetWallets : AirDropAction()
}