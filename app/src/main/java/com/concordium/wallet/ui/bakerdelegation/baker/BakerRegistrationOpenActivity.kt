package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_POOL
import com.concordium.wallet.databinding.ActivityBakerRegistrationOpenBinding
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.uicore.handleUrlClicks
import com.concordium.wallet.util.KeyboardUtil

class BakerRegistrationOpenActivity : BaseDelegationBakerActivity(
    R.layout.activity_baker_registration_open, R.string.baker_registration_open_title
) {
    private lateinit var binding: ActivityBakerRegistrationOpenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBakerRegistrationOpenBinding.bind(findViewById(R.id.root_layout))
        hideActionBarBack(isVisible = true)
        initViews()
    }

    override fun initViews() {
        super.initViews()

        if (viewModel.bakerDelegationData.type == UPDATE_BAKER_POOL) {
            setActionBarTitle(R.string.baker_update_pool_settings_title)
            binding.openUrlExplain.setText(R.string.baker_update_pool_settings_open_url_explain)
            viewModel.bakerDelegationData.account.baker?.bakerPoolInfo?.metadataUrl?.let {
                binding.currentUrl.text =
                    getString(R.string.baker_update_pool_settings_current_url, it)
                binding.currentUrl.visibility = View.VISIBLE
                binding.openUrl.setText(it)
            }
        }

        fun onUrlClicked(url: String) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            ContextCompat.startActivity(this, browserIntent, null)
        }

        binding.openUrlExplain.handleUrlClicks(::onUrlClicked)
        binding.readMoreTextView.handleUrlClicks(::onUrlClicked)

        binding.bakerRegistrationOpenContinue.setOnClickListener {
            KeyboardUtil.hideKeyboard(this)
            viewModel.bakerDelegationData.metadataUrl = binding.openUrl.text?.toString()
            validate()
        }
    }

    private fun validate() {
        val gotoNextPage =
            if (viewModel.bakerDelegationData.oldMetadataUrl != viewModel.bakerDelegationData.metadataUrl ||
                viewModel.bakerDelegationData.oldOpenStatus != viewModel.bakerDelegationData.bakerPoolInfo?.openStatus
            ) {
                true
            } else {
                viewModel.bakerDelegationData.oldCommissionRates?.transactionCommission != viewModel.bakerDelegationData.transactionCommissionRate ||
                        viewModel.bakerDelegationData.oldCommissionRates?.bakingCommission != viewModel.bakerDelegationData.bakingCommissionRate
            }
        if (gotoNextPage) gotoNextPage()
        else showNoChange()
    }


    private fun gotoNextPage() {
        val intent = if (viewModel.bakerDelegationData.type == UPDATE_BAKER_POOL) {
            Intent(this, BakerRegistrationConfirmationActivity::class.java)
        } else {
            Intent(this, BakerRegistrationCloseActivity::class.java)
        }
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    override fun errorLiveData(value: Int) {
    }
}
