package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.os.Bundle
import com.concordium.wallet.R
import com.concordium.wallet.data.model.BakerPoolInfo
import com.concordium.wallet.data.model.BakerPoolInfo.Companion.OPEN_STATUS_CLOSED_FOR_ALL
import com.concordium.wallet.data.model.BakerPoolInfo.Companion.OPEN_STATUS_CLOSED_FOR_NEW
import com.concordium.wallet.data.model.BakerPoolInfo.Companion.OPEN_STATUS_OPEN_FOR_ALL
import com.concordium.wallet.databinding.ActivityBakerUpdatePoolSettingsBinding
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.uicore.view.SegmentedControlView

class BakerUpdatePoolSettingsActivity : BaseDelegationBakerActivity(
    R.layout.activity_baker_update_pool_settings, R.string.baker_update_pool_settings_title
) {
    private lateinit var binding: ActivityBakerUpdatePoolSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBakerUpdatePoolSettingsBinding.bind(findViewById(R.id.root_layout))
        initViews()
    }

    override fun initViews() {
        super.initViews()

        viewModel.bakerDelegationData.account.baker?.bakerPoolInfo?.let {
            viewModel.selectOpenStatus(it)
        }

        binding.poolOptions.clearAll()

        binding.poolOptions.addControl(
            title = getString(R.string.baker_update_pool_settings_option_open),
            clickListener = object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectOpenStatus(BakerPoolInfo(OPEN_STATUS_OPEN_FOR_ALL))
                }
            },
            initiallySelected = viewModel.bakerDelegationData.account.baker?.bakerPoolInfo?.openStatus == OPEN_STATUS_OPEN_FOR_ALL
        )
        if (viewModel.bakerDelegationData.account.baker?.bakerPoolInfo?.openStatus != OPEN_STATUS_CLOSED_FOR_ALL) {
            binding.poolOptions.addControl(
                title = getString(R.string.baker_update_pool_settings_option_close_for_new),
                clickListener = object : SegmentedControlView.OnItemClickListener {
                    override fun onItemClicked() {
                        viewModel.selectOpenStatus(BakerPoolInfo(OPEN_STATUS_CLOSED_FOR_NEW))
                    }
                },
                initiallySelected = viewModel.bakerDelegationData.account.baker?.bakerPoolInfo?.openStatus == OPEN_STATUS_CLOSED_FOR_NEW
            )
        }
        binding.poolOptions.addControl(
            title = getString(R.string.baker_update_pool_settings_option_close),
            clickListener = object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectOpenStatus(BakerPoolInfo(OPEN_STATUS_CLOSED_FOR_ALL))
                }
            },
            initiallySelected = viewModel.bakerDelegationData.account.baker?.bakerPoolInfo?.openStatus == OPEN_STATUS_CLOSED_FOR_ALL
        )

        when (viewModel.bakerDelegationData.account.baker?.bakerPoolInfo?.openStatus) {
            OPEN_STATUS_OPEN_FOR_ALL -> binding.poolSettingsCurrentStatus.text =
                getString(R.string.baker_update_pool_settings_current_status_open)

            OPEN_STATUS_CLOSED_FOR_NEW -> binding.poolSettingsCurrentStatus.text =
                getString(R.string.baker_update_pool_settings_current_status_closed_for_new)

            else -> binding.poolSettingsCurrentStatus.text =
                getString(R.string.baker_update_pool_settings_current_status_closed)
        }

        binding.updatePoolSettingsContinue.setOnClickListener {
            gotoNextPage()
        }
    }

    private fun gotoNextPage() {
        val intent = Intent(this, BakerPoolSettingsActivity::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun errorLiveData(value: Int) {
    }
}
