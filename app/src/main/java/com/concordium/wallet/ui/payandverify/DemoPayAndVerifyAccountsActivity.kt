package com.concordium.wallet.ui.payandverify

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityDemoPayAndVerifyAccountsBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.getSerializable

class DemoPayAndVerifyAccountsActivity : BaseActivity(
    R.layout.activity_demo_pay_and_verify_accounts,
    R.string.pay_and_verify_accounts_title,
) {

    private val binding: ActivityDemoPayAndVerifyAccountsBinding by lazy {
        ActivityDemoPayAndVerifyAccountsBinding.bind(findViewById(R.id.root_layout))
    }
    private val viewModel: DemoPayAndVerifyAccountsViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        viewModel.initializeOnce(
            selectedAccountAddress = requireNotNull(
                intent.getStringExtra(
                    SELECTED_ACCOUNT_ADDRESS_EXTRA
                )
            ) {
                "No $SELECTED_ACCOUNT_ADDRESS_EXTRA specified"
            },
            invoice = requireNotNull(
                intent.getSerializable(
                    INVOICE_EXTRA,
                    DemoPayAndVerifyInvoice::class.java
                )
            ) {
                "No $INVOICE_EXTRA specified"
            },
        )

        hideActionBarBack(isVisible = true)

        initLoading()
        initList()
    }

    private fun initLoading() {

        viewModel.isLoading.collectWhenStarted(
            this,
            binding.loading.progressBar::isVisible::set,
        )
    }

    private fun initList() {

        val adapter = DemoPayAndVerifyAccountListItemAdapter(
            onItemClicked = { item ->
                setResult(
                    Activity.RESULT_OK,
                    Intent().putExtra(SELECTED_ACCOUNT_EXTRA, item.account)
                )
                finish()
            }
        )
        binding.recyclerview.adapter = adapter

        viewModel.accountItemList.collectWhenStarted(this) { accountItemList ->
            if (accountItemList != null) {
                adapter.setData(accountItemList)
            }
        }
    }

    companion object {
        private const val SELECTED_ACCOUNT_ADDRESS_EXTRA = "selected_account_address"
        private const val INVOICE_EXTRA = "invoice"
        private const val SELECTED_ACCOUNT_EXTRA = "selected_account"

        fun getBundle(
            selectedAccountAddress: String,
            invoice: DemoPayAndVerifyInvoice,
        ) = Bundle().apply {
            putString(SELECTED_ACCOUNT_ADDRESS_EXTRA, selectedAccountAddress)
            putSerializable(INVOICE_EXTRA, invoice)
        }

        fun getSelectedAccount(
            result: Intent,
        ): DemoPayAndVerifyAccount =
            requireNotNull(
                result.getSerializable(
                    SELECTED_ACCOUNT_EXTRA,
                    DemoPayAndVerifyAccount::class.java
                )
            ) {
                "No $SELECTED_ACCOUNT_EXTRA specified"
            }
    }
}
