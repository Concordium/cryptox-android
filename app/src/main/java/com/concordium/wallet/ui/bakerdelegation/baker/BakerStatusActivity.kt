package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentResultListener
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.BakerPoolInfo
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.bakerdelegation.baker.introflow.BakerUpdateIntroFlow
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.bakerdelegation.common.StatusActivity
import com.concordium.wallet.ui.bakerdelegation.dialog.baker.StopValidationDialog
import com.concordium.wallet.ui.bakerdelegation.dialog.baker.SuspendValidationDialog
import java.math.BigInteger

class BakerStatusActivity : StatusActivity(R.string.baker_status_title), FragmentResultListener {
    private var menuDialog: AlertDialog? = null

    override fun initView() {
        clearState()

        val account = viewModel.bakerDelegationData.account
        val accountBaker = account.baker

        if (viewModel.bakerDelegationData.isTransactionInProgress) {
            addWaitingForTransaction(
                R.string.baker_status_baker_waiting_title,
                R.string.baker_status_baker_waiting
            )
            binding.actionButtonsLayout.root.visibility = View.GONE
            return
        }

        if (accountBaker == null) {
            setContentTitle(R.string.baker_status_no_baker_title)
            setEmptyState(getString(R.string.baker_status_no_baker))
            return
        }

        binding.actionButtonsLayout.bakerUpdateButton.visibility = View.VISIBLE

        if (viewModel.isBakerPrimedForSuspension()) {
            setContentTitle(R.string.baker_status_baker_primed_for_suspension_title)
            setExplanation(getString(R.string.validation_primed_for_suspension_baker_explanation))
            binding.actionButtonsLayout.suspendButton.visibility = View.VISIBLE
            binding.actionButtonsLayout.resumeButton.visibility = View.GONE
        } else if (viewModel.isBakerSuspended()) {
            setContentTitle(R.string.baker_status_baker_suspended_title)
            setExplanation(getString(R.string.validation_suspended_baker_explanation))
            binding.actionButtonsLayout.suspendButton.visibility = View.GONE
            binding.actionButtonsLayout.resumeButton.visibility = View.VISIBLE
        } else {
            binding.statusLayout.visibility = View.GONE
            binding.actionButtonsLayout.suspendButton.visibility = View.VISIBLE
            binding.actionButtonsLayout.resumeButton.visibility = View.GONE
            setContentTitle(R.string.baker_status_baker_registered_title)
        }

        addContent(
            titleRes = R.string.baker_status_baker_account,
            text = account.getAccountName() + "\n\n" + account.address,
            visibleDivider = false
        )
        addContent(
            R.string.baker_status_baker_stake,
            getString(R.string.amount, CurrencyUtil.formatGTU(accountBaker.stakedAmount))
        )
        addContent(R.string.baker_status_baker_id, accountBaker.bakerId.toString())

        if (accountBaker.restakeEarnings) addContent(
            R.string.baker_status_baker_rewards_will_be,
            getString(R.string.baker_status_baker_added_to_stake)
        )
        else addContent(
            R.string.baker_status_baker_rewards_will_be,
            getString(R.string.baker_status_baker_at_disposal)
        )

        when (accountBaker.bakerPoolInfo.openStatus) {
            BakerPoolInfo.OPEN_STATUS_OPEN_FOR_ALL -> addContent(
                R.string.baker_status_baker_delegation_pool_status,
                getString(R.string.baker_status_baker_delegation_pool_status_open)
            )

            BakerPoolInfo.OPEN_STATUS_CLOSED_FOR_NEW -> addContent(
                R.string.baker_status_baker_delegation_pool_status,
                getString(R.string.baker_status_baker_delegation_pool_status_closed_for_new)
            )

            else -> addContent(
                R.string.baker_status_baker_delegation_pool_status,
                getString(R.string.baker_status_baker_delegation_pool_status_closed)
            )
        }

        if (!accountBaker.bakerPoolInfo.metadataUrl.isNullOrBlank()) {
            addContent(
                R.string.baker_status_baker_metadata_url,
                accountBaker.bakerPoolInfo.metadataUrl
            )
        }

        addCooldowns(viewModel.bakerDelegationData.account.cooldowns)
        initButtons()
        initObservers()
    }

    private fun initButtons() {
        binding.actionButtonsLayout.bakerUpdateButton.setOnClickListener {
            openChangeBakerStatusBottomSheet()
        }

        binding.actionButtonsLayout.stopButton.setOnClickListener {
            showBakerRemoveDialog()
        }

        binding.actionButtonsLayout.suspendButton.setOnClickListener {
            showBakerSuspendDialog()
        }

        binding.actionButtonsLayout.resumeButton.setOnClickListener {
            gotoBakerResume()
        }
    }

    private fun initObservers() {
        supportFragmentManager.setFragmentResultListener(
            ChangeBakerStatusBottomSheet.REQUEST_KEY,
            this,
            this
        )

        supportFragmentManager.setFragmentResultListener(
            StopValidationDialog.ACTION_CONTINUE,
            this
        ) { _, bundle ->
            if (StopValidationDialog.getResult(bundle)) {
                gotoBakerRemove()
            }
        }

        supportFragmentManager.setFragmentResultListener(
            SuspendValidationDialog.ACTION_SUSPEND_CONTINUE,
            this
        ) { _, bundle ->
            if (SuspendValidationDialog.getResult(bundle)) {
                gotoBakerSuspend()
            }
        }
    }

    private fun continueToBakerAmount() {
        val intent = Intent(this, BakerRegisterAmountActivity::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun openChangeBakerStatusBottomSheet() {
        ChangeBakerStatusBottomSheet().showSingle(
            supportFragmentManager,
            ChangeBakerStatusBottomSheet.REQUEST_KEY
        )
    }

    override fun onFragmentResult(requestKey: String, result: Bundle) {
        val clickedButtonId = ChangeBakerStatusBottomSheet.getResult(result)
            ?: return

        when (clickedButtonId) {
            R.id.update_stake_button ->
                gotoBakerUpdateIntroFlow(ProxyRepository.UPDATE_BAKER_STAKE)

            R.id.update_pool_settings_button ->
                gotoBakerUpdateIntroFlow(ProxyRepository.UPDATE_BAKER_POOL)

            R.id.update_keys_button ->
                gotoBakerUpdateIntroFlow(ProxyRepository.UPDATE_BAKER_KEYS)

            else ->
                error("Unknown button clicked")
        }
    }

    private fun gotoBakerUpdateIntroFlow(bakerSettingsMenuItem: String) {
        menuDialog?.dismiss()
        val intent = Intent(this, BakerUpdateIntroFlow::class.java)
        viewModel.bakerDelegationData.type = bakerSettingsMenuItem
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun gotoBakerRemove() {
        viewModel.bakerDelegationData.type = ProxyRepository.REMOVE_BAKER
        viewModel.bakerDelegationData.amount = BigInteger.ZERO
        viewModel.bakerDelegationData.metadataUrl = null

        val intent = Intent(this, BakerRegistrationConfirmationActivity::class.java)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
        finishUntilClass(MainActivity::class.java.canonicalName)
    }

    private fun showBakerRemoveDialog() {
        StopValidationDialog().showSingle(
            supportFragmentManager,
            StopValidationDialog.TAG
        )
    }

    private fun gotoBakerResume() {
        menuDialog?.dismiss()
        val intent = Intent(this, BakerRegistrationConfirmationActivity::class.java)
        viewModel.bakerDelegationData.type = ProxyRepository.CONFIGURE_BAKER
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData.copy().apply {
                toSetBakerSuspended = false
            }
        )
        startActivityForResultAndHistoryCheck(intent)
        finishUntilClass(MainActivity::class.java.canonicalName)
    }

    private fun showBakerSuspendDialog() {
        SuspendValidationDialog().showSingle(
            supportFragmentManager,
            SuspendValidationDialog.TAG
        )
    }

    private fun gotoBakerSuspend() {
        menuDialog?.dismiss()
        val intent = Intent(this, BakerRegistrationConfirmationActivity::class.java)
        viewModel.bakerDelegationData.type = ProxyRepository.CONFIGURE_BAKER
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData.copy().apply {
                toSetBakerSuspended = true
            }
        )
        startActivityForResultAndHistoryCheck(intent)
        finishUntilClass(MainActivity::class.java.canonicalName)
    }
}
