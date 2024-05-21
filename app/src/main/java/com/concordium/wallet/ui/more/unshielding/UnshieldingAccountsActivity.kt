package com.concordium.wallet.ui.more.unshielding

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.databinding.ActivityUnshieldingAccountsBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl

class UnshieldingAccountsActivity : BaseActivity(
    R.layout.activity_unshielding_accounts,
    R.string.accounts_overview_title
), AuthDelegate by AuthDelegateImpl() {

    private val binding by lazy {
        ActivityUnshieldingAccountsBinding.bind(findViewById(R.id.toastLayoutTopError))
    }
    private lateinit var viewModel: UnshieldingAccountsViewModel
    private val unshieldingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::onUnshieldingResult,
    )

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
        ).get()

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

        viewModel.openUnshieldingLiveData.observe(this, object : EventObserver<String>() {
            override fun onUnhandledEvent(value: String) {
                openUnshielding(value)
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

    private fun openUnshielding(accountAddress: String) {
        unshieldingLauncher.launch(
            Intent(this, UnshieldingActivity::class.java).putExtras(
                UnshieldingActivity.getBundle(
                    accountAddress = accountAddress,
                )
            )
        )
    }

    private fun onUnshieldingResult(unshieldingResult: ActivityResult) {
        if (unshieldingResult.resultCode == Activity.RESULT_OK) {
            val resultBundle = checkNotNull(unshieldingResult.data?.extras) {
                "The result has no bundle"
            }
            viewModel.onUnshieldingResult(UnshieldingActivity.getResult(resultBundle))
        }
    }
}
