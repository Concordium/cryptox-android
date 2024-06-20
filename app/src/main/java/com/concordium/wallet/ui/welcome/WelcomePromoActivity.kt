package com.concordium.wallet.ui.welcome

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityWelcomePromoBinding
import com.concordium.wallet.ui.auth.passcode.PasscodeSetupActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.more.notifications.NotificationsPermissionDialog
import com.concordium.wallet.ui.more.tracking.TrackingPermissionDialog
import com.concordium.wallet.ui.passphrase.setup.OneStepSetupWalletActivity

class WelcomePromoActivity :
    BaseActivity(R.layout.activity_welcome_promo),
    WelcomeAccountActivationLauncher {

    private val passcodeSetupForCreateLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                goToCreateWallet()
            }
        }
    private val passcodeSetupForImportLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                goToImportWallet()
            }
        }

    private val binding: ActivityWelcomePromoBinding by lazy {
        ActivityWelcomePromoBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var viewModel: WelcomePromoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()
        viewModel.initialize()

        if (savedInstanceState == null) {
            showAccounts()

            if (viewModel.shouldShowTrackingPermissionDialog) {
                TrackingPermissionDialog()
                    .show(supportFragmentManager, TrackingPermissionDialog.TAG)
            } else if (viewModel.shouldShowNotificationPermissionDialog) {
                NotificationsPermissionDialog()
                    .show(supportFragmentManager, NotificationsPermissionDialog.TAG)
            }

            App.appCore.tracker.welcomeHomeScreen()
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menuitem_accounts -> {
                    showAccounts()
                    true
                }

                R.id.menuitem_more -> {
                    showMore()
                    true
                }

                else -> {
                    proceedWithAccountActivation()
                    false
                }
            }
        }

        // Subscribe to activation bottom sheet chosen action.
        supportFragmentManager.setFragmentResultListener(
            WelcomePromoActivateAccountBottomSheet.ACTION_REQUEST,
            this
        ) { _, bundle ->
            when (WelcomePromoActivateAccountBottomSheet.getResult(bundle)) {
                WelcomePromoActivateAccountBottomSheet.ChosenAction.CREATE ->
                    if (viewModel.shouldSetUpPassword) {
                        // Set up password and create a new wallet.
                        passcodeSetupForCreateLauncher
                            .launch(Intent(this, PasscodeSetupActivity::class.java))
                    } else {
                        goToCreateWallet()
                    }

                WelcomePromoActivateAccountBottomSheet.ChosenAction.IMPORT ->
                    if (viewModel.shouldSetUpPassword) {
                        // Set up password and create a import an existing wallet.
                        passcodeSetupForImportLauncher
                            .launch(Intent(this, PasscodeSetupActivity::class.java))
                    } else {
                        goToImportWallet()
                    }
            }
        }

        // Subscribe to notification permission dialog result
    }

    override fun proceedWithAccountActivation() {
        WelcomePromoActivateAccountBottomSheet()
            .show(supportFragmentManager, WelcomePromoActivateAccountBottomSheet.TAG)
    }

    private fun goToCreateWallet() {
        startActivity(Intent(this, OneStepSetupWalletActivity::class.java))
    }

    private fun goToImportWallet() {
        startActivity(Intent(this, WelcomeRecoverWalletActivity::class.java))
    }

    private fun showAccounts() =
        supportFragmentManager.commit {
            disallowAddToBackStack()
            replace(R.id.fragment_container, WelcomePromoAccountsFragment())
        }

    private fun showMore() = supportFragmentManager.commit {
        disallowAddToBackStack()
        replace(R.id.fragment_container, WelcomePromoMoreFragment())
    }

    override fun loggedOut() {
        // do nothing as we are one of the root activities
    }
}
