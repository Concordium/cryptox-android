package com.concordium.wallet.ui.bakerdelegation.delegation

import android.content.Intent
import android.view.View
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.REGISTER_DELEGATION
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_DELEGATION
import com.concordium.wallet.data.model.BakerStakePendingChange.Companion.CHANGE_REMOVE_POOL
import com.concordium.wallet.data.model.DelegationTarget
import com.concordium.wallet.data.model.PendingChange
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.EXTRA_DELEGATION_BAKER_DATA
import com.concordium.wallet.ui.bakerdelegation.common.StatusActivity
import com.concordium.wallet.ui.bakerdelegation.delegation.introflow.DelegationUpdateIntroFlowActivity
import com.concordium.wallet.ui.bakerdelegation.dialog.delegation.StopDelegationDialog
import com.concordium.wallet.util.DateTimeUtil.formatTo
import com.concordium.wallet.util.DateTimeUtil.toDate

class DelegationStatusActivity : StatusActivity(R.string.delegation_status_title) {
    override fun initializeViewModel() {
        super.initializeViewModel()

        viewModel.bakerPoolStatusLiveData.observe(this) {
            it?.let { bakerPoolStatus ->
                if (bakerPoolStatus.bakerStakePendingChange.pendingChangeType == CHANGE_REMOVE_POOL) {
                    bakerPoolStatus.bakerStakePendingChange.estimatedChangeTime?.let { estimatedChangeTime ->
                        val prefix = estimatedChangeTime.toDate()?.formatTo("yyyy-MM-dd")
                        val postfix = estimatedChangeTime.toDate()?.formatTo("HH:mm")
                        val dateStr =
                            getString(R.string.delegation_status_effective_time, prefix, postfix)
                        addContent(
                            getString(R.string.delegation_status_pool_deregistered) + "\n" + dateStr,
                            "",
                            R.color.text_pink
                        )
                    }
                }
            }
        }
    }

    override fun initView() {
        clearState()

        val account = viewModel.bakerDelegationData.account
        val accountDelegation = account.delegation

        if (viewModel.bakerDelegationData.isTransactionInProgress) {
            addWaitingForTransaction(
                R.string.delegation_status_waiting_to_finalize_title,
                R.string.delegation_status_waiting_to_finalize
            )
            binding.actionButtonsLayout.root.visibility = View.GONE
            return
        }

        if (accountDelegation == null) {
            setContentTitle(R.string.delegation_status_content_empty_title)
            setEmptyState(getString(R.string.delegation_status_content_empty_desc))
            binding.statusButtonBottom.text = getString(R.string.delegation_status_continue)
            binding.statusButtonBottom.setOnClickListener {
                continueToCreate()
            }
            return
        }

        if (viewModel.isBakerSuspended()) {
            setContentTitle(R.string.delegation_status_content_suspended_title)
            setExplanation(getString(R.string.validation_suspended_delegator_explanation))
        } else {
            binding.statusLayout.visibility = View.GONE
        }

        addContent(
            R.string.delegation_status_content_delegating_account,
            account.getAccountName() + "\n\n" + account.address,
            false
        )
        addContent(
            R.string.delegation_status_content_delegation_amount,
            getString(R.string.amount, CurrencyUtil.formatGTU(accountDelegation.stakedAmount))
        )

        if (accountDelegation.delegationTarget.delegateType == DelegationTarget.TYPE_DELEGATE_TO_BAKER) addContent(
            R.string.delegation_status_content_target_pool,
            accountDelegation.delegationTarget.bakerId.toString()
        )
        else addContent(
            R.string.delegation_status_content_target_pool,
            getString(R.string.delegation_register_delegation_passive_long)
        )

        if (accountDelegation.restakeEarnings) addContent(
            R.string.delegation_status_content_rewards_will_be,
            getString(R.string.delegation_status_added_to_delegation_amount)
        )
        else addContent(
            R.string.delegation_status_content_rewards_will_be,
            getString(R.string.delegation_status_at_disposal)
        )

        viewModel.bakerDelegationData.account.delegation?.pendingChange?.let { pendingChange ->
            addPendingChange(pendingChange)
            binding.actionButtonsLayout.stopBtn.isEnabled =
                pendingChange.change == PendingChange.CHANGE_NO_CHANGE
        }

        addCooldowns(viewModel.bakerDelegationData.account.cooldowns)

        binding.actionButtonsLayout.stopBtn.setOnClickListener {
            continueToDelete()
        }

        viewModel.bakerDelegationData.bakerPoolStatus?.bakerStakePendingChange?.pendingChangeType.let { pendingChangeType ->
            if (pendingChangeType == CHANGE_REMOVE_POOL) {
                binding.statusButtonTop.isEnabled = true
            }
        }

        binding.actionButtonsLayout.updateBtn.setOnClickListener {
            continueToUpdate()
        }

        if (accountDelegation.delegationTarget.delegateType == DelegationTarget.TYPE_DELEGATE_TO_BAKER) {
            viewModel.getBakerPool(accountDelegation.delegationTarget.bakerId.toString())
        }

        initObservers()
    }

    private fun initObservers() {
        supportFragmentManager.setFragmentResultListener(
            StopDelegationDialog.ACTION_KEY,
            this
        ) { _, bundle ->
            if (StopDelegationDialog.getResult(bundle)) {
                val intent = Intent(this, DelegationRemoveActivity::class.java)
                intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
                startActivityForResultAndHistoryCheck(intent)
                finishUntilClass(MainActivity::class.java.canonicalName)
            }
        }
    }

    private fun continueToDelete() {
        StopDelegationDialog().showSingle(
            supportFragmentManager,
            StopDelegationDialog.TAG
        )
    }

    private fun continueToCreate() {
        val intent = Intent(this, DelegationRegisterAmountActivity::class.java)
        viewModel.bakerDelegationData.type = REGISTER_DELEGATION
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun continueToUpdate() {
        val intent = Intent(this, DelegationUpdateIntroFlowActivity::class.java)
        viewModel.bakerDelegationData.type = UPDATE_DELEGATION
        intent.putExtra(EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }
}
