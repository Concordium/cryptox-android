package com.concordium.wallet.ui.multiwallet

import com.concordium.wallet.core.multiwallet.AppWallet

sealed interface WalletListItem {

    class Wallet(
        val type: AppWallet.Type,
        val isSelected: Boolean,
        val source: AppWallet?,
    ): WalletListItem {
        constructor(
            appWallet: AppWallet,
            isSelected: Boolean,
        ) : this(
            type = appWallet.type,
            isSelected = isSelected,
            source = appWallet,
        )
    }

    class AddButton(
        val walletType: AppWallet.Type,
    ) : WalletListItem
}
