package com.concordium.wallet.ui.payandverify

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityDemoPayAndVerifyBinding
import com.concordium.wallet.ui.base.BaseActivity

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
