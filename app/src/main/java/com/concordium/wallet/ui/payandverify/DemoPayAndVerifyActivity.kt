package com.concordium.wallet.ui.payandverify

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.airbnb.lottie.LottieDrawable
import com.concordium.wallet.R
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityDemoPayAndVerifyBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.util.ImageUtil
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DemoPayAndVerifyActivity : BaseActivity(
    R.layout.activity_demo_pay_and_verify,
    R.string.pay_and_verify_title,
), AuthDelegate by AuthDelegateImpl() {

    private val binding: ActivityDemoPayAndVerifyBinding by lazy {
        ActivityDemoPayAndVerifyBinding.bind(findViewById(R.id.toastLayoutTopError))
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

        initLoading()
        initRejectButton()
        initVerifyAndPayButton()
        initInvoiceDetails()
        initAccountSelection()

        subscribeToEvents()
    }

    private fun initLoading() {

        viewModel.isLoading.collectWhenStarted(
            this,
            binding.loading.progressBar::isVisible::set,
        )
    }

    private fun initRejectButton() {

        combine(
            viewModel.invoice,
            viewModel.paymentStatus,
            transform = ::Pair,
        ).collectWhenStarted(this) { (invoice, paymentResult) ->
            binding.rejectButton.isInvisible = invoice == null || paymentResult != null
        }

        binding.rejectButton.setOnClickListener {
            finish()
        }
    }

    private fun initVerifyAndPayButton() {

        viewModel.canPayAndVerify.collectWhenStarted(this) { canPayAndVerify ->
            if (!canPayAndVerify) {
                binding.verifyAndPayButton.isEnabled = false
                binding.verifyAndPayButton.alpha = 0.5f
            } else {
                binding.verifyAndPayButton.isEnabled = true
                binding.verifyAndPayButton.alpha = 1f
            }
        }

        binding.verifyAndPayButton.setText(getString(R.string.verify_and_pay))

        binding.verifyAndPayButton.setOnSliderCompleteListener(
            viewModel::onVerifyAndPayClicked
        )

        binding.closeButton.setOnClickListener {
            finish()
        }

        viewModel.paymentStatus.collectWhenStarted(this) { paymentResult ->
            binding.verifyAndPayButton.isVisible = paymentResult == null
            binding.closeButton.isVisible = paymentResult != null
        }
    }

    private fun initInvoiceDetails() {

        viewModel.paymentStatus.collectWhenStarted(this) { paymentResult ->
            if (paymentResult == null) {
                binding.transactionAnimation.isVisible = false
                binding.animationDivider.isVisible = false
                return@collectWhenStarted
            }

            binding.transactionAnimation.isVisible = true
            binding.animationDivider.isVisible = true

            when (paymentResult) {
                is DemoPayAndVerifyViewModel.PaymentStatus.Failure -> {
                    binding.transactionAnimation.setAnimation(R.raw.transaction_fail)
                    binding.transactionAnimation.repeatCount = 0
                }

                is DemoPayAndVerifyViewModel.PaymentStatus.Success -> {
                    binding.transactionAnimation.setAnimation(R.raw.transaction_success)
                    binding.transactionAnimation.repeatCount = 0
                }

                DemoPayAndVerifyViewModel.PaymentStatus.Sending -> {
                    binding.transactionAnimation.setAnimation(R.raw.transaction_loading)
                    binding.transactionAnimation.repeatCount = LottieDrawable.INFINITE
                }
            }
            binding.transactionAnimation.playAnimation()
        }

        combine(
            viewModel.invoice,
            viewModel.paymentStatus,
            transform = ::Pair,
        ).collectWhenStarted(this)
        { (invoice, paymentResult) ->
            if (invoice == null) {
                binding.invoiceDetailsCard.isVisible = false
                return@collectWhenStarted
            }

            val cis2PaymentDetails =
                invoice.paymentDetails as DemoPayAndVerifyInvoice.PaymentDetails.Cis2

            binding.invoiceDetailsCard.isVisible = true

            binding.titleTextView.text = when (paymentResult) {
                is DemoPayAndVerifyViewModel.PaymentStatus.Failure ->
                    getString(
                        R.string.template_failed_payment_and_verification_request,
                        invoice.storeName,
                    )

                is DemoPayAndVerifyViewModel.PaymentStatus.Success ->
                    getString(
                        R.string.template_successful_payment_and_verification_request,
                        invoice.storeName,
                    )

                else ->
                    getString(
                        R.string.template_payment_and_verification_request,
                        invoice.storeName,
                    )
            }

            binding.amountTitleTextView.text = when (paymentResult) {
                is DemoPayAndVerifyViewModel.PaymentStatus.Success ->
                    getString(R.string.amount_paid)

                else ->
                    getString(R.string.amount_requested)
            }

            @SuppressLint("SetTextI18n")
            binding.amountTextView.text =
                CurrencyUtil.formatGTU(
                    value = cis2PaymentDetails.amount,
                    decimals = cis2PaymentDetails.tokenDecimals,
                ) + " " + cis2PaymentDetails.tokenSymbol

            binding.verificationTitleTextView.text = when (paymentResult) {
                is DemoPayAndVerifyViewModel.PaymentStatus.Success ->
                    getString(R.string.verification_performed)

                else ->
                    getString(R.string.verification_requested)
            }

            binding.verificationTextView.text = getString(
                R.string.template_over_years_old,
                invoice.minAgeYears,
            )
        }

        viewModel.fee.collectWhenStarted(this) { fee ->
            if (fee == null) {
                binding.feeTextView.isVisible = false
                return@collectWhenStarted
            }

            binding.feeTextView.isVisible = true
            binding.feeTextView.text = getString(
                R.string.cis_estimated_fee,
                CurrencyUtil.formatGTU(fee.cost)
            )
        }
    }

    private fun initAccountSelection() {

        combine(
            viewModel.selectedAccount,
            viewModel.paymentStatus,
            transform = ::Pair
        ).collectWhenStarted(this) { (selectedAccount, paymentStatus) ->
            if (selectedAccount == null || paymentStatus != null) {
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

        viewModel.selectedAccountErrors.collectWhenStarted(this) { selectedAccountErrors ->

            if (selectedAccountErrors.isEmpty()) {
                binding.accountErrorsLayout.isVisible = false
                return@collectWhenStarted
            }

            binding.accountErrorsLayout.isVisible = true
            binding.accountErrorsLayout.forEach {
                it.isVisible = false
            }

            selectedAccountErrors.forEach { error ->
                when (error) {
                    DemoPayAndVerifyViewModel.SelectedAccountError.InsufficientBalance -> {
                        binding.insufficientBalanceTextView.isVisible = true
                    }

                    is DemoPayAndVerifyViewModel.SelectedAccountError.Underage -> {
                        binding.underageTextView.isVisible = true
                        binding.underageTextView.text = getString(
                            R.string.template_error_underage,
                            error.minAgeYears,
                        )
                    }
                }
            }
        }

        viewModel.selectedAccountErrors
            .map(List<*>::isNotEmpty)
            .distinctUntilChanged()
            .collectWhenStarted(this) { hasAccountErrors ->
                if (!hasAccountErrors) {
                    binding.accountContainer.backgroundTintList = null
                    binding.accountView.accountName.setTextColor(
                        ContextCompat.getColor(this, R.color.cryptox_white_main)
                    )
                    binding.accountView.identityName.setTextColor(
                        ContextCompat.getColor(this, R.color.mw24_blue_3)
                    )
                    binding.accountView.amountTextView.setTextColor(
                        ContextCompat.getColor(this, R.color.mw24_blue_3)
                    )
                    binding.accountView.tokenSymbolTextView.setTextColor(
                        ContextCompat.getColor(this, R.color.mw24_blue_3)
                    )
                    binding.accountArrowButton.imageTintList = ColorStateList.valueOf(Color.WHITE)
                } else {
                    binding.accountContainer.backgroundTintList =
                        ColorStateList.valueOf(Color.WHITE)
                    val blackColor = ContextCompat.getColor(this, R.color.mw24_black_1)
                    binding.accountView.accountName.setTextColor(blackColor)
                    binding.accountView.identityName.setTextColor(blackColor)
                    binding.accountView.amountTextView.setTextColor(blackColor)
                    binding.accountView.tokenSymbolTextView.setTextColor(blackColor)
                    binding.accountArrowButton.imageTintList = ColorStateList.valueOf(blackColor)
                }
            }
    }

    private fun subscribeToEvents(
    ) = viewModel.events.collectWhenStarted(this) { event ->

        when (event) {
            DemoPayAndVerifyViewModel.Event.Authenticate ->
                showAuthentication(
                    activity = this,
                    onAuthenticated = viewModel::onAuthenticated,
                )

            is DemoPayAndVerifyViewModel.Event.ShowFloatingError ->
                showError(event.message)

            is DemoPayAndVerifyViewModel.Event.FinishWithError -> {
                Toast.makeText(this, event.message, Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
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
