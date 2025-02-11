package com.concordium.wallet.ui.more.unshielding

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityUnshieldingBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl

class UnshieldingActivity : BaseActivity(
    R.layout.activity_unshielding,
    R.string.accounts_overview_title
), AuthDelegate by AuthDelegateImpl() {

    private val binding by lazy {
        ActivityUnshieldingBinding.bind(findViewById(R.id.toastLayoutTopError))
    }
    private lateinit var viewModel: UnshieldingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViewModel()
        initButtons()

        hideActionBarBack(isVisible = true)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()

        viewModel.initializeOnce(
            accountAddress = requireNotNull(intent.getStringExtra(ACCOUNT_ADDRESS_EXTRA)) {
                "No $ACCOUNT_ADDRESS_EXTRA specified"
            }
        )

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
                    activity = this@UnshieldingActivity,
                    onAuthenticated = viewModel::onAuthenticated
                )
            }
        })

        viewModel.titleLiveData.observe(this, this::setActionBarTitle)

        viewModel.amountLiveData.observe(this) { amount ->
            binding.shieldedAmountTextView.text = CurrencyUtil.formatGTU(amount)
        }

        viewModel.transactionCostLiveData.observe(this) { cost ->
            binding.feeTextView.text =
                getString(R.string.amount, CurrencyUtil.formatGTU(cost))
        }

        viewModel.insufficientFundsLiveData.observe(
            this,
            binding.insufficientFundsTextView::isVisible::set,
        )

        viewModel.isUnshieldEnabledLiveData.observe(this, binding.unshieldButton::setEnabled)

        viewModel.finishWithResultLiveData.observe(
            this,
            object : EventObserver<UnshieldingResult>() {
                override fun onUnhandledEvent(value: UnshieldingResult) {
                    setResult(Activity.RESULT_OK, Intent().putExtras(createResult(value)))
                    finish()
                }
            }
        )
    }

    private fun initButtons() {
        binding.unshieldButton.setOnClickListener {
            viewModel.onUnshieldClicked()
        }
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    companion object {
        const val ACCOUNT_ADDRESS_EXTRA = "account_address"
        private const val UNSHIELDED_RESULT_EXTRA = "unshielded_result"

        private fun createResult(unshieldingResult: UnshieldingResult) = Bundle().apply {
            putSerializable(UNSHIELDED_RESULT_EXTRA, unshieldingResult)
        }

        fun getResult(bundle: Bundle): UnshieldingResult =
            checkNotNull(bundle.getSerializable(UNSHIELDED_RESULT_EXTRA) as? UnshieldingResult) {
                "No $UNSHIELDED_RESULT_EXTRA specified"
            }

        fun getBundle(accountAddress: String) = Bundle().apply {
            putString(ACCOUNT_ADDRESS_EXTRA, accountAddress)
        }
    }
}
