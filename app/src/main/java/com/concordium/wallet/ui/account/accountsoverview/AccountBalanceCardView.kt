package com.concordium.wallet.ui.account.accountsoverview

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.concordium.wallet.R
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.AccountBalanceCardBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.cis2.TokenDetailsActivity

class AccountBalanceCardView(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    private val binding = AccountBalanceCardBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    fun bind(viewModel: AccountBalanceViewModel) {
        val lifecycleOwner = findViewTreeLifecycleOwner()
            ?: error("Can't bind before the view has the lifecycle owner")

        viewModel.uiState.collectWhenStarted(lifecycleOwner) { state ->
            with(binding) {
                if (state.showBalanceInEur) {
                    state.eurBalance?.let {
                        balance.text = CurrencyUtil.toEURRate(
                            amount = state.totalBalance,
                            eurPerUnit = it
                        )
                        balanceAtDisposal.text = root.context.getString(
                            R.string.account_details_balance_eur_at_disposal,
                            CurrencyUtil.toEURRate(
                                amount = state.atDisposalBalance,
                                eurPerUnit = it
                            )
                        )
                    }
                    symbol.text = root.context.getString(
                        R.string.accounts_overview_balance_suffix_eur
                    )
                    changeRateText.text =
                        root.context.getString(
                            R.string.account_details_account_card_show_in_ccd
                        )
                } else {
                    balance.text = CurrencyUtil.formatAndRoundGTU(
                        value = state.totalBalance,
                        roundDecimals = 2,
                        decimals = 6,
                    )
                    balanceAtDisposal.text = root.context.getString(
                        R.string.account_details_balance_ccd_at_disposal,
                        CurrencyUtil.formatAndRoundGTU(
                            value = state.atDisposalBalance,
                            roundDecimals = 2,
                            decimals = 6,
                        )
                    )
                    symbol.text = root.context.getString(
                        R.string.accounts_overview_balance_suffix
                    )
                    changeRateText.text = root.context.getString(
                        R.string.account_details_account_card_show_in_eur
                    )
                }

                balanceAtDisposal.isVisible = state.totalBalance != state.atDisposalBalance
                stakingButton.isVisible = state.isStaking
                changeRateButton.setOnClickListener {
                    viewModel.onChangeCurrencyClicked()
                }

                if (state.account != null && state.ccdToken != null) {
                    tokenDetailsButton.setOnClickListener {
                        gotoTokenDetails(
                            state.ccdToken,
                            state.account
                        )
                    }
                } else {
                    tokenDetailsButton.setOnClickListener(null)
                }
            }
        }
    }

    private fun gotoTokenDetails(token: CCDToken, account: Account) {
        val intent = Intent(context, TokenDetailsActivity::class.java)
        intent.putExtra(TokenDetailsActivity.ACCOUNT, account)
        intent.putExtra(TokenDetailsActivity.TOKEN, token)
        context.startActivity(intent)
    }
}