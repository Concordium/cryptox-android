package com.concordium.wallet.ui.account.accountsoverview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.concordium.wallet.databinding.AccountBalanceCardBinding

class AccountBalanceCardView(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    private val binding = AccountBalanceCardBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

}