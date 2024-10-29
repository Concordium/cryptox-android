package com.concordium.wallet.ui.identity.identitiesoverview

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.data.room.Identity
import com.concordium.wallet.databinding.ActivityIdentitiesOverviewBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.IdentityAdapter
import com.concordium.wallet.ui.identity.identitydetails.IdentityDetailsActivity
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity
import com.concordium.wallet.ui.multiwallet.FileWalletCreationLimitationDialog

class IdentitiesOverviewActivity : BaseActivity(
    R.layout.activity_identities_overview,
    R.string.identities_overview_title
) {
    private lateinit var viewModel: IdentitiesOverviewViewModel
    private val binding: ActivityIdentitiesOverviewBinding by lazy {
        ActivityIdentitiesOverviewBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var identityAdapter: IdentityAdapter

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeViewModel()
        viewModel.initialize()
        initializeViews()

        hideActionBarBack(isVisible = true)

        hideRightPlus(
            isVisible = true,
            isDisabled = viewModel.isCreationLimitedForFileWallet,
            hasNotice = viewModel.isCreationLimitedForFileWallet,
        ) {
            onCreateClicked()
        }
    }
    // endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[IdentitiesOverviewViewModel::class.java]

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModel.identityListLiveData.observe(this) { identityList ->
            identityList?.let {
                identityAdapter.setData(it)
                showWaiting(false)
                binding.noIdentityLayout.isVisible = identityList.isEmpty()
            }
        }
    }

    private fun initializeViews() {
        binding.progress.progressLayout.visibility = View.VISIBLE
        binding.noIdentityLayout.visibility = View.GONE

        binding.newIdentityButton.setOnClickListener {
            onCreateClicked()
        }

        initializeList()
    }

    private fun initializeList() {
        identityAdapter = IdentityAdapter()
        binding.identityRecyclerview.setHasFixedSize(true)
        binding.identityRecyclerview.adapter = identityAdapter

        identityAdapter.setOnItemClickListener(object : IdentityAdapter.OnItemClickListener {
            override fun onItemClicked(item: Identity) {
                gotoIdentityDetails(item)
            }
        })
    }
    // endregion

    //region Control/UI
    // ************************************************************

    private fun onCreateClicked() {
        if (viewModel.isCreationLimitedForFileWallet) {
            showFileWalletCreationLimitation()
        } else {
            gotoCreateIdentity()
        }
    }

    private fun showFileWalletCreationLimitation() {
        FileWalletCreationLimitationDialog().showSingle(
            supportFragmentManager,
            FileWalletCreationLimitationDialog.TAG
        )
    }

    private fun gotoCreateIdentity() {
        val intent = Intent(this, IdentityProviderListActivity::class.java)
        startActivity(intent)
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    private fun gotoIdentityDetails(identity: Identity) {
        val intent = Intent(this, IdentityDetailsActivity::class.java)
        intent.putExtra(
            IdentityDetailsActivity.EXTRA_IDENTITY, identity
        )
        startActivity(intent)
    }

    //endregion
}
