package com.concordium.wallet.ui.onramp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentCcdOnrampSitesBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.uicore.toast.showCustomToast
import com.concordium.wallet.util.ClipboardUtil
import com.concordium.wallet.util.IntentUtil
import com.concordium.wallet.util.getOptionalSerializable
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
            onReadDisclaimerClicked = { gotoDisclaimer() },
            isDisclaimerAccepted = viewModel.isHasAcceptedOnRampDisclaimer()
        )
        binding.recyclerview.adapter = adapter
        viewModel.listItemsLiveData.observe(viewLifecycleOwner, adapter::setData)
        viewModel.siteToOpen.collectWhenStarted(this) { (site, copyAddress) ->
            if (viewModel.isHasAcceptedOnRampDisclaimer().not()) {
                gotoDisclaimer(
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

    private val getResultDisclaimer =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { data ->
                    val isDisclaimerAccepted = data.getBooleanExtra(
                        CcdOnrampDisclaimerActivity.DISCLAIMER_ACCEPTED,
                        false
                    )
                    val siteToOpen = data.getOptionalSerializable(
                        CcdOnrampDisclaimerActivity.SITE_TO_OPEN,
                        CcdOnrampSite::class.java
                    )
                    val copyAddress = data.getBooleanExtra(
                        CcdOnrampDisclaimerActivity.COPY_ADDRESS,
                        false
                    )
                    handleDisclaimerAccepted(
                        isDisclaimerAccepted,
                        siteToOpen,
                        copyAddress
                    )
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
            requireContext().showCustomToast(
                iconResId = R.drawable.mw24_ic_address_copy_check,
                title = getString(
                    R.string.template_ccd_onramp_opening_site,
                    site.name
                )
            )
            ClipboardUtil.copyToClipboard(
                context = requireContext(),
                label = getString(R.string.account_details_address),
                text = viewModel.accountAddress.value
            )
        }
        IntentUtil.openUrl(requireActivity(), site.url)
    }

    private fun handleDisclaimerAccepted(
        isAccepted: Boolean,
        siteToOpen: CcdOnrampSite? = null,
        copyAddress: Boolean = false
    ) {
        if (isAccepted) {
            viewModel.setHasAcceptedOnRampDisclaimer(true)
            adapter.updateHeaderDisclaimerButton(viewModel.isHasAcceptedOnRampDisclaimer())
            siteToOpen?.let {
                openSite(
                    site = siteToOpen,
                    copyAddress = copyAddress
                )
            }
        }
    }

    private fun gotoDisclaimer(
        siteToOpen: CcdOnrampSite? = null,
        copyAddress: Boolean = false
    ) {
        val intent = Intent(requireActivity(), CcdOnrampDisclaimerActivity::class.java).apply {
            putExtra(
                CcdOnrampDisclaimerActivity.IS_DISCLAIMER_ACCEPTED,
                viewModel.isHasAcceptedOnRampDisclaimer()
            )
            putExtra(
                CcdOnrampDisclaimerActivity.SHOW_SITE_ICON,
                siteToOpen != null
            )
            putExtra(
                CcdOnrampDisclaimerActivity.COPY_ADDRESS,
                copyAddress
            )
            siteToOpen?.let { site ->
                putExtra(
                    CcdOnrampDisclaimerActivity.SITE_TO_OPEN,
                    site
                )
            }
        }
        getResultDisclaimer.launch(intent)
    }
}
