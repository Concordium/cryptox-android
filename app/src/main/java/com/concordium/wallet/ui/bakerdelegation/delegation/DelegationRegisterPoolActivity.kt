package com.concordium.wallet.ui.bakerdelegation.delegation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_DELEGATION
import com.concordium.wallet.databinding.ActivityDelegationRegistrationPoolBinding
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.AMOUNT_TOO_LARGE_FOR_POOL
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel.Companion.AMOUNT_TOO_LARGE_FOR_POOL_COOLDOWN
import com.concordium.wallet.uicore.handleUrlClicks
import com.concordium.wallet.uicore.view.SegmentedControlView
import com.concordium.wallet.util.KeyboardUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DelegationRegisterPoolActivity : BaseDelegationBakerActivity(
    R.layout.activity_delegation_registration_pool, R.string.delegation_register_delegation_title
) {
    private lateinit var binding: ActivityDelegationRegistrationPoolBinding
    private lateinit var lPoolControl: View
    private lateinit var bakerPoolControl: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDelegationRegistrationPoolBinding.bind(findViewById(R.id.root_layout))
        hideActionBarBack(isVisible = true)
        initViews()
    }

    override fun onResume() {
        super.onResume()
        if (!binding.poolId.text.isNullOrBlank())
            viewModel.setPoolID(binding.poolId.text.toString())
    }

    fun showError() {
        binding.poolIdError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.poolIdError.visibility = View.GONE
    }

    private fun showDetailedPage() {
        val intent = Intent(this, DelegationRegisterAmountActivity::class.java)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun initViews() {
        showWaiting(binding.includeProgress.progressLayout, false)

        binding.poolOptions.clearAll()
        bakerPoolControl = binding.poolOptions.addControl(
            getString(R.string.delegation_register_delegation_pool_baker),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    binding.poolId.setText("")
                    viewModel.selectBakerPool()
                    updateVisibilities()
                }
            },
            viewModel.isBakerPool() || (!viewModel.isBakerPool() && !viewModel.isLPool())
        )
        lPoolControl = binding.poolOptions.addControl(
            getString(R.string.delegation_register_delegation_passive),
            object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectLPool()
                    updateVisibilities()
                }
            },
            viewModel.isLPool()
        )

        binding.poolId.setText(viewModel.getPoolId())
        binding.poolId.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                KeyboardUtil.hideKeyboard(this)
                onContinueClicked()
                @Suppress("UNUSED_EXPRESSION")
                true
            }
            false
        }

        binding.poolRegistrationContinue.setOnClickListener {
            onContinueClicked()
        }

        binding.poolId.doOnTextChanged { text, _, _, _ ->
            if (text != null && text.isNotEmpty())
                viewModel.setPoolID(text.toString())
            else if (viewModel.bakerDelegationData.oldDelegationTargetPoolId != null)
                viewModel.setPoolID(viewModel.bakerDelegationData.oldDelegationTargetPoolId.toString())
            updateVisibilities()
        }

        updateContent()
        updateVisibilities()

        initializeWaitingLiveData(binding.includeProgress.progressLayout)

        viewModel.showDetailedLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showDetailedPage()
                }
            }
        })

        viewModel.bakerPoolStatusLiveData.observe(this) {
            showWaiting(binding.includeProgress.progressLayout, false)
            viewModel.bakerDelegationData.bakerPoolStatus = it
            showDetailedPage()
        }
    }

    override fun errorLiveData(value: Int) {
        when (value) {
            AMOUNT_TOO_LARGE_FOR_POOL -> {
                showDelegationAmountTooLargeNotice()
            }

            AMOUNT_TOO_LARGE_FOR_POOL_COOLDOWN -> {
                showDetailedPage()
            }

            else -> {
                binding.poolIdError.text = getString(value)
                showError()
            }
        }
    }

    private fun updateContent() {
        if (viewModel.bakerDelegationData.type == UPDATE_DELEGATION) {
            setActionBarTitle(R.string.delegation_update_delegation_title)
            if (viewModel.isBakerPool()) {
                viewModel.selectBakerPool()
                binding.existingPoolId.text = getString(
                    R.string.delegation_update_delegation_pool_id_baker,
                    getExistingPoolIdText()
                )
            } else {
                viewModel.selectLPool()
                binding.existingPoolId.text =
                    getString(R.string.delegation_update_delegation_pool_id__passive)
            }
            binding.existingPoolId.isVisible = true
        } else {
            binding.existingPoolId.isVisible = false
        }
    }

    private fun updateVisibilities() {
        binding.poolId.hint =
            if (viewModel.bakerDelegationData.oldDelegationTargetPoolId == null) getString(R.string.delegation_register_delegation_pool_id_hint) else getString(
                R.string.delegation_register_delegation_pool_id_hint_update
            )
        binding.poolId.visibility =
            if (viewModel.bakerDelegationData.isLPool) View.GONE else View.VISIBLE
        if (viewModel.bakerDelegationData.isLPool) binding.poolDesc.setText(R.string.delegation_register_delegation_desc_passive) else binding.poolDesc.setText(
            R.string.delegation_register_delegation_desc
        )
        binding.poolDesc.handleUrlClicks { url ->
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            ContextCompat.startActivity(this, browserIntent, null)
        }
        binding.poolRegistrationContinue.isEnabled =
            getExistingPoolIdText().isNotEmpty() || viewModel.bakerDelegationData.isLPool || binding.poolId.text.isNotEmpty()
        hideError()
    }

    private fun onContinueClicked() {
        if (viewModel.isBakerPool() && viewModel.getPoolId()
                .isEmpty() && getExistingPoolIdText().isNotEmpty()
        )
            viewModel.setPoolID(getExistingPoolIdText())
        viewModel.validatePoolId()
    }

    private fun getExistingPoolIdText(): String {
        viewModel.bakerDelegationData.account.delegation?.delegationTarget?.bakerId?.let {
            return it.toString()
        }
        return ""
    }

    private fun showDelegationAmountTooLargeNotice() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.delegation_amount_too_large_notice_title)
        builder.setMessage(getString(R.string.delegation_amount_too_large_notice_message))
        builder.setPositiveButton(getString(R.string.delegation_amount_too_large_notice_lower)) { _, _ ->
            viewModel.bakerDelegationData.oldDelegationTargetPoolId?.let {
                viewModel.setPoolID(it.toString())
            }
            showWaiting(binding.includeProgress.progressLayout, true)
            viewModel.getBakerPool(viewModel.getPoolId())
        }
        builder.setNegativeButton(getString(R.string.delegation_amount_too_large_notice_stop)) { _, _ -> gotoStopDelegation() }
        builder.setNeutralButton(getString(R.string.delegation_amount_too_large_notice_cancel)) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun gotoStopDelegation() {
        val intent = Intent(this, DelegationRemoveActivity::class.java)
        intent.putExtra(DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA, viewModel.bakerDelegationData)
        startActivityForResultAndHistoryCheck(intent)
    }
}
