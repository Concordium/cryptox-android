package com.concordium.wallet.ui.bakerdelegation.common

import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.uicore.view.AmountEditText
import java.math.BigDecimal
import java.math.BigInteger

abstract class BaseDelegationBakerRegisterAmountActivity(
    layout: Int,
    titleId: Int
) : BaseDelegationBakerActivity(layout, titleId) {
    protected var validateFee: BigInteger? = null
    protected var baseDelegationBakerRegisterAmountListener: BaseDelegationBakerRegisterAmountListener? =
        null

    interface BaseDelegationBakerRegisterAmountListener {
        fun onReStakeChanged()
    }

    protected fun initReStakeOptionsView(reStakeOptions: SwitchCompat) {
        val initiallyReStake = if (viewModel.bakerDelegationData.isBakerFlow()) {
            viewModel.bakerDelegationData.account.baker?.restakeEarnings == true || viewModel.bakerDelegationData.account.baker?.restakeEarnings == null
        } else {
            viewModel.bakerDelegationData.account.delegation?.restakeEarnings == true || viewModel.bakerDelegationData.account.delegation?.restakeEarnings == null
        }
        viewModel.bakerDelegationData.restake = initiallyReStake

        reStakeOptions.isChecked = initiallyReStake
        reStakeOptions.setOnClickListener {
            viewModel.markRestake(reStakeOptions.isChecked)
            baseDelegationBakerRegisterAmountListener?.onReStakeChanged()
        }
    }

    protected fun moreThan95Percent(amountToStake: BigInteger): Boolean {
        return amountToStake.toBigDecimal() > viewModel.bakerDelegationData.account.balance.toBigDecimal() * BigDecimal(0.95)
    }

    protected fun validateAmountInput(amount: AmountEditText, amountError: TextView) {
        setAmountHint(amount)
        if (amount.text.toString().isNotBlank()) {
            val stakeAmountInputValidator = getStakeAmountInputValidator()
            val stakeError = stakeAmountInputValidator.validate(
                CurrencyUtil.toGTUValue(amount.text.toString()),
                validateFee,
            )
            if (stakeError != StakeAmountInputValidator.StakeError.OK) {
                amountError.text = stakeAmountInputValidator.getErrorText(this, stakeError)
                showError(stakeError)
            } else {
                hideError()
                loadTransactionFee()
            }
        } else {
            hideError()
        }
    }

    protected fun setAmountHint(amount: AmountEditText) {
        when {
            amount.text.isNotEmpty() -> {
                amount.hint = ""
            }

            else -> {
                amount.hint = BigInteger.ZERO.toString()
            }
        }
    }

    abstract fun getStakeAmountInputValidator(): StakeAmountInputValidator

    abstract fun showError(stakeError: StakeAmountInputValidator.StakeError?)
    abstract fun hideError()
    abstract fun loadTransactionFee()
}
