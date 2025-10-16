package com.concordium.wallet.ui.bakerdelegation.common

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.account.earn.EarnLegalDisclaimerDialog
import com.concordium.wallet.ui.bakerdelegation.dialog.NoChangeDialog
import com.concordium.wallet.ui.bakerdelegation.dialog.NotEnoughFundsForFeeDialog
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import org.koin.androidx.viewmodel.ext.android.viewModel

abstract class BaseDelegationBakerActivity(
    layout: Int?,
    titleId: Int,
) : BaseActivity(layout, titleId), AuthDelegate by AuthDelegateImpl() {
    protected val viewModel: DelegationBakerViewModel by viewModel()

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideActionBarBack(isVisible = true)
        viewModel.initialize(
            intent.extras?.getSerializable(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA)
                    as BakerDelegationData
        )
    }

    protected fun initializeWaitingLiveData(progressLayout: View) {
        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(progressLayout, waiting)
            }
        }
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                errorLiveData(value)
            }
        })
    }

    protected fun initializeTransactionFeeLiveData(
        progressLayout: View,
        estimatedTransactionFee: TextView,
    ) {
        viewModel.transactionFeeLiveData.observe(this) { response ->
            response?.first?.let {
                estimatedTransactionFee.text =
                    getString(
                        R.string.cis_estimated_fee,
                        CurrencyUtil.formatGTU(it)
                    )
                showWaiting(progressLayout, false)
            }
        }
        viewModel.loadTransactionFee(false)
    }

    protected fun initializeShowAuthenticationLiveData() {
        viewModel.showAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showAuthentication(
                        activity = this@BaseDelegationBakerActivity,
                        onAuthenticated = viewModel::continueWithPassword
                    )
                }
            }
        })
    }

    abstract fun errorLiveData(value: Int)

    protected open fun showWaiting(progressLayout: View, waiting: Boolean) {
        if (waiting) {
            progressLayout.visibility = View.VISIBLE
        } else {
            progressLayout.visibility = View.GONE
        }
    }

    protected fun showNotEnoughFundsForFee() {
        NotEnoughFundsForFeeDialog().showSingle(
            supportFragmentManager,
            NotEnoughFundsForFeeDialog.TAG
        )
    }

    protected fun showNoChange() {
        NoChangeDialog().showSingle(supportFragmentManager, NoChangeDialog.TAG)
    }

    protected open fun initViews() {}

    protected fun showLegalDisclaimerDialog() {
        EarnLegalDisclaimerDialog().showSingle(
            supportFragmentManager,
            EarnLegalDisclaimerDialog.TAG
        )
    }
}
