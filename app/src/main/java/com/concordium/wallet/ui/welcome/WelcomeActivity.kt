package com.concordium.wallet.ui.welcome

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityWelcomeBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.auth.setup.AuthSetupPasscodeActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.more.notifications.NotificationsPermissionDialog
import com.concordium.wallet.uicore.handleUrlClicks

class WelcomeActivity :
    BaseActivity(R.layout.activity_welcome),
    WelcomeAccountActivationLauncher {

    private val binding: ActivityWelcomeBinding by lazy {
        ActivityWelcomeBinding.bind(findViewById(R.id.root_layout))
    }

    private val trackingPreferences = App.appCore.appTrackingPreferences

    private val passcodeSetupForCreateLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                gotoAccountOverview()
            }
        }
    private val passcodeSetupForImportLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                goToImportWallet()
            }
        }

    private lateinit var viewModel: WelcomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get()

        initViews()

        viewModel.isNotificationDialogEverShowed.collectWhenStarted(this) {
            if (it.not()) {
                NotificationsPermissionDialog().showSingle(
                    supportFragmentManager,
                    NotificationsPermissionDialog.TAG,
                )
                viewModel.setNotificationDialogShowed()
            }
        }

        // Subscribe to activation bottom sheet chosen action.
        supportFragmentManager.setFragmentResultListener(
            WelcomeActivateAccountBottomSheet.ACTION_REQUEST,
            this
        ) { _, bundle ->
            when (WelcomeActivateAccountBottomSheet.getResult(bundle)) {
                WelcomeActivateAccountBottomSheet.ChosenAction.CREATE ->
                    if (viewModel.shouldSetUpPassword) {
                        // Set up password and create a new seed phrase wallet.
                        passcodeSetupForCreateLauncher
                            .launch(
                                Intent(this, AuthSetupPasscodeActivity::class.java)
                                    .putExtra(
                                        AuthSetupPasscodeActivity.SET_UP_SEED_PHRASE_WALLET_EXTRA,
                                        true
                                    )
                            )
                    } else {
                        gotoAccountOverview()
                    }

                WelcomeActivateAccountBottomSheet.ChosenAction.IMPORT ->
                    if (viewModel.shouldSetUpPassword) {
                        // Set up password and create a import an existing wallet.
                        passcodeSetupForImportLauncher
                            .launch(Intent(this, AuthSetupPasscodeActivity::class.java))
                    } else {
                        goToImportWallet()
                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        App.appCore.tracker.welcomeScreen()
    }

    private fun initViews() {
        binding.termsTextView.handleUrlClicks { url ->
            when (url) {
                "#terms" ->
                    openTerms()

                else -> {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    ContextCompat.startActivity(this, browserIntent, null)
                }
            }
        }

        binding.termsCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                App.appCore.tracker.welcomeTermAndConditionsCheckBoxChecked()
            }
            binding.getStartedButton.isEnabled = isChecked
        }

        binding.activityTrackingCheckBox.isChecked = trackingPreferences.isTrackingEnabled
        binding.activityTrackingCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                App.appCore.tracker.welcomeActivityTrackingCheckBoxChecked()
            }
            trackingPreferences.isTrackingEnabled = isChecked
        }

        binding.getStartedButton.setOnClickListener {
            App.appCore.tracker.welcomeGetStartedClicked()
            proceedWithAccountActivation()
        }
    }

    private fun openTerms() {
        startActivity(Intent(this, WelcomeTermsActivity::class.java))
    }

    override fun loggedOut() {
        // do nothing as we are one of the root activities
    }

    override fun proceedWithAccountActivation() {
        WelcomeActivateAccountBottomSheet()
            .show(supportFragmentManager, WelcomeActivateAccountBottomSheet.TAG)
    }

    private fun goToImportWallet() {
        startActivity(Intent(this, WelcomeRecoverWalletActivity::class.java))
    }

    private fun gotoAccountOverview() {
        finish()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
}
