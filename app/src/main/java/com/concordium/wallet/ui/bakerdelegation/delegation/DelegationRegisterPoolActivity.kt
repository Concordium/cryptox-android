package com.concordium.wallet.ui.bakerdelegation.delegation

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.concordium.wallet.BuildConfig
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

@Suppress("KotlinConstantConditions")
class DelegationRegisterPoolActivity : BaseDelegationBakerActivity(
    R.layout.activity_delegation_registration_pool, R.string.delegation_register_staking_mode
) {
    private lateinit var binding: ActivityDelegationRegistrationPoolBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDelegationRegistrationPoolBinding.bind(findViewById(R.id.root_layout))
        hideActionBarBack(isVisible = true)
        initViews()
    }

    override fun onResume() {
        super.onResume()
        if (binding.poolId.getText().isNotBlank())
            viewModel.setPoolID(binding.poolId.getText())

        if (checkIsPassiveMode()) {
            viewModel.selectLPool()
        }
        updateVisibilities()
    }

    fun showError() {
        binding.poolIdError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.poolIdError.visibility = View.GONE
    }

    private fun showDetailedPage() {
        val intent = Intent(this, DelegationRegisterAmountActivity::class.java)
        intent.apply {
            putExtra(
                DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
                viewModel.bakerDelegationData
            )
            putExtra(DelegationRegisterAmountActivity.UPDATE_DATA, true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    override fun initViews() {
        showWaiting(binding.includeProgress.progressLayout, false)

        binding.poolId.setText(viewModel.getPoolId())
        binding.poolId.setOnSearchDoneListener {
            KeyboardUtil.hideKeyboard(this)
            onContinueClicked()
        }

        binding.poolRegistrationContinue.setOnClickListener {
            onContinueClicked()
        }

        binding.poolId.setTextChangeListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s.isNullOrBlank().not()) {
                        viewModel.setPoolID(s.toString())
                    } else if (viewModel.bakerDelegationData.oldDelegationTargetPoolId != null)
                        viewModel.setPoolID(viewModel.bakerDelegationData.oldDelegationTargetPoolId.toString())
                    updateVisibilities()
                }
            }
        )
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
                binding.existingPoolId.text = getString(
                    R.string.delegation_update_delegation_pool_id_baker,
                    getExistingPoolIdText()
                )
            } else {
                binding.existingPoolId.text =
                    getString(R.string.delegation_update_delegation_pool_id__passive)
            }
            binding.existingPoolId.isVisible = true
        } else {
            binding.existingPoolId.isVisible = false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateVisibilities() {
        binding.poolOptions.clearAll()
        binding.poolOptions.addControl(
            title = getString(R.string.delegation_register_delegation_passive),
            clickListener = object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectLPool()
                    updateVisibilities()
                    KeyboardUtil.hideKeyboard(this@DelegationRegisterPoolActivity)
                }
            },
            initiallySelected = checkIsPassiveMode()
        )
        binding.poolOptions.addControl(
            title = getString(R.string.delegation_register_delegation_pool_baker),
            clickListener = object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    binding.poolId.setText("")
                    viewModel.selectBakerPool()
                    updateVisibilities()
                }
            },
            initiallySelected = checkIsPassiveMode().not()
        )

        binding.poolId.setLabelText(
            if (viewModel.bakerDelegationData.oldDelegationTargetPoolId == null)
                getString(R.string.delegation_register_delegation_pool_id_hint)
            else
                getString(
                    R.string.delegation_register_delegation_pool_id_hint_update
                )
        )
        binding.poolId.isVisible = checkIsPassiveMode().not()

        if (checkIsPassiveMode())
            binding.poolDesc.text = ""
        else
            binding.poolDesc.setText(
                if (BuildConfig.ENV_NAME == "production")
                    R.string.delegation_register_delegation_desc
                else
                    R.string.delegation_register_delegation_desc_testnet
            )

        binding.poolDesc.handleUrlClicks { url ->
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            ContextCompat.startActivity(this, browserIntent, null)
        }
        binding.poolRegistrationContinue.isEnabled = viewModel.isInitialSetup() ||
                getExistingPoolIdText().isNotEmpty() || viewModel.bakerDelegationData.isLPool ||
                binding.poolId.getText().isNotEmpty()
        hideError()
    }

    private fun checkIsPassiveMode(): Boolean {
        return when {
            viewModel.bakerDelegationData.isLPool -> true
            viewModel.bakerDelegationData.isBakerPool -> false
            viewModel.isLPool() -> true
            viewModel.isBakerPool() -> false
            else -> true
        }
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
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }
}
