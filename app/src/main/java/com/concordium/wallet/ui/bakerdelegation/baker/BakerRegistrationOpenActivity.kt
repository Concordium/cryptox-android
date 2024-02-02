package com.concordium.wallet.ui.bakerdelegation.baker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import com.concordium.wallet.R
import com.concordium.wallet.data.backend.repository.ProxyRepository.Companion.UPDATE_BAKER_POOL
import com.concordium.wallet.databinding.ActivityBakerRegistrationOpenBinding
import com.concordium.wallet.ui.bakerdelegation.common.BaseDelegationBakerActivity
import com.concordium.wallet.ui.bakerdelegation.common.DelegationBakerViewModel
import com.concordium.wallet.ui.common.GenericFlowActivity
import com.concordium.wallet.uicore.handleUrlClicks
import com.concordium.wallet.util.KeyboardUtil
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

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

        viewModel.bakerDelegationData.oldMetadataUrl =
            viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.metadataUrl

        binding.openUrl.doOnTextChanged { _, _, _, _ ->
            hideMetadataUrlInvalid()
        }

        if (viewModel.bakerDelegationData.type == UPDATE_BAKER_POOL) {
            setActionBarTitle(R.string.baker_update_pool_settings_title)
            binding.openUrlExplain.setText(R.string.baker_update_pool_settings_open_url_explain)
            viewModel.bakerDelegationData.account?.accountBaker?.bakerPoolInfo?.metadataUrl?.let {
                binding.currentUrl.text =
                    getString(R.string.baker_update_pool_settings_current_url, it)
                binding.currentUrl.visibility = View.VISIBLE
                binding.openUrl.setText(it)
            }
        }

        binding.readMoreTextView.handleUrlClicks { url ->
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            ContextCompat.startActivity(this, browserIntent, null)
        }

        binding.bakerRegistrationOpenContinue.setOnClickListener {
            KeyboardUtil.hideKeyboard(this)
            viewModel.bakerDelegationData.metadataUrl =
                binding.openUrl.text?.toString()?.trim()
            validate()
        }
    }

    private fun validate() {
        var gotoNextPage = false

        val metadataUrl = viewModel.bakerDelegationData.metadataUrl
        val isMetadataUrlValid = metadataUrl.isNullOrEmpty() || metadataUrl.toHttpUrlOrNull() != null

        if (isMetadataUrlValid
            && (viewModel.bakerDelegationData.oldMetadataUrl != metadataUrl
                    || viewModel.bakerDelegationData.oldOpenStatus != viewModel.bakerDelegationData.bakerPoolInfo?.openStatus
                    )
        ) {
            gotoNextPage = true
        }

        if (gotoNextPage) {
            gotoNextPage()
        } else if (!isMetadataUrlValid) {
            showMetadataUrlInvalid()
        } else {
            showNoChange()
        }
    }

    private fun gotoNextPage() {
        val intent = if (viewModel.bakerDelegationData.type == UPDATE_BAKER_POOL) {
            Intent(this, BakerRegistrationConfirmationActivity::class.java)
        } else {
            Intent(this, BakerRegistrationCloseActivity::class.java)
        }
        intent.putExtra(GenericFlowActivity.EXTRA_IGNORE_BACK_PRESS, false)
        intent.putExtra(
            DelegationBakerViewModel.EXTRA_DELEGATION_BAKER_DATA,
            viewModel.bakerDelegationData
        )
        startActivityForResultAndHistoryCheck(intent)
    }

    private fun showMetadataUrlInvalid() {
        binding.openUrlError.isVisible = true
    }

    private fun hideMetadataUrlInvalid() {
        binding.openUrlError.isVisible = false
    }

    override fun errorLiveData(value: Int) {
    }
}
