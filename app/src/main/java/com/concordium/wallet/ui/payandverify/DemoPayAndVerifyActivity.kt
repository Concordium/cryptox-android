package com.concordium.wallet.ui.payandverify

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityDemoPayAndVerifyBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.util.ImageUtil

class DemoPayAndVerifyActivity : BaseActivity(
    R.layout.activity_demo_pay_and_verify,
    R.string.pay_and_verify_title,
) {
    private val binding: ActivityDemoPayAndVerifyBinding by lazy {
        ActivityDemoPayAndVerifyBinding.bind(findViewById(R.id.root_layout))
    }
    private val viewModel: DemoPayAndVerifyViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.initializeOnce(
            invoiceUrl = intent.getStringExtra(INVOICE_URL_EXTRA)
                ?: error("No $INVOICE_URL_EXTRA specified"),
        )

        initAccountSelection()
    }

    private fun initAccountSelection() {

        viewModel.selectedAccount.collectWhenStarted(this) { selectedAccount ->
            if (selectedAccount == null) {
                binding.accountContainer.isVisible = false
                return@collectWhenStarted
            }

            binding.accountContainer.isVisible = true

            binding.accountView.accountName.text = selectedAccount.account.getAccountName()
            binding.accountView.identityName.text = selectedAccount.identity.name
            binding.accountView.accountIcon.setImageDrawable(
                ImageUtil.getIconById(
                    context = this,
                    id = selectedAccount.account.iconId,
                )
            )
            binding.accountView.amountTextView.text = CurrencyUtil.formatGTU(
                value = selectedAccount.balance,
                decimals = selectedAccount.tokenDecimals,
            )
            binding.accountView.tokenSymbolTextView.text = selectedAccount.tokenSymbol
            binding.accountView.notValidBadge.isVisible = false
        }
    }

    companion object {
        const val INVOICE_URL_EXTRA = "invoice_url"

        fun getBundle(
            invoiceUrl: String,
        ) = Bundle().apply {
            putString(INVOICE_URL_EXTRA, invoiceUrl)
        }
    }
}
