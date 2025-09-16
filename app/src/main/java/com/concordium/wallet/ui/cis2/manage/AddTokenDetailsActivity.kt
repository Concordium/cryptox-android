package com.concordium.wallet.ui.cis2.manage

import android.os.Bundle
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.ActivityAddTokenDetailsBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.TokenDetailsView
import com.concordium.wallet.util.getSerializable

class AddTokenDetailsActivity : BaseActivity(
    R.layout.activity_add_token_details,
    R.string.cis_add_token_title
) {

    private val binding by lazy {
        ActivityAddTokenDetailsBinding.bind(findViewById(R.id.root))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideActionBarBack(isVisible = true)

        initViews(
            token = requireNotNull(intent.getSerializable(TOKEN, Token::class.java)),
        )
    }

    private fun initViews(token: Token) {
        TokenDetailsView(
            binding = binding.detailsLayout,
            fragmentManager = supportFragmentManager
        )
            .showTokenDetails(
                token = token,
                isHideVisible = false,
            )
    }

    companion object {
        const val TOKEN = "token_details"
    }
}
