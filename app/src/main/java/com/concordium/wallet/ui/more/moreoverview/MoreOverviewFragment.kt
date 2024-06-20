package com.concordium.wallet.ui.more.moreoverview

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentMoreOverviewBinding
import com.concordium.wallet.extension.collect
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.identity.identitiesoverview.IdentitiesOverviewActivity
import com.concordium.wallet.ui.more.about.AboutActivity
import com.concordium.wallet.ui.more.alterpassword.AlterPasswordActivity
import com.concordium.wallet.ui.more.dev.DevActivity
import com.concordium.wallet.ui.more.export.ExportActivity
import com.concordium.wallet.ui.more.import.ImportActivity
import com.concordium.wallet.ui.more.notifications.NotificationsPreferencesActivity
import com.concordium.wallet.ui.more.tracking.TrackingPreferencesActivity
import com.concordium.wallet.ui.more.unshielding.UnshieldingAccountsActivity
import com.concordium.wallet.ui.passphrase.recoverprocess.RecoverProcessActivity
import com.concordium.wallet.ui.passphrase.reveal.SavedPassPhraseRevealActivity
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListActivity
import com.concordium.wallet.ui.welcome.WelcomeActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MoreOverviewFragment : BaseFragment() {

    private lateinit var viewModel: MoreOverviewViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var binding: FragmentMoreOverviewBinding

    private val activity: BaseActivity
        get() = requireActivity() as BaseActivity
    private val authDelegate: AuthDelegate
        get() = requireActivity() as AuthDelegate

    //region Lifecycle
    // ************************************************************

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMoreOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
        viewModel.initialize()

        initializeViews()
        subscribeToEvents()

        (requireActivity() as BaseActivity).hideLeftPlus(isVisible = false)
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateOptionsVisibility()
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[MoreOverviewViewModel::class.java]

        mainViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[MainViewModel::class.java]

        viewModel.waitingLiveData.observe(viewLifecycleOwner) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
    }

    private fun initializeViews() {
        binding.progress.progressLayout.visibility = View.GONE
        mainViewModel.setTitle(getString(R.string.more_overview_title))

        binding.devLayout.visibility = View.GONE
        binding.devLayout.setOnClickListener {
            gotoDevConfig()
        }
        if (BuildConfig.INCL_DEV_OPTIONS) {
            binding.devLayout.visibility = View.VISIBLE
        }

        binding.unshieldingLayout.setOnClickListener {
            gotoUnshielding()
        }
        viewModel.unshieldingVisibilityLiveData.observe(
            viewLifecycleOwner,
            binding.unshieldingLayout::isVisible::set
        )

        binding.identitiesLayout.setOnClickListener {
            gotoIdentitiesOverview()
        }

        binding.addressBookLayout.setOnClickListener {
            gotoAddressBook()
        }

        binding.notificationsLayout.setOnClickListener {
            openNotificationsPreferences()
        }

        binding.exportLayout.setOnClickListener {
            gotoExport()
        }
        viewModel.fileImportExportVisibilityLiveData.observe(
            viewLifecycleOwner,
            binding.exportLayout::isVisible::set
        )

        binding.importLayout.setOnClickListener {
            import()
        }
        viewModel.fileImportExportVisibilityLiveData.observe(
            viewLifecycleOwner,
            binding.importLayout::isVisible::set
        )

        binding.recoverLayout.setOnClickListener {
            recover()
        }
        viewModel.seedRecoveryVisibilityLiveData.observe(
            viewLifecycleOwner,
            binding.recoverLayout::isVisible::set
        )

        binding.seedPhraseRevealLayout.setOnClickListener {
            revealSeedPhrase()
        }
        viewModel.seedPhraseRevealVisibilityLiveData.observe(
            viewLifecycleOwner,
            binding.seedPhraseRevealLayout::isVisible::set
        )

        binding.trackingLayout.setOnClickListener {
            openTrackingPreferences()
        }

        binding.aboutLayout.setOnClickListener {
            about()
        }

        binding.alterLayout.setOnClickListener {
            alterPassword()
        }

        binding.walletConnectLayout.setOnClickListener {
            clearWalletConnectAndRestart()
        }

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

            MoreOverviewViewModel.Event.ShowWalletErasedMessage ->
                Toast.makeText(
                    requireContext(),
                    R.string.more_overview_erase_wallet_erased,
                    Toast.LENGTH_SHORT
                )
                    .show()

            MoreOverviewViewModel.Event.GoToWelcome -> {
                requireActivity().finishAffinity()
                startActivity(Intent(requireContext(), WelcomeActivity::class.java))
            }
        }
    }
    //endregion

    //region Control/UI
    // ************************************************************

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    private fun clearWalletConnectAndRestart() {
        showConfirmDeleteWalletConnect()
    }

    private fun showConfirmDeleteWalletConnect() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(R.string.wallet_connect_clear_data_warning_title)
        builder.setMessage(getString(R.string.wallet_connect_clear_data_warning_message))
        builder.setPositiveButton(getString(R.string.wallet_connect_clear_data_warning_ok)) { _, _ ->
            viewModel.deleteWCDatabaseAndExit()
            Toast.makeText(
                requireContext(),
                R.string.wallet_connect_database_cleared,
                Toast.LENGTH_SHORT
            ).show()
        }
        builder.setNegativeButton(getString(R.string.wallet_connect_clear_data_warning_cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun eraseWalletAndExit() {
        showConfirmEraseWallet()
    }

    private fun showConfirmEraseWallet() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.more_overview_erase_wallet_confirmation_title)
            .setMessage(getString(R.string.more_overview_erase_wallet_confirmation_message))
            .setPositiveButton(getString(R.string.more_overview_erase_wallet_continue)) { _, _ ->
                viewModel.onEraseContinueClicked()
            }
            .setNegativeButton(getString(R.string.wallet_connect_clear_data_warning_cancel), null)
            .show()
    }

    private fun gotoDevConfig() {
        val intent = Intent(activity, DevActivity::class.java)
        startActivity(intent)
    }

    private fun gotoUnshielding() {
        val intent = Intent(activity, UnshieldingAccountsActivity::class.java)
        startActivity(intent)
    }

    private fun gotoIdentitiesOverview() {
        val intent = Intent(activity, IdentitiesOverviewActivity::class.java)
        startActivity(intent)
    }

    private fun gotoAddressBook() {
        val intent = Intent(activity, RecipientListActivity::class.java)
        startActivity(intent)
    }

    private fun gotoExport() {
        val intent = Intent(activity, ExportActivity::class.java)
        startActivity(intent)
    }

    private fun import() {
        val intent = Intent(activity, ImportActivity::class.java)
        startActivity(intent)
    }

    private fun recover() {
        val intent = Intent(activity, RecoverProcessActivity::class.java)
        intent.putExtra(RecoverProcessActivity.SHOW_FOR_FIRST_RECOVERY, false)
        startActivity(intent)
    }

    private fun revealSeedPhrase() {
        val intent = Intent(activity, SavedPassPhraseRevealActivity::class.java)
        startActivity(intent)
    }

    private fun openTrackingPreferences() {
        val intent = Intent(activity, TrackingPreferencesActivity::class.java)
        startActivity(intent)
    }

    private fun openNotificationsPreferences() {
        val intent = Intent(activity, NotificationsPreferencesActivity::class.java)
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

    //endregion
}
