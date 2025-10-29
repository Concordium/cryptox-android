package com.concordium.wallet.ui.onramp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.App
import com.concordium.wallet.databinding.FragmentCcdOnrampSitesBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.base.BaseFragment

class CcdOnrampSitesFragment : BaseFragment() {

    private lateinit var binding: FragmentCcdOnrampSitesBinding
    private lateinit var adapter: CcdOnrampItemAdapter

    private val viewModel: CcdOnrampSitesViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCcdOnrampSitesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.initialize(
            accountAddress = requireActivity().intent.getStringExtra(ACCOUNT_ADDRESS_EXTRA) ?: "",
        )
        initList()
        initObservers()
        initToolbar()
    }

    override fun onResume() {
        super.onResume()
        App.appCore.tracker.homeOnrampScreen()
    }

    private fun initToolbar() {
        val baseActivity = (activity as BaseActivity)
        baseActivity.hideQrScan(isVisible = false)
        baseActivity.hideSettings(isVisible = false)
    }

    private fun initList() {
        adapter = CcdOnrampItemAdapter(
            onSiteClicked = { item: CcdOnrampListItem.Site ->
                item.source?.also(::onSiteClicked)
            },
            onReadDisclaimerClicked = { showDisclaimerDialog() },
            isDisclaimerAccepted = viewModel.isHasAcceptedOnRampDisclaimer()
        )
        binding.recyclerview.adapter = adapter
        viewModel.listItemsLiveData.observe(viewLifecycleOwner, adapter::setData)
        viewModel.siteToOpen.collectWhenStarted(this) { site ->
            site?.let {
                if (viewModel.isHasAcceptedOnRampDisclaimer().not()) {
                    viewModel.launchSite = it
                    showDisclaimerDialog(showSiteIcon = true)
                } else {
                    openSite(launchSite = it)
                }
            }
        }
        viewModel.sessionLoading.collectWhenStarted(this) { isLoading ->
            showLoading(isLoading)
        }
        viewModel.error.collectWhenStarted(this) { error ->
            if (error != -1) {
                Toast.makeText(requireActivity(), getString(error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initObservers() {
        parentFragmentManager.setFragmentResultListener(
            CcdOnrampDisclaimerDialog.ACTION_REQUEST,
            viewLifecycleOwner
        ) { _, bundle ->
            if (CcdOnrampDisclaimerDialog.getResult(bundle)) {
                viewModel.setHasAcceptedOnRampDisclaimer(true)
                adapter.updateHeaderDisclaimerButton(viewModel.isHasAcceptedOnRampDisclaimer())
                viewModel.launchSite?.let(::openSite)
            } else {
                viewModel.launchSite = null
            }
        }
    }

    private fun onSiteClicked(site: CcdOnrampSite) {
        App.appCore.tracker.homeOnrampSiteClicked(siteName = site.name)
        viewModel.onSiteClicked(site)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.progressBar.isVisible = isLoading
    }

    private fun openSite(launchSite: CcdOnrampSite) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(launchSite.url))
        startActivity(Intent.createChooser(browserIntent, launchSite.name))
    }

    private fun showDisclaimerDialog(showSiteIcon: Boolean = false) {
        CcdOnrampDisclaimerDialog.newInstance(
            CcdOnrampDisclaimerDialog.setBundle(
                isDisclaimerAccepted = viewModel.isHasAcceptedOnRampDisclaimer(),
                showSiteIcon = showSiteIcon
            )
        ).showSingle(parentFragmentManager, CcdOnrampDisclaimerDialog.TAG)
    }

    companion object {
        private const val ACCOUNT_ADDRESS_EXTRA = "account_address"

        fun getBundle(accountAddress: String?) = Bundle().apply {
            putString(ACCOUNT_ADDRESS_EXTRA, accountAddress)
        }
    }
}