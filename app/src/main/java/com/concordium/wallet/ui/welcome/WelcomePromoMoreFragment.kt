package com.concordium.wallet.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentWelcomePromoMoreBinding
import com.concordium.wallet.extension.collect
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.more.about.AboutActivity
import com.concordium.wallet.ui.more.alterpassword.AlterPasswordActivity
import com.concordium.wallet.ui.more.moreoverview.MoreOverviewViewModel
import com.concordium.wallet.ui.more.tracking.TrackingPreferencesActivity
import com.concordium.wallet.ui.seed.reveal.SavedSeedPhraseRevealActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class WelcomePromoMoreFragment : Fragment() {
    private lateinit var binding: FragmentWelcomePromoMoreBinding
    private lateinit var viewModel: MoreOverviewViewModel
    private lateinit var promoViewModel: WelcomePromoViewModel

    private val activity: BaseActivity
        get() = requireActivity() as BaseActivity
    private val authDelegate: AuthDelegate
        get() = requireActivity() as AuthDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        promoViewModel = ViewModelProvider(requireActivity()).get()
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWelcomePromoMoreBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.initialize()
        initializeViews()
        subscribeToEvents()
    }

    private fun initializeViews() {
        viewModel.seedPhraseRevealVisibilityLiveData.observe(
            viewLifecycleOwner,
            binding.seedPhraseRevealLayout::isVisible::set
        )
        binding.seedPhraseRevealLayout.setOnClickListener {
            revealSeedPhrase()
        }

        viewModel.passwordAlterVisibilityLiveData.observe(
            viewLifecycleOwner,
            binding.alterLayout::isVisible::set
        )
        binding.alterLayout.setOnClickListener {
            alterPassword()
        }

        binding.trackingLayout.setOnClickListener {
            openTrackingPreferences()
        }

        binding.aboutLayout.setOnClickListener {
            about()
        }

        viewModel.walletEraseVisibilityLiveData.observe(
            viewLifecycleOwner,
            binding.eraseWalletLayout::isVisible::set
        )
        binding.eraseWalletLayout.setOnClickListener {
            eraseWalletAndExit()
        }
    }

    private fun subscribeToEvents() = viewModel.eventsFlow.collect(this) { event ->
        when (event) {
            MoreOverviewViewModel.Event.ShowAuthentication ->
                authDelegate.showAuthentication(
                    activity = activity,
                    onAuthenticated = viewModel::onAuthenticated
                )

            MoreOverviewViewModel.Event.ShowDataErasedMessage ->
                Toast.makeText(
                    requireContext(),
                    R.string.more_overview_erase_data_erased,
                    Toast.LENGTH_SHORT
                )
                    .show()

            MoreOverviewViewModel.Event.GoToWelcome -> {
                requireActivity().finishAffinity()
                startActivity(Intent(requireContext(), WelcomeActivity::class.java))
            }
        }
    }

    private fun revealSeedPhrase() {
        val intent = Intent(activity, SavedSeedPhraseRevealActivity::class.java)
        startActivity(intent)
    }

    private fun eraseWalletAndExit() {
        showConfirmEraseWallet()
    }

    private fun showConfirmEraseWallet() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.more_overview_erase_data_confirmation_title)
            .setMessage(getString(R.string.more_overview_erase_wallet_confirmation_message))
            .setPositiveButton(getString(R.string.more_overview_erase_data_continue)) { _, _ ->
                viewModel.onEraseContinueClicked()
            }
            .setNegativeButton(getString(R.string.wallet_connect_clear_data_warning_cancel), null)
            .show()
    }

    private fun openTrackingPreferences() {
        val intent  = Intent(activity, TrackingPreferencesActivity::class.java)
        startActivity(intent)
    }

    private fun about() {
        val intent = Intent(activity, AboutActivity::class.java)
        startActivity(intent)
    }

    private fun alterPassword() {
        val intent = Intent(activity, AlterPasswordActivity::class.java)
        startActivity(intent)
    }
}
