package com.concordium.wallet.ui.bakerdelegation.delegation

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_DELEGATION
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityDelegationRegistrationAmountBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerRegisterAmountActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.bakerdelegation.common.StakeAmountInputValidator
import com.concordium.wallet.ui.bakerdelegation.dialog.WarningDialog
import com.concordium.wallet.util.KeyboardUtil.showKeyboard
import com.concordium.wallet.util.getSerializable
import java.math.BigInteger

class DelegationRegisterAmountActivity : BaseDelegationBakerRegisterAmountActivity(
    R.layout.activity_delegation_registration_amount,
    R.string.delegation_register_delegation_title
) {
    private lateinit var binding: ActivityDelegationRegistrationAmountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDelegationRegistrationAmountBinding.bind(findViewById(R.id.root_layout))
        hideActionBarBack(isVisible = true)
        initViews()
        initObservers()
    }

    override fun onResume() {
        super.onResume()
        checkDelegationType()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.getBooleanExtra(UPDATE_DATA, false)) {
            val updatedBakerData = intent.getSerializable(
                DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
                BakerDelegationData::class.java
            )
            viewModel.bakerDelegationData = updatedBakerData
            initViews()
            validateAmountInput(binding.amount, binding.amountError)
            showStakingModeError(
                updatedBakerData.isLPool.not() &&
                        updatedBakerData.isBakerPool.not() &&
                        viewModel.isBakerPool().not() &&
                        viewModel.isLPool()
            )
        }
    }

    override fun showError(stakeError: StakeAmountInputValidator.StakeError?) {
        binding.amountError.visibility = View.VISIBLE
        if (stakeError == StakeAmountInputValidator.StakeError.POOL_LIMIT_REACHED ||
            stakeError == StakeAmountInputValidator.StakeError.POOL_LIMIT_REACHED_COOLDOWN
        ) {
            binding.poolLimitTitle.setTextColor(getColor(R.color.cryptox_pinky_main))
            binding.poolLimit.setTextColor(getColor(R.color.cryptox_pinky_main))
        } else {
            binding.poolLimitTitle.setTextColor(getColor(R.color.cryptox_grey_secondary))
            binding.poolLimit.setTextColor(getColor(R.color.cryptox_white_main))
        }
        if (stakeError == StakeAmountInputValidator.StakeError.POOL_LIMIT_REACHED_COOLDOWN) {
            binding.delegationAmountTitle.setTextColor(getColor(R.color.cryptox_pinky_main))
            binding.delegationAmount.setTextColor(getColor(R.color.cryptox_pinky_main))
        }
    }

    override fun hideError() {
        binding.poolLimitTitle.setTextColor(getColor(R.color.cryptox_grey_secondary))
        binding.poolLimit.setTextColor(getColor(R.color.cryptox_white_main))
        binding.delegationAmountTitle.setTextColor(getColor(R.color.cryptox_grey_secondary))
        binding.delegationAmount.setTextColor(getColor(R.color.cryptox_white_main))
        binding.amountError.visibility = View.GONE
    }

    override fun loadTransactionFee() {
        viewModel.loadTransactionFee(true)
    }

    @SuppressLint("SetTextI18n")
    override fun initViews() {
        super.initViews()
        super.initReStakeOptionsView(binding.restakeOptions)

        if (viewModel.isUpdatingDelegation())
            setActionBarTitle(R.string.delegation_update_delegation_title)

        binding.amount.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onContinueClicked()
                true
            } else {
                false
            }
        }
        setAmountHint(binding.amount)
        binding.amount.addTextChangedListener {
            validateAmountInput(binding.amount, binding.amountError)
            binding.poolRegistrationContinue.isEnabled = hasChanges()
            if (it.toString().isEmpty()) {
                binding.balanceSymbol.alpha = 0.5f
            } else {
                binding.balanceSymbol.alpha = 1f
            }
            CurrencyUtil.toGTUValue(binding.amount.text.toString())?.let { amount ->
                viewModel.loadEURRate(amount)
            }
        }
        binding.amount.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showKeyboard(this, binding.amount)
            }
        }
        binding.balanceSymbol.setOnClickListener {
            showKeyboard(this, binding.amount)
        }

        binding.poolRegistrationContinue.setOnClickListener {
            onContinueClicked()
        }

        binding.delegationTypeLayout.setOnClickListener {
            gotoDelegationTypeSelection()
        }

        binding.maxAmountButton.setOnClickListener {
            binding.amount.setText(CurrencyUtil.formatGTU(viewModel.getMaxDelegationBalance()))
        }

        binding.legalDisclaimerButton.setOnClickListener {
            showLegalDisclaimerDialog()
        }

        binding.balanceAmount.text = CurrencyUtil.formatGTU(viewModel.getAvailableBalance())

        viewModel.bakerDelegationData.account.let { account ->
            account.delegation?.let { accountDelegation ->
                binding.amountsLayout.visibility = View.VISIBLE
                binding.delegationAmount.text = getString(
                    R.string.amount,
                    CurrencyUtil.formatGTU(accountDelegation.stakedAmount)
                )
            }
        }
        updatePoolInfo()

        binding.poolRegistrationContinue.isEnabled = false
        binding.amount.isEnabled = false
        showWaiting(binding.includeProgress.progressLayout, true)

        viewModel.transactionFeeLiveData.observe(this) { response ->
            response?.first?.let {
                validateFee = it
                showWaiting(binding.includeProgress.progressLayout, false)
                binding.poolEstimatedTransactionFee.visibility = View.VISIBLE
                binding.poolEstimatedTransactionFee.text = getString(
                    R.string.cis_estimated_fee,
                    CurrencyUtil.formatGTU(validateFee ?: BigInteger.ZERO)
                )
                binding.poolRegistrationContinue.isEnabled = true
                if (!viewModel.isInCoolDown())
                    binding.amount.isEnabled = true
            }
        }

        loadTransactionFee()

        updateContent()

        initializeWaitingLiveData(binding.includeProgress.progressLayout)

        viewModel.showDetailedLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    updatePoolInfo()
                }
            }
        })
        viewModel.eurRateLiveData.observe(this) { rate ->
            binding.eurRate.text =
                if (rate != null)
                    getString(R.string.cis_estimated_eur_rate, rate)
                else
                    ""
        }

        baseDelegationBakerRegisterAmountListener =
            object : BaseDelegationBakerRegisterAmountListener {
                override fun onReStakeChanged() {
                    binding.poolRegistrationContinue.isEnabled = hasChanges()
                }
            }
    }

    private fun initObservers() {
        supportFragmentManager.setFragmentResultListener(
            WarningDialog.ACTION_REQUEST,
            this
        ) { _, bundle ->
            if (WarningDialog.getResult(bundle)) {
                continueToConfirmation()
            }
        }
    }

    private fun updatePoolInfo() {
        binding.poolInfo.visibility =
            when {
                viewModel.bakerDelegationData.isBakerPool -> View.VISIBLE
                viewModel.bakerDelegationData.isLPool -> View.GONE
                viewModel.isBakerPool() -> View.VISIBLE
                else -> View.GONE
            }

        binding.poolLimit.text =
            viewModel.bakerDelegationData.bakerPoolStatus?.let {
                getString(
                    R.string.amount,
                    CurrencyUtil.formatGTU(it.delegatedCapitalCap)
                )
            }
        binding.currentPool.text =
            viewModel.bakerDelegationData.bakerPoolStatus?.let {
                getString(
                    R.string.amount,
                    CurrencyUtil.formatGTU(it.delegatedCapital)
                )
            }
    }

    override fun getStakeAmountInputValidator(): StakeAmountInputValidator {
        return StakeAmountInputValidator(
            minimumValue = if (viewModel.isUpdatingDelegation()) BigInteger.ZERO else BigInteger.ONE,
            maximumValue = null,
            oldStakedAmount = null,
            balance = viewModel.bakerDelegationData.account.balance,
            atDisposal = viewModel.bakerDelegationData.account.balanceAtDisposal,
            currentPool = viewModel.bakerDelegationData.bakerPoolStatus?.delegatedCapital,
            poolLimit = viewModel.bakerDelegationData.bakerPoolStatus?.delegatedCapitalCap,
            previouslyStakedInPool = viewModel.bakerDelegationData.account.delegation?.stakedAmount,
            isInCoolDown = viewModel.isInCoolDown(),
            oldPoolId = viewModel.bakerDelegationData.account.delegation?.delegationTarget?.bakerId,
            newPoolId = viewModel.bakerDelegationData.poolId
        )
    }

    override fun errorLiveData(value: Int) {
        showError(null)
    }

    private fun updateContent() {
        if (viewModel.isInCoolDown()) {
            binding.amountLocked.visibility = View.VISIBLE
            binding.amount.isEnabled = false
        }
        if (viewModel.bakerDelegationData.type == UPDATE_DELEGATION &&
            binding.amount.text.toString().isEmpty()
        ) {
            binding.amount.setText(viewModel.bakerDelegationData.account.delegation?.stakedAmount?.let {
                CurrencyUtil.formatGTU(
                    value = it,
                    withCommas = false
                )
            })
            if (viewModel.isBakerPool()) {
                viewModel.validatePoolId()
            }
        }
    }

    private fun onContinueClicked() {

        if (!binding.poolRegistrationContinue.isEnabled) return

        val stakeAmountInputValidator = getStakeAmountInputValidator()
        val stakeError = stakeAmountInputValidator.validate(
            CurrencyUtil.toGTUValue(binding.amount.text.toString()),
            validateFee
        )
        if (stakeError != StakeAmountInputValidator.StakeError.OK) {
            binding.amountError.text = stakeAmountInputValidator.getErrorText(this, stakeError)
            showError(stakeError)
            return
        }

        val amountToStake = getAmountToStake()
        if (viewModel.isUpdatingDelegation()) {
            when {
                !hasChanges() -> showNoChange()
                amountToStake.signum() == 0 -> showNewAmountZero()
                amountToStake < (viewModel.bakerDelegationData.account.delegation?.stakedAmount
                    ?: BigInteger.ZERO) -> showReduceWarning()

                moreThan95Percent(amountToStake) -> show95PercentWarning()
                else -> continueToConfirmation()
            }
        } else {
            when {
                viewModel.isInitialSetup() -> showStakingModeError(true)
                moreThan95Percent(amountToStake) -> show95PercentWarning()
                else -> continueToConfirmation()
            }
        }
    }

    private fun hasChanges(): Boolean {
        return !((getAmountToStake() == viewModel.bakerDelegationData.oldStakedAmount &&
                viewModel.getPoolId() == (viewModel.bakerDelegationData.oldDelegationTargetPoolId?.toString()
            ?: "") &&
                viewModel.bakerDelegationData.restake == viewModel.bakerDelegationData.oldRestake &&
                viewModel.bakerDelegationData.isBakerPool == viewModel.bakerDelegationData.oldDelegationIsBaker))
    }

    private fun getAmountToStake(): BigInteger {
        return CurrencyUtil.toGTUValue(binding.amount.text.toString()) ?: BigInteger.ZERO
    }

    private fun showNewAmountZero() {
        WarningDialog.newInstance(
            WarningDialog.setBundle(
                title = getString(R.string.delegation_amount_zero_title),
                description = getString(R.string.delegation_amount_zero_message),
                confirmButton = getString(R.string.delegation_amount_zero_continue),
                denyButton = getString(R.string.delegation_amount_zero_new_stake)
            )
        ).showSingle(supportFragmentManager, WarningDialog.TAG)
    }

    private fun showReduceWarning() {
        WarningDialog.newInstance(
            WarningDialog.setBundle(
                title = getString(R.string.delegation_register_delegation_reduce_warning_title),
                description = getString(R.string.delegation_register_delegation_reduce_warning_content),
                confirmButton = getString(R.string.delegation_register_delegation_reduce_warning_ok),
                denyButton = getString(R.string.delegation_register_delegation_reduce_warning_cancel)
            )
        ).showSingle(supportFragmentManager, WarningDialog.TAG)
    }

    private fun show95PercentWarning() {
        WarningDialog.newInstance(
            WarningDialog.setBundle(
                title = getString(R.string.delegation_more_than_95_title),
                description = getString(R.string.delegation_more_than_95_message),
                confirmButton = getString(R.string.delegation_more_than_95_continue),
                denyButton = getString(R.string.delegation_more_than_95_new_stake)
            )
        ).showSingle(supportFragmentManager, WarningDialog.TAG)
    }

    private fun continueToConfirmation() {
        viewModel.bakerDelegationData.amount =
            CurrencyUtil.toGTUValue(binding.amount.text.toString())
        val intent = if ((viewModel.bakerDelegationData.amount ?: 0) == 0L)
            Intent(this, DelegationRemoveActivity::class.java)
        else
            Intent(this, DelegationRegisterConfirmationActivity::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun checkDelegationType() {
        binding.delegationTypeTitle.setTextAppearance(R.style.MW24_Typography_Text_Main)
        binding.delegationTypeTitle.setTextColor(getColor(R.color.cryptox_white_main))

        val delegationTypeText: String
        when {
            viewModel.bakerDelegationData.isLPool -> {
                delegationTypeText = getString(R.string.delegation_register_delegation_passive)
            }

            viewModel.bakerDelegationData.isBakerPool -> {
                delegationTypeText = getString(R.string.delegation_register_delegation_pool_baker)
            }

            viewModel.isUpdatingDelegation() -> {
                delegationTypeText = if (viewModel.isLPool())
                    getString(R.string.delegation_register_delegation_passive)
                else
                    getString(R.string.delegation_register_delegation_pool_baker)
            }

            else -> {
                delegationTypeText = getString(R.string.delegation_register_staking_mode)
                binding.delegationTypeTitle.setTextAppearance(R.style.MW24_Typography_Text_Mid)
                binding.delegationTypeTitle.setTextColor(getColor(R.color.mw24_blue_3_50))
            }
        }

        binding.delegationTypeTitle.text = delegationTypeText
    }

    private fun showStakingModeError(isVisible: Boolean) {
        binding.stakingModeError.isVisible = isVisible
    }

    private fun gotoDelegationTypeSelection() {
        val intent = Intent(this, DelegationRegisterPoolActivity::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    companion object {
        const val UPDATE_DATA = "update_data"
    }
}
