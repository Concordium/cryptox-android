package com.concordium.wallet.ui.more.moreoverview

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentMoreOverviewBinding
import com.concordium.wallet.extension.collect
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.identity.identitiesoverview.IdentitiesOverviewActivity
import com.concordium.wallet.ui.more.about.AboutActivity
import com.concordium.wallet.ui.more.alterpassword.AlterPasswordActivity
import com.concordium.wallet.ui.more.dialog.ClearWalletConnectDialog
import com.concordium.wallet.ui.more.dialog.RemoveWalletDialog
import com.concordium.wallet.ui.more.export.ExportActivity
import com.concordium.wallet.ui.more.import.ImportActivity
import com.concordium.wallet.ui.more.notifications.NotificationsPreferencesActivity
import com.concordium.wallet.ui.more.tracking.TrackingPreferencesActivity
import com.concordium.wallet.ui.more.unshielding.UnshieldingAccountsActivity
import com.concordium.wallet.ui.multiwallet.WalletsActivity
import com.concordium.wallet.ui.recipient.recipientlist.RecipientListActivity
import com.concordium.wallet.ui.seed.recoverprocess.RecoverProcessActivity
import com.concordium.wallet.ui.seed.reveal.SavedSeedPhraseRevealActivity
import com.concordium.wallet.ui.seed.reveal.SavedSeedRevealActivity
import com.concordium.wallet.ui.tokens.provider.NFTActivity
import com.concordium.wallet.ui.welcome.WelcomeActivity

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
        savedInstanceState: Bundle?,
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
        initObservers()

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

        binding.closeMenuBtn.setOnClickListener {
            setFragmentResult(
                CLOSE_ACTION,
                setResult()
            )
        }

        binding.unshieldingLayout.setOnClickListener {
            gotoUnshielding()
        }
        viewModel.unshieldingVisibilityLiveData.observe(
            viewLifecycleOwner,
            binding.unshieldingLayout::isVisible::set
        )

        binding.identitiesLayout.setOnClickListener {
            if (mainViewModel.hasCompletedOnboarding())
                gotoIdentitiesOverview()
            else
                (requireActivity() as BaseActivity).showUnlockFeatureDialog()
        }

        binding.walletsLayout.setOnClickListener {
            openWallets()
        }

        binding.addressBookLayout.setOnClickListener {
            gotoAddressBook()
        }

        binding.notificationsLayout.setOnClickListener {
            openNotificationsPreferences()
        }

        binding.exportLayout.setOnClickListener {
            if (mainViewModel.hasCompletedOnboarding())
                gotoExport()
            else
                (requireActivity() as BaseActivity).showUnlockFeatureDialog()
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
            if (mainViewModel.hasCompletedOnboarding())
                revealSeedPhrase()
            else
                (requireActivity() as BaseActivity).showUnlockFeatureDialog()
        }
        viewModel.seedPhraseRevealVisibilityLiveData.observe(
            viewLifecycleOwner,
            binding.seedPhraseRevealLayout::isVisible::set
        )

        binding.seedRevealLayout.setOnClickListener {
            if (mainViewModel.hasCompletedOnboarding())
                revealSeed()
            else
                (requireActivity() as BaseActivity).showUnlockFeatureDialog()
        }
        viewModel.seedRevealVisibilityLiveData.observe(
            viewLifecycleOwner,
            binding.seedRevealLayout::isVisible::set
        )

        binding.trackingLayout.setOnClickListener {
            openTrackingPreferences()
        }

        binding.nftLayout.setOnClickListener {
            if (mainViewModel.hasCompletedOnboarding())
                gotoNFT()
            else
                (requireActivity() as BaseActivity).showUnlockFeatureDialog()
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

        binding.eraseDataLayout.setOnClickListener {
            eraseDataAndExit()
        }
    }

    private fun subscribeToEvents() = viewModel.eventsFlow.collect(this) { event ->
        when (event) {
            MoreOverviewViewModel.Event.ShowAuthentication ->
                authDelegate.showAuthentication(
                    activity = activity,
                    onAuthenticated = {
                        viewModel.onAuthenticated()
                    }
                )

            MoreOverviewViewModel.Event.ShowDataErasedMessage ->
                Toast.makeText(
                    requireContext(),
                    R.string.settings_overview_erase_data_erased,
                    Toast.LENGTH_SHORT
                )
                    .show()

            MoreOverviewViewModel.Event.GoToWelcome -> {
                requireActivity().finishAffinity()
                startActivity(Intent(requireContext(), WelcomeActivity::class.java))
            }
        }
    }

    private fun initObservers() {
        parentFragmentManager.setFragmentResultListener(
            ClearWalletConnectDialog.ACTION_REQUEST,
            this
        ) { _, bundle ->
            val isClearWalletConnect = ClearWalletConnectDialog.getResult(bundle)
            if (isClearWalletConnect) {
                viewModel.deleteWCDatabaseAndExit()

                Toast.makeText(
                    requireContext(),
                    R.string.wallet_connect_database_cleared,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        parentFragmentManager.setFragmentResultListener(
            RemoveWalletDialog.ACTION_REQUEST,
            this
        ) { _, bundle ->
            if (RemoveWalletDialog.getResult(bundle)) {
                viewModel.onEraseContinueClicked()
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
        ClearWalletConnectDialog().showSingle(
            parentFragmentManager,
            ClearWalletConnectDialog.TAG
        )
    }

    private fun eraseDataAndExit() {
        RemoveWalletDialog().showSingle(
            parentFragmentManager,
            RemoveWalletDialog.TAG
        )
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

    private fun gotoNFT() {
        val intent = Intent(activity, NFTActivity::class.java)
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
        val intent = Intent(activity, SavedSeedPhraseRevealActivity::class.java)
        startActivity(intent)
    }

    private fun revealSeed() {
        val intent = Intent(activity, SavedSeedRevealActivity::class.java)
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

    private fun openWallets() {
        val intent = Intent(activity, WalletsActivity::class.java)
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

    companion object {
        const val CLOSE_ACTION = "close_action"
        private const val CLOSE_MENU = "close_menu"

        fun getResult(bundle: Bundle): Boolean = bundle.getBoolean(CLOSE_MENU, false)

        private fun setResult() = Bundle().apply {
            putBoolean(CLOSE_MENU, true)
        }
    }
}
