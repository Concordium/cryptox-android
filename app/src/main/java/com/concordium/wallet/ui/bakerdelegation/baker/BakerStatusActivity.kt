package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentResultListener
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository
import com.concordium.wallet.data.model.BakerPoolInfo
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.ui.bakerdelegation.baker.introflow.BakerRemoveIntroFlow
import com.concordium.wallet.ui.bakerdelegation.baker.introflow.BakerUpdateIntroFlow
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.bakerdelegation.common.StatusActivity
import com.concordium.wallet.ui.common.GenericFlowActivity

class BakerStatusActivity : StatusActivity(R.string.baker_status_title), FragmentResultListener {
    private var menuDialog: AlertDialog? = null

    override fun initView() {
        clearState()

        val account = viewModel.bakerDelegationData.account
        val accountBaker = account?.baker

        binding.statusButtonBottom.text = getString(R.string.baker_status_change_baking_status)

        if (viewModel.bakerDelegationData.isTransactionInProgress) {
            addWaitingForTransaction(
                R.string.baker_status_baker_waiting_title,
                R.string.baker_status_baker_waiting
            )
            return
        }

        if (account == null || accountBaker == null) {
            binding.statusIconImageView.setImageResource(R.drawable.ic_pending)
            setContentTitle(R.string.baker_status_no_baker_title)
            setEmptyState(getString(R.string.baker_status_no_baker))
            binding.statusButtonBottom.text = getString(R.string.baker_status_register_baker)
            binding.statusButtonBottom.setOnClickListener {
                continueToBakerAmount()
            }
            return
        }

        if (accountBaker.isPrimedForSuspension) {
            binding.statusIconImageView.setImageResource(R.drawable.ic_status_problem)
            setContentTitle(R.string.baker_status_baker_primed_for_suspension_title)
        } else if (accountBaker.isSuspended) {
            binding.statusIconImageView.setImageResource(R.drawable.ic_status_problem)
            setContentTitle(R.string.baker_status_baker_suspended_title)
        } else {
            binding.statusIconImageView.setImageResource(R.drawable.cryptox_ico_successfully)
            setContentTitle(R.string.baker_status_baker_registered_title)
        }

        addContent(
            titleRes = R.string.baker_status_baker_account,
            text = account.getAccountName() + "\n\n" + account.address,
            visibleDivider = false
        )
        addContent(
            R.string.baker_status_baker_stake,
            CurrencyUtil.formatGTU(accountBaker.stakedAmount)
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

        viewModel.bakerDelegationData.account?.cooldowns
            ?.also(::addCooldowns)

        binding.statusButtonBottom.setOnClickListener {
            openChangeBakerStatusBottomSheet()
        }

        supportFragmentManager.setFragmentResultListener(
            ChangeBakerStatusBottomSheet.REQUEST_KEY,
            this,
            this
        )
    }

    private fun continueToBakerAmount() {
        val intent = Intent(this, BakerRegisterAmountActivity::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun openChangeBakerStatusBottomSheet() {
        ChangeBakerStatusBottomSheet
            .newInstance(isStopEnabled = !viewModel.isInCoolDown())
            .show(supportFragmentManager, ChangeBakerStatusBottomSheet.REQUEST_KEY)
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

            R.id.stop_button ->
                gotoBakerRemoveIntroFlow()

            else ->
                error("Unknown button clicked")
        }
    }

    private fun gotoBakerUpdateIntroFlow(bakerSettingsMenuItem: String) {
        menuDialog?.dismiss()
        val intent = Intent(this, BakerUpdateIntroFlow::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        viewModel.bakerDelegationData.type = bakerSettingsMenuItem
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun gotoBakerRemoveIntroFlow() {
        menuDialog?.dismiss()
        val intent = Intent(this, BakerRemoveIntroFlow::class.java)
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        viewModel.bakerDelegationData.type = ProxyRepository.REMOVE_BAKER
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }
}
