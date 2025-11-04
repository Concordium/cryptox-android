package com.concordium.wallet.ui.onramp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentCcdOnrampSitesBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.uicore.toast.showCustomToast
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class CcdOnrampSitesFragment : BaseFragment() {

    private lateinit var binding: FragmentCcdOnrampSitesBinding
    private lateinit var adapter: CcdOnrampItemAdapter

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity())[MainViewModel::class.java]
    }

    private val viewModel: CcdOnrampSitesViewModel by viewModel {
        parametersOf(mainViewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentCcdOnrampSitesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initList()
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
        viewModel.siteToOpen.collectWhenStarted(this) { (site, copyAddress) ->
            if (viewModel.isHasAcceptedOnRampDisclaimer().not()) {
                showDisclaimerDialog(
                    siteToOpen = site,
                    copyAddress = copyAddress,
                )
            } else {
                openSite(
                    site = site,
                    copyAddress = copyAddress,
                )
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

    private fun onSiteClicked(site: CcdOnrampSite) {
        App.appCore.tracker.homeOnrampSiteClicked(siteName = site.name)
        viewModel.onSiteClicked(site)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loading.progressBar.isVisible = isLoading
    }

    private fun openSite(
        site: CcdOnrampSite,
        copyAddress: Boolean,
    ) {
        if (copyAddress) {
            val clipboardManager: ClipboardManager =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(
                getString(R.string.account_details_address),
                viewModel.accountAddress.value,
            )
            requireContext().showCustomToast(
                iconResId = R.drawable.mw24_ic_address_copy_check,
                title = getString(
                    R.string.template_ccd_onramp_opening_site,
                    site.name
                )
            )
            clipboardManager.setPrimaryClip(clipData)
        }

        val browserIntent = Intent(Intent.ACTION_VIEW, site.url.toUri())
        startActivity(Intent.createChooser(browserIntent, site.name))
    }

    private fun showDisclaimerDialog(
        siteToOpen: CcdOnrampSite? = null,
        copyAddress: Boolean = false,
    ) {
        parentFragmentManager.setFragmentResultListener(
            CcdOnrampDisclaimerDialog.ACTION_REQUEST,
            viewLifecycleOwner
        ) { _, bundle ->
            if (CcdOnrampDisclaimerDialog.getResult(bundle)) {
                viewModel.setHasAcceptedOnRampDisclaimer(true)
                adapter.updateHeaderDisclaimerButton(viewModel.isHasAcceptedOnRampDisclaimer())
                if (siteToOpen != null) {
                    openSite(
                        site = siteToOpen,
                        copyAddress = copyAddress,
                    )
                }
            }
        }

        CcdOnrampDisclaimerDialog.newInstance(
            CcdOnrampDisclaimerDialog.setBundle(
                isDisclaimerAccepted = viewModel.isHasAcceptedOnRampDisclaimer(),
                showSiteIcon = siteToOpen != null,
            )
        ).showSingle(parentFragmentManager, CcdOnrampDisclaimerDialog.TAG)
    }
}
