package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REGISTER_BAKER
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityBakerRegistrationAmountBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerRegisterAmountActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.bakerdelegation.common.StakeAmountInputValidator
import com.concordium.wallet.ui.bakerdelegation.dialog.WarningDialog
import com.concordium.wallet.ui.common.BackendErrorHandler
import com.concordium.wallet.util.KeyboardUtil.showKeyboard
import java.math.BigInteger

class BakerRegisterAmountActivity : BaseDelegationBakerRegisterAmountActivity(
    R.layout.activity_baker_registration_amount, R.string.baker_registration_amount_title
) {
    private var minFee: BigInteger? = null
    private var maxFee: BigInteger? = null
    private var singleFee: BigInteger? = null

    companion object {
        private const val RANGE_MIN_FEE = 1
        private const val RANGE_MAX_FEE = 2
        private const val SINGLE_FEE = 3
    }

    private lateinit var binding: ActivityBakerRegistrationAmountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBakerRegistrationAmountBinding.bind(findViewById(R.id.root_layout))
        hideActionBarBack(isVisible = true)
        initViews()
        initObservers()
    }

    override fun initViews() {
        super.initViews()
        super.initReStakeOptionsView(binding.restakeOptions)

        if (viewModel.bakerDelegationData.isUpdateBaker()) {
            setActionBarTitle(R.string.baker_registration_update_amount_title)
            binding.amount.setText(
                CurrencyUtil.formatGTU(
                    value = viewModel.bakerDelegationData.account.baker?.stakedAmount
                        ?: BigInteger.ZERO,
                    withCommas = false
                )
            )
        }

        binding.balanceAmount.text = CurrencyUtil.formatGTU(viewModel.getAvailableBalance())

        viewModel.transactionFeeLiveData.observe(this) { response ->
            response?.second?.let { requestId ->
                when (requestId) {
                    SINGLE_FEE -> {
                        singleFee = response.first
                        validateFee = response.first
                    }

                    RANGE_MIN_FEE -> minFee = response.first
                    RANGE_MAX_FEE -> {
                        maxFee = response.first
                        validateFee = response.first
                    }
                }
                singleFee?.let {
                    binding.poolEstimatedTransactionFee.visibility = View.VISIBLE
                    binding.poolEstimatedTransactionFee.text = getString(
                        R.string.cis_estimated_fee,
                        CurrencyUtil.formatGTU(singleFee ?: BigInteger.ZERO)
                    )
                } ?: run {
                    if (minFee != null && maxFee != null) {
                        binding.poolEstimatedTransactionFee.visibility = View.VISIBLE
                        binding.poolEstimatedTransactionFee.text = getString(
                            R.string.cis_estimated_fee,
                            CurrencyUtil.formatGTU(maxFee ?: BigInteger.ZERO)
                        )
                    }
                }
            }
        }

        viewModel.chainParametersPassiveDelegationBakerPoolLoaded.observe(this) { success ->
            success?.let {
                updateViews()
                showWaiting(binding.includeProgress.progressLayout, false)
            }
        }

        viewModel.eurRateLiveData.observe(this) { rate ->
            binding.eurRate.text =
                if (rate != null)
                    getString(
                        R.string.cis_estimated_eur_rate,
                        rate
                    )
                else
                    ""
        }

        showWaiting(binding.includeProgress.progressLayout, true)

        loadTransactionFee()

        try {
            viewModel.loadChainParametersPassiveDelegationAndPossibleBakerPool()
        } catch (ex: Exception) {
            handleBackendError(ex)
        }
    }

    private fun initObservers() {
        supportFragmentManager.setFragmentResultListener(
            WarningDialog.ACTION_REQUEST,
            this
        ) { _, bundle ->
            if (WarningDialog.getResult(bundle)) {
                gotoNextPage()
            }
        }
    }

    private fun handleBackendError(throwable: Throwable) {
        val stringRes = BackendErrorHandler.getExceptionStringRes(throwable)
        runOnUiThread {
            popup.showSnackbar(binding.root, stringRes)
        }
    }

    private fun updateViews() {
        binding.amount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onContinueClicked()
            }
            false
        }
        setAmountHint(binding.amount)
        binding.amount.addTextChangedListener {
            validateAmountInput(binding.amount, binding.amountError)
            if (it.toString().isEmpty()) {
                binding.balanceSymbol.alpha = 0.5f
            } else {
                binding.balanceSymbol.alpha = 1f
            }
            CurrencyUtil.toGTUValue(binding.amount.text.toString())?.let { amount ->
                viewModel.loadEURRate(amount)
            }
        }
        binding.amount.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showKeyboard(this, binding.amount)
            }
        }
        binding.balanceSymbol.setOnClickListener {
            showKeyboard(this, binding.amount)
        }
        binding.maxAmountButton.setOnClickListener {
            binding.amount.setText(CurrencyUtil.formatGTU(viewModel.getMaxDelegationBalance()))
        }
        binding.bakerRegistrationContinue.setOnClickListener {
            onContinueClicked()
        }

        if (viewModel.isInCoolDown()) {
            binding.amount.isEnabled = false
        }
    }

    override fun loadTransactionFee() {
        when (viewModel.bakerDelegationData.type) {
            REGISTER_BAKER -> {
                viewModel.loadTransactionFee(
                    true,
                    requestId = RANGE_MIN_FEE,
                    metadataSizeForced = 0
                )
                viewModel.loadTransactionFee(
                    true,
                    requestId = RANGE_MAX_FEE,
                    metadataSizeForced = 2048
                )
            }

            else -> viewModel.loadTransactionFee(
                true,
                requestId = SINGLE_FEE,
                metadataSizeForced = viewModel.bakerDelegationData.account.baker?.bakerPoolInfo?.metadataUrl?.length
            )
        }
    }

    override fun getStakeAmountInputValidator(): StakeAmountInputValidator {
        return StakeAmountInputValidator(
            minimumValue = viewModel.bakerDelegationData.chainParameters?.minimumEquityCapital,
            maximumValue = viewModel.getStakeInputMax(),
            oldStakedAmount = viewModel.bakerDelegationData.oldStakedAmount,
            balance = viewModel.bakerDelegationData.account.balance,
            atDisposal = viewModel.bakerDelegationData.account.balanceAtDisposal,
            currentPool = viewModel.bakerDelegationData.bakerPoolStatus?.delegatedCapital,
            poolLimit = null,
            previouslyStakedInPool = viewModel.bakerDelegationData.account.delegation?.stakedAmount,
            isInCoolDown = viewModel.isInCoolDown(),
            oldPoolId = viewModel.bakerDelegationData.account.delegation?.delegationTarget?.bakerId,
            newPoolId = viewModel.bakerDelegationData.poolId
        )
    }

    override fun showError(stakeError: StakeAmountInputValidator.StakeError?) {
        binding.amountError.visibility = View.VISIBLE
    }

    override fun hideError() {
        binding.amountError.visibility = View.GONE
    }

    override fun errorLiveData(value: Int) {
    }

    private fun onContinueClicked() {
        if (!binding.bakerRegistrationContinue.isEnabled) return

        val amountToStake = CurrencyUtil.toGTUValue(binding.amount.text.toString()) ?: BigInteger.ZERO
        val stakeAmountInputValidator = getStakeAmountInputValidator()
        val stakeError = stakeAmountInputValidator.validate(amountToStake, validateFee)
        if (stakeError != StakeAmountInputValidator.StakeError.OK) {
            binding.amountError.text = stakeAmountInputValidator.getErrorText(this, stakeError)
            showError(stakeError)
            return
        }

        if (viewModel.bakerDelegationData.isUpdateBaker()) {
            when {
                amountToStake == viewModel.bakerDelegationData.oldStakedAmount && viewModel.bakerDelegationData.restake == viewModel.bakerDelegationData.oldRestake -> showNoChange()
                moreThan95Percent(amountToStake) -> show95PercentWarning()
                else -> gotoNextPage()
            }
        } else {
            when {
                moreThan95Percent(amountToStake) -> show95PercentWarning()
                else -> gotoNextPage()
            }
        }
    }

    private fun show95PercentWarning() {
        WarningDialog.newInstance(
            WarningDialog.setBundle(
                title = getString(R.string.baker_more_than_95_title),
                description = getString(R.string.baker_more_than_95_message),
                confirmButton = getString(R.string.baker_more_than_95_continue),
                denyButton = getString(R.string.baker_more_than_95_new_stake)
            )
        ).showSingle(supportFragmentManager, WarningDialog.TAG)
    }

    private fun gotoNextPage() {
        viewModel.bakerDelegationData.amount =
            CurrencyUtil.toGTUValue(binding.amount.text.toString())
        val intent = if (viewModel.bakerDelegationData.isUpdateBaker())
            Intent(this, BakerRegistrationConfirmationActivity::class.java)
        else
            Intent(this, BakerRegistrationActivity::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }
}
