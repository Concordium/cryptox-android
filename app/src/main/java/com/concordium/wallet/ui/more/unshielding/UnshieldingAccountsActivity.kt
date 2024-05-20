package com.concordium.wallet.ui.more.unshielding

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.ActivityUnshieldingAccountsBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.util.Log

class UnshieldingAccountsActivity : BaseActivity(
    R.layout.activity_unshielding_accounts,
    R.string.accounts_overview_title
), AuthDelegate by AuthDelegateImpl() {

    private val binding by lazy {
        ActivityUnshieldingAccountsBinding.bind(findViewById(R.id.toastLayoutTopError))
    }
    private lateinit var viewModel: UnshieldingAccountsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViewModel()
        initList()

        hideActionBarBack(isVisible = true)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[UnshieldingAccountsViewModel::class.java]

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }

        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })

        viewModel.showAuthLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                showAuthentication(
                    activity = this@UnshieldingAccountsActivity,
                    onCanceled = ::finish,
                    onAuthenticated = viewModel::onAuthenticated
                )
            }
        })

        viewModel.goToUnshieldingLiveData.observe(this, object : EventObserver<Account>() {
            override fun onUnhandledEvent(value: Account) {
                goToUnshielding(value)
            }
        })
    }

    private fun initList() {
        val adapter = UnshieldingAccountItemAdapter(
            onUnshieldClicked = viewModel::onUnshieldClicked,
        )
        binding.recyclerview.adapter = adapter
        viewModel.listItemsLiveData.observe(this, adapter::setData)
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    private fun goToUnshielding(account: Account) {
        Log.d("Go to unshielding: ${account.totalShieldedBalance}")
    }
}
