package com.concordium.wallet.ui.tokens.provider

import android.os.Bundle
import com.concordium.wallet.R
import com.concordium.wallet.ui.base.BaseActivity

class NFTActivity : BaseActivity(R.layout.activity_nft, R.string.app_menu_item_tokens) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)
    }
}