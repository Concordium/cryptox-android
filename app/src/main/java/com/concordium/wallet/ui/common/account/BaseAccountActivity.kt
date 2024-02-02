package com.concordium.wallet.ui.common.account

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.ui.account.common.NewAccountViewModel
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.common.failed.FailedActivity
import com.concordium.wallet.ui.common.failed.FailedViewModel

abstract class BaseAccountActivity(
    layout: Int? = null,
    titleId: Int = R.string.app_name,
) : BaseActivity(layout, titleId), AuthDelegate by AuthDelegateImpl() {

    protected lateinit var viewModelNewAccount: NewAccountViewModel

    protected fun initializeNewAccountViewModel() {
        viewModelNewAccount = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[NewAccountViewModel::class.java]
    }

    protected fun initializeAuthenticationObservers() {
        viewModelNewAccount.showAuthenticationLiveData.observe(
            this,
            object : EventObserver<Boolean>() {
                override fun onUnhandledEvent(value: Boolean) {
                    if (value) showAuthentication()
                }
            })
        viewModelNewAccount.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModelNewAccount.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
        viewModelNewAccount.gotoAccountCreatedLiveData.observe(
            this,
            object : EventObserver<Account>() {
                override fun onUnhandledEvent(value: Account) {
                    accountCreated(value)
                }
            })
        viewModelNewAccount.gotoFailedLiveData.observe(
            this,
            object : EventObserver<Pair<Boolean, BackendError?>>() {
                override fun onUnhandledEvent(value: Pair<Boolean, BackendError?>) {
                    if (value.first) {
                        gotoFailed(value.second)
                    }
                }
            })
    }

    protected abstract fun showWaiting(waiting: Boolean)
    protected abstract fun accountCreated(account: Account)

    protected fun showAuthentication() {
        showAuthentication(
            activity = this@BaseAccountActivity,
            onAuthenticated = viewModelNewAccount::continueWithPassword
        )
    }
    protected fun gotoFailed(error: BackendError?) {
        val intent = Intent(this, FailedActivity::class.java)
        intent.putExtra(FailedActivity.EXTRA_SOURCE, FailedViewModel.Source.Account)
        error?.let {
            intent.putExtra(FailedActivity.EXTRA_ERROR, it)
        }
        startActivity(intent)
    }
}
