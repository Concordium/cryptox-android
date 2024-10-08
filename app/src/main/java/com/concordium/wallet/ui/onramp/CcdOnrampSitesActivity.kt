package com.concordium.wallet.ui.onramp

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityCcdOnrampSitesBinding
import com.concordium.wallet.ui.base.BaseActivity

class CcdOnrampSitesActivity : BaseActivity(
    R.layout.activity_ccd_onramp_sites,
    R.string.ccd_onramp_title,
) {
    private val binding: ActivityCcdOnrampSitesBinding by lazy {
        ActivityCcdOnrampSitesBinding.bind(findViewById(R.id.root_layout))
    }
    private val viewModel: CcdOnrampSitesViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()
    }

    private lateinit var chromeClient: SwipeluxWebChromeClient
    private lateinit var permissionManager: PermissionManager

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            chromeClient.handleFileChooserResult(result.resultCode, result.data)
        }

    private val permissionResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted =
            permissions[Manifest.permission.CAMERA] == true && permissions[Manifest.permission.RECORD_AUDIO] == true
        if (!granted) {
            Toast.makeText(
                this,
                "Camera and microphone permissions are required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.initialize(
            accountAddress = intent.getStringExtra(ACCOUNT_ADDRESS_EXTRA),
        )
        permissionManager = PermissionManager(this, permissionResultLauncher)
        initList()

        hideActionBarBack(isVisible = true)
        hideDisclaimer(
            isVisible = true,
            listener = {
                binding.recyclerview.smoothScrollToPosition(
                    viewModel.listItemsLiveData.value!!.indexOf(
                        CcdOnrampListItem.Disclaimer
                    )
                )
            }
        )
    }

    private fun initList() {
        chromeClient = SwipeluxWebChromeClient(this, filePickerLauncher, permissionManager)

        val adapter = CcdOnrampItemAdapter(
            onSiteClicked = { item: CcdOnrampListItem.Site ->
                item.source?.also(::onSiteClicked)
            },
            onReadDisclaimerClicked = {
                binding.recyclerview.smoothScrollToPosition(
                    viewModel.listItemsLiveData.value!!.indexOf(
                        CcdOnrampListItem.Disclaimer
                    )
                )
            },
            chromeClient = chromeClient,
            context = this@CcdOnrampSitesActivity
        )
        binding.recyclerview.adapter = adapter
        viewModel.listItemsLiveData.observe(this, adapter::setData)
    }

    private fun onSiteClicked(site: CcdOnrampSite) {
        val accountAddress = viewModel.accountAddress

        if (accountAddress != null) {
            OpenCcdOnrampSiteWithAccountUseCase(
                site = site,
                accountAddress = accountAddress,
                onAccountAddressCopied = {
                    Toast
                        .makeText(
                            this,
                            getString(R.string.template_ccd_onramp_opening_site, site.name),
                            Toast.LENGTH_SHORT
                        )
                        .show()
                },
                context = this,
            ).invoke()
        } else {
            val intent = Intent(this, CcdOnrampAccountsActivity::class.java)
            intent.putExtras(
                CcdOnrampAccountsActivity.getBundle(
                    site = site,
                )
            )
            startActivity(intent)
        }
    }

    companion object {
        private const val ACCOUNT_ADDRESS_EXTRA = "account_address"

        fun getBundle(accountAddress: String?) = Bundle().apply {
            putString(ACCOUNT_ADDRESS_EXTRA, accountAddress)
        }
    }
}
