package com.concordium.wallet.ui.bakerdelegation.common

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.bakerdelegation.dialog.NoChangeDialog
import com.concordium.wallet.ui.bakerdelegation.dialog.NotEnoughFundsDialog
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl

abstract class BaseDelegationBakerActivity(
    layout: Int?,
    titleId: Int,
) : BaseActivity(layout, titleId), AuthDelegate by AuthDelegateImpl() {
    protected lateinit var viewModel: DelegationBakerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideActionBarBack(isVisible = true)
        initializeViewModel()
        viewModel.initialize(intent.extras?.getSerializable(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA) as BakerDelegationData)
    }

    open fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[DelegationBakerViewModel::class.java]
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
        estimatedTransactionFee: TextView
    ) {
        viewModel.transactionFeeLiveData.observe(this) { response ->
            response?.first?.let {
                estimatedTransactionFee.text =
                    getString(
                        R.string.delegation_register_delegation_amount_estimated_transaction_fee,
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

    protected fun showNotEnoughFunds() {
        NotEnoughFundsDialog().showSingle(supportFragmentManager, NotEnoughFundsDialog.TAG)
    }

    protected fun showNoChange() {
        NoChangeDialog().showSingle(supportFragmentManager, NoChangeDialog.TAG)
    }

    protected open fun initViews() {}
}
