package com.concordium.wallet.ui.welcome

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.auth.setup.AuthSetupPasscodeActivity
import com.concordium.wallet.ui.base.BaseActivity

class WelcomeActivity : BaseActivity(R.layout.activity_welcome) {

    private val passcodeSetupForCreateLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                goToAccountOverview()
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

        supportFragmentManager.setFragmentResultListener(
            WelcomeGetStartedFragment.ACTION_REQUEST,
            this
        ) { _, bundle ->
            when (WelcomeGetStartedFragment.getChosenAction(bundle)) {
                WelcomeGetStartedFragment.ChosenAction.CREATE ->
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
                        // If the password was created for Import, but now they returned
                        // and want to Create, initial setup must be marked as finished
                        // to avoid getting back again.
                        App.appCore.setup.finishInitialSetup()
                        goToAccountOverview()
                    }

                WelcomeGetStartedFragment.ChosenAction.IMPORT ->
                    if (viewModel.shouldSetUpPassword) {
                        // Set up password and create a import an existing wallet.
                        passcodeSetupForImportLauncher
                            .launch(Intent(this, AuthSetupPasscodeActivity::class.java))
                    } else {
                        goToImportWallet()
                    }
            }
        }

        supportFragmentManager.setFragmentResultListener(
            WelcomeCarouselFragment.GET_STARTED_REQUEST,
            this
        ) { _, _ ->
            supportFragmentManager.commit {
                replace(
                    R.id.fragment_container,
                    WelcomeGetStartedFragment()
                )
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                disallowAddToBackStack()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        App.appCore.tracker.welcomeScreen()
    }

    private fun initViews() {

    }

    override fun loggedOut() {
        // do nothing as we are one of the root activities
    }

    private fun goToImportWallet() {
        startActivity(Intent(this, WelcomeRecoverWalletActivity::class.java))
    }

    private fun goToAccountOverview() {
        finish()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
}
