package com.concordium.wallet.ui.bakerdelegation.delegation

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_DELEGATION
import com.concordium.wallet.data.model.BakerDelegationData
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityDelegationRegistrationAmountBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerRegisterAmountActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.bakerdelegation.common.StakeAmountInputValidator
import com.concordium.wallet.ui.bakerdelegation.dialog.delegation.DelegationWarningDialog
import com.concordium.wallet.util.KeyboardUtil.showKeyboard
import java.math.BigInteger

class DelegationRegisterAmountActivity : BaseDelegationBakerRegisterAmountActivity(
    R.layout.activity_delegation_registration_amount, R.string.delegation_register_delegation_title
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

    private fun showConfirmationPage() {
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
            }
            false
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

        binding.balanceAmount.text =
            getString(
                R.string.amount,
                CurrencyUtil.formatGTU(viewModel.getAvailableBalance())
            )
        binding.delegationAmount.text = getString(
            R.string.amount,
            CurrencyUtil.formatGTU(BigInteger.ZERO)
        )
        viewModel.bakerDelegationData.account.let { account ->
            account.delegation?.let { accountDelegation ->
                binding.delegationAmount.text =
                    CurrencyUtil.formatGTU(accountDelegation.stakedAmount)
            }
        }

        binding.poolLimit.text =
            viewModel.bakerDelegationData.bakerPoolStatus?.let {
                CurrencyUtil.formatGTU(it.delegatedCapitalCap)
            }
        binding.currentPool.text =
            viewModel.bakerDelegationData.bakerPoolStatus?.let {
                CurrencyUtil.formatGTU(it.delegatedCapital)
            }

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

        binding.poolInfo.visibility =
            if (viewModel.bakerDelegationData.isLPool) View.GONE else View.VISIBLE

        updateContent()

        initializeWaitingLiveData(binding.includeProgress.progressLayout)

        viewModel.showDetailedLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showConfirmationPage()
                }
            }
        })

        baseDelegationBakerRegisterAmountListener =
            object : BaseDelegationBakerRegisterAmountListener {
                override fun onReStakeChanged() {
                    binding.poolRegistrationContinue.isEnabled = hasChanges()
                }
            }
    }

    private fun initObservers() {
        supportFragmentManager.setFragmentResultListener(
            DelegationWarningDialog.ACTION_REQUEST,
            this
        ) { _, bundle ->
            if (DelegationWarningDialog.getResult(bundle)) {
                continueToConfirmation()
            }
        }
    }

    override fun getStakeAmountInputValidator(): StakeAmountInputValidator {
        return StakeAmountInputValidator(
            minimumValue = if (viewModel.isUpdatingDelegation()) BigInteger.ZERO else BigInteger.ONE,
            maximumValue = null,
            oldStakedAmount = null,
            balance = viewModel.bakerDelegationData.account.balance,
            atDisposal = viewModel.atDisposal(),
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
        if (viewModel.bakerDelegationData.type == UPDATE_DELEGATION) {
            binding.amount.setText(viewModel.bakerDelegationData.account.delegation?.stakedAmount?.let {
                CurrencyUtil.formatGTU(
                    value = it,
                    withCommas = false
                )
            })
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
        DelegationWarningDialog.newInstance(
            DelegationWarningDialog.setBundle(
                title = getString(R.string.delegation_amount_zero_title),
                description = getString(R.string.delegation_amount_zero_message),
                confirmButton = getString(R.string.delegation_amount_zero_continue),
                denyButton = getString(R.string.delegation_amount_zero_new_stake)
            )
        ).showSingle(supportFragmentManager, DelegationWarningDialog.TAG)
    }

    private fun showReduceWarning() {
        DelegationWarningDialog.newInstance(
            DelegationWarningDialog.setBundle(
                title = getString(R.string.delegation_register_delegation_reduce_warning_title),
                description = getString(R.string.delegation_register_delegation_reduce_warning_content),
                confirmButton = getString(R.string.delegation_register_delegation_reduce_warning_ok),
                denyButton = getString(R.string.delegation_register_delegation_reduce_warning_cancel)
            )
        ).showSingle(supportFragmentManager, DelegationWarningDialog.TAG)
    }

    private fun show95PercentWarning() {
        DelegationWarningDialog.newInstance(
            DelegationWarningDialog.setBundle(
                title = getString(R.string.delegation_more_than_95_title),
                description = getString(R.string.delegation_more_than_95_message),
                confirmButton = getString(R.string.delegation_more_than_95_continue),
                denyButton = getString(R.string.delegation_more_than_95_new_stake)
            )
        ).showSingle(supportFragmentManager, DelegationWarningDialog.TAG)
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
        val delegationType = if (viewModel.bakerDelegationData.isLPool)
            getString(R.string.delegation_register_delegation_passive)
        else if (viewModel.bakerDelegationData.isBakerPool)
            getString(R.string.delegation_register_delegation_pool_baker)
        else
            getString(R.string.delegation_register_staking_mode)
        binding.delegationTypeTitle.text = delegationType
    }

    private fun gotoDelegationTypeSelection() {
        val intent = Intent(this, DelegationRegisterPoolActivity::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            BakerDelegationData(viewModel.bakerDelegationData.account, type = ProxyRepository.REGISTER_DELEGATION)
        )
        startActivity(intent)
    }
}
