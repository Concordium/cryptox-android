package com.concordium.wallet.ui.identity.identityproviderlist

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.IdentityProvider
import com.concordium.wallet.databinding.ActivityIdentityProviderListBinding
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.identity.identityproviderpolicywebview.IdentityProviderPolicyWebViewActivity
import com.concordium.wallet.ui.identity.identityproviderwebview.IdentityProviderWebviewActivity

class IdentityProviderListActivity : BaseActivity(
    R.layout.activity_identity_provider_list,
    R.string.identity_provider_list_title
), AuthDelegate by AuthDelegateImpl() {
    private lateinit var binding: ActivityIdentityProviderListBinding
    private lateinit var viewModel: IdentityProviderListViewModel
    private val identityProviderAdapter = IdentityProviderAdapter(emptyList())
    private var showForFirstIdentity = false

    companion object {
        const val SHOW_FOR_FIRST_IDENTITY = "SHOW_FOR_FIRST_IDENTITY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentityProviderListBinding.bind(findViewById(R.id.toastLayoutTopError))

        showForFirstIdentity = intent.getBooleanExtra(SHOW_FOR_FIRST_IDENTITY, false)

        setActionBarTitle("")
        hideActionBarBack(isVisible = !showForFirstIdentity)

        initializeViewModel()
        initializeViews()
        viewModel.getIdentityProviders()
        viewModel.getGlobalInfo()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[IdentityProviderListViewModel::class.java]

        if (showForFirstIdentity) {
            viewModel.checkNotFileWallet()
        }

        viewModel.waitingLiveData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting || viewModel.waitingGlobalData.value!!)
            }
        }
        viewModel.waitingGlobalData.observe(this) { waiting ->
            waiting?.let {
                showWaiting(waiting || viewModel.waitingLiveData.value!!)
            }
        }
        viewModel.errorLiveData.observe(this, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
        viewModel.showAuthenticationLiveData.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    showAuthentication(
                        activity = this@IdentityProviderListActivity,
                        onAuthenticated = viewModel::continueWithPassword
                    )
                }
            }
        })
        viewModel.gotoIdentityProviderWebView.observe(this, object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (value) {
                    gotoIdentityProviderWebView()
                    showWaiting(false)
                }
            }
        })
    }

    private fun initializeViews() {
        binding.progress.progressLayout.visibility = View.GONE
        initializeList()
    }

    private fun initializeList() {
        identityProviderAdapter.setOnItemClickListener(object :
            IdentityProviderAdapter.OnItemClickListener {
            override fun onItemClicked(item: IdentityProvider) {
                viewModel.selectedIdentityVerificationItem(item)
            }

            override fun onItemActionClicked(item: IdentityProvider) {
                gotoIdentityProviderPolicyWebView(item)
            }
        })
        binding.recyclerview.adapter = identityProviderAdapter

        viewModel.identityProviderList.observe(this, identityProviderAdapter::setData)
    }

    private fun gotoIdentityProviderWebView() {
        viewModel.getIdentityCreationData()?.let { identityCreationData ->
            val intent = Intent(this, IdentityProviderWebviewActivity::class.java)
            intent.putExtra(
                IdentityProviderWebviewActivity.EXTRA_IDENTITY_CREATION_DATA,
                identityCreationData
            )
            if (showForFirstIdentity)
                intent.putExtra(
                    IdentityProviderWebviewActivity.EXTRA_SHOW_FOR_FIRST_IDENTITY,
                    true
                )
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    private fun gotoIdentityProviderPolicyWebView(identityProvider: IdentityProvider) {
        identityProvider.metadata
        val intent = Intent(this, IdentityProviderPolicyWebViewActivity::class.java)
        intent.putExtra(IdentityProviderPolicyWebViewActivity.EXTRA_URL, "https://google.com")
        startActivity(intent)
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }
}
