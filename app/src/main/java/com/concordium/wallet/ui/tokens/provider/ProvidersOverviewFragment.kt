package com.concordium.wallet.ui.tokens.provider

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.recyclerview.widget.LinearLayoutManager
import com.concordium.wallet.App
import com.concordium.wallet.Constants
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentProvidersOverviewBinding
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.tokens.add_provider.AddProviderActivity
import com.concordium.wallet.ui.tokens.wallets.WalletsOverviewActivity

class ProvidersOverviewFragment : BaseFragment(), ProviderItemView.IProviderItemView {
    private val providerPrefs = App.appCore.session.walletStorage.providerPreferences

    private lateinit var sharedViewModel: ProvidersOverviewViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var binding: FragmentProvidersOverviewBinding
    private var providersAdapter = ProvidersAdapter(this)

    private val addProviderResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                showWaiting(true)
                sharedViewModel.processAction(ProvidersViewAction.GetProviders(providerPrefs.getProviders()))
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProvidersOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
        initializeViews()

        (requireActivity() as BaseActivity).hideAddContact(isVisible = true) {
            addProviderResult.launch(Intent(requireContext(), AddProviderActivity::class.java))
        }

        (requireActivity() as BaseActivity).hideQrScan(isVisible = false)
    }

    private fun initializeViewModel() {
        sharedViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[ProvidersOverviewViewModel::class.java]

        sharedViewModel.onAccountReady().observe(viewLifecycleOwner) { event ->
            event.contentOrNullIfUsed?.let { _ ->
                sharedViewModel.processAction(ProvidersViewAction.GetProviders(providerPrefs.getProviders()))
            }
        }

        sharedViewModel.onProvidersReady().observe(viewLifecycleOwner) { event ->
            event.contentOrNullIfUsed?.let { providers ->
                providersAdapter.setData(providers)
                providersAdapter.notifyDataSetChanged()
                showWaiting(false)
            }
        }

        sharedViewModel.processAction(ProvidersViewAction.GetAccount)

        mainViewModel = ViewModelProvider(requireActivity()).get()
    }

    private fun initializeViews() {
        mainViewModel.setTitle(getString(R.string.app_menu_item_tokens))
        showWaiting(true)
        with(binding.providersPool) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = providersAdapter
        }
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    override fun showProvider(provider: ProviderMeta) {
        Intent(requireContext(), WalletsOverviewActivity::class.java).apply {
            putExtra(Constants.Extras.EXTRA_PROVIDER_DATA, provider)
            startActivity(this)
        }
    }

    override fun deleteProvider(provider: ProviderMeta) {
        showWaiting(true)
        val delRes = providerPrefs.removeProvider(provider)
        if (delRes) {
            sharedViewModel.processAction(ProvidersViewAction.GetProviders(providerPrefs.getProviders()))
        } else {
            Toast.makeText(requireContext(), "Unable to delete provider", Toast.LENGTH_SHORT).show()
        }
    }
}
