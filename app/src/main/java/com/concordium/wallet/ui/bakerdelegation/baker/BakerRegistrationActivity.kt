package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.os.Bundle
import com.concordium.wallet.R
import com.concordium.wallet.data.model.BakerPoolInfo
import com.concordium.wallet.data.model.BakerPoolInfo.Companion.OPEN_STATUS_CLOSED_FOR_ALL
import com.concordium.wallet.data.model.BakerPoolInfo.Companion.OPEN_STATUS_OPEN_FOR_ALL
import com.concordium.wallet.databinding.ActivityBakerRegistrationBinding
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.uicore.view.SegmentedControlView

class BakerRegistrationActivity : BaseDelegationBakerActivity(
    R.layout.activity_baker_registration, R.string.baker_registration_title
) {
    private lateinit var binding: ActivityBakerRegistrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBakerRegistrationBinding.bind(findViewById(R.id.root_layout))
        hideActionBarBack(isVisible = true)
        initViews()
    }

    override fun initViews() {
        binding.bakerOptions.clearAll()
        binding.bakerOptions.addControl(
            title = getString(R.string.baker_registration_open),
            clickListener = object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectOpenStatus(BakerPoolInfo(OPEN_STATUS_OPEN_FOR_ALL))
                }
            },
            initiallySelected = viewModel.bakerDelegationData.bakerPoolStatus?.poolInfo?.openStatus == OPEN_STATUS_OPEN_FOR_ALL || viewModel.bakerDelegationData.bakerPoolStatus?.poolInfo?.openStatus == null
        )
        binding.bakerOptions.addControl(
            title = getString(R.string.baker_registration_close),
            clickListener = object : SegmentedControlView.OnItemClickListener {
                override fun onItemClicked() {
                    viewModel.selectOpenStatus(BakerPoolInfo(OPEN_STATUS_CLOSED_FOR_ALL))
                }
            },
            initiallySelected = viewModel.bakerDelegationData.bakerPoolStatus?.poolInfo?.openStatus == OPEN_STATUS_CLOSED_FOR_ALL
        )

        binding.bakerRegistrationContinue.setOnClickListener {
            onContinueClicked()
        }
    }

    private fun onContinueClicked() {
        val intent =
            if (viewModel.bakerDelegationData.bakerPoolInfo?.openStatus == OPEN_STATUS_CLOSED_FOR_ALL)
                Intent(this, BakerRegistrationCloseActivity::class.java)
            else
                Intent(this, BakerPoolSettingsActivity::class.java)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun errorLiveData(value: Int) {
    }
}
