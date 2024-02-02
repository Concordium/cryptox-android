package com.concordium.wallet.ui.common.failed

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.backend.BackendError
import com.concordium.wallet.databinding.ActivityFailedBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.BackendErrorHandler

class FailedActivity : BaseActivity(
    R.layout.activity_failed,
    R.string.failed_title
) {

    companion object {
        const val EXTRA_ERROR = "EXTRA_ERROR"
        const val EXTRA_SOURCE = "EXTRA_SOURCE"
    }

    private lateinit var viewModel: FailedViewModel
    private val binding by lazy {
        ActivityFailedBinding.bind(findViewById(R.id.root_layout))
    }

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent.extras!!.getSerializable(EXTRA_SOURCE) as FailedViewModel.Source
        val error = intent.extras!!.getSerializable(EXTRA_ERROR) as BackendError?
        initializeViewModel()
        viewModel.initialize(source, error)
        initViews()
    }

    override fun onBackPressed() {
        // Ignore back press
    }

    // endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[FailedViewModel::class.java]
    }

    private fun initViews() {
        hideActionBarBack(false)

        when (viewModel.source) {
            FailedViewModel.Source.Identity -> {
                setActionBarTitle(R.string.identity_confirmed_title)
                binding.errorTitleTextview.setText(R.string.identity_confirmed_failed)
            }
            FailedViewModel.Source.Account -> {
                setActionBarTitle(R.string.new_account_confirmed_title)
                binding.errorTitleTextview.setText(R.string.new_account_confirmed_failed)
            }
            FailedViewModel.Source.Transfer -> {
                setActionBarTitle(R.string.send_funds_title)
                binding.errorTitleTextview.setText(R.string.send_funds_confirmed_failed)
            }
        }

        viewModel.error?.let { backendError ->
            if (backendError.errorMessage != null) {
                binding.errorTextview.setText(backendError.errorMessage)
            } else
            BackendErrorHandler.getExceptionStringResOrNull(backendError)?.let { stringRes ->
                binding.errorTextview.setText(stringRes)
            }
        }

        binding.confirmButton.setOnClickListener {
            finishFlow()
        }
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun finishFlow() {
        when (viewModel.source) {
            FailedViewModel.Source.Identity -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finishAffinity()
            }
            FailedViewModel.Source.Account -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finishAffinity()
            }
            FailedViewModel.Source.Transfer -> {
                val intent = Intent(this, AccountDetailsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }
        }
    }

    //endregion
}
