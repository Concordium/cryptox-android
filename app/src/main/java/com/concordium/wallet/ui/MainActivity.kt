package com.concordium.wallet.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.databinding.ActivityMainBinding
import com.concordium.wallet.ui.account.accountsoverview.AccountsOverviewFragment
import com.concordium.wallet.ui.auth.login.AuthLoginActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.common.identity.IdentityErrorDialogHelper
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity
import com.concordium.wallet.ui.more.import.ImportActivity
import com.concordium.wallet.ui.more.moreoverview.MoreOverviewFragment
import com.concordium.wallet.ui.tokens.provider.ProvidersOverviewFragment
import com.concordium.wallet.ui.walletconnect.WalletConnectView
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel
import com.concordium.wallet.ui.welcome.WelcomeActivity
import com.concordium.wallet.ui.welcome.WelcomePromoActivity
import com.concordium.wallet.uicore.dialog.CustomDialogFragment
import com.concordium.wallet.uicore.dialog.Dialogs
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : BaseActivity(R.layout.activity_main, R.string.accounts_overview_title),
    Dialogs.DialogFragmentListener, AuthDelegate by AuthDelegateImpl() {

    companion object {
        const val EXTRA_CREATE_FIRST_IDENTITY = "EXTRA_CREATE_FIRST_IDENTITY"
        const val EXTRA_WALLET_CONNECT_URI = "wc_uri"
    }

    private val binding by lazy {
        ActivityMainBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var viewModel: MainViewModel
    private lateinit var walletConnectViewModel: WalletConnectViewModel
    private var hasHandledPossibleImportFile = false

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set theme to default to remove launcher theme.
        setTheme(R.style.CCX_Screen)

        super.onCreate(savedInstanceState)

        // Make the navigation bar color match the bottom navigation bar.
        window.navigationBarColor =
            ContextCompat.getColor(this, R.color.bottom_navigation_bar_background)

        initializeViewModel()
        viewModel.initialize()
        walletConnectViewModel.initialize()

        initializeViews()

        // If we're being restored from a previous state,
        // then we don't want to add fragments and should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return
        }

        handlePossibleWalletConnectUri(intent)

        if (intent.getBooleanExtra(EXTRA_CREATE_FIRST_IDENTITY, false)) {
            goToFirstIdentityCreation()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!viewModel.databaseVersionAllowed) {
            val builder = MaterialAlertDialogBuilder(this)
            builder.setMessage(getString(R.string.error_database))
            builder.setPositiveButton(
                getString(R.string.error_database_close)
            ) { _, _ -> finish() }
            builder.setCancelable(false)
            builder.create().show()
        } else {
            if (viewModel.shouldShowAuthentication()) {
                showAuthenticationOrContinueSetup()
            } else {
                viewModel.setInitialStateIfNotSet()

                if (!hasHandledPossibleImportFile) {
                    hasHandledPossibleImportFile = true
                    val handlingImportFile = handlePossibleImportFile()
                    if (handlingImportFile) {
                        // Do not start identity update, since we are leaving this page
                        return
                    }
                }

                viewModel.startIdentityUpdate()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopIdentityUpdate()
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)

        // MainActivity has launchMode singleTask to not start a new instance (if already running),
        // when selecting an import file to launch the app. In this case onNewIntent will be called.
        newIntent.data?.let {
            // Save this new intent to handle it in onResume (in case of an import file)
            this.intent = newIntent
            hasHandledPossibleImportFile = false
        }

        handlePossibleWalletConnectUri(newIntent)
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == RequestCodes.REQUEST_IDENTITY_ERROR_DIALOG) {
            if (resultCode == Dialogs.POSITIVE) {
                gotoIdentityProviderList()
            }
        }
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]

        viewModel.titleLiveData.observe(this, this::setActionBarTitle)
        viewModel.stateLiveData.observe(this) { state ->
            checkNotNull(state)

            hideLeftPlus(false)
            hideRightPlus(false)
            hideQrScan(false)
            replaceFragment(state)
        }

        viewModel.identityErrorLiveData.observe(this) { data ->
            data?.let {
                IdentityErrorDialogHelper.showIdentityError(this, dialogs, data)
            }
        }

        viewModel.newFinalizedAccountLiveData.observe(this) {
            CustomDialogFragment.newAccountFinalizedDialog(this)
        }

        walletConnectViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[WalletConnectViewModel::class.java]
    }

    private fun initializeViews() {
        binding.bottomNavigationView.setOnItemSelectedListener {
            onNavigationItemSelected(it)
        }
        hideActionBarBack(false)
        binding.toolbarLayout.toolbarTitle.setTextAppearance(R.style.CCX_Typography_PageTitle)

        WalletConnectView(
            activity = this,
            fragmentManager = supportFragmentManager,
            authDelegate = this,
            viewModel = walletConnectViewModel,
        ).init()
    }

    //endregion

    //region Menu Navigation
    // ************************************************************

    private fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        menuItem.isChecked = true

        val state = getState(menuItem)
        if (state != null) {
            viewModel.setState(state)
            return true
        }
        return false
    }

    private fun getState(menuItem: MenuItem): MainViewModel.State? {
        return when (menuItem.itemId) {
            R.id.menuitem_accounts -> MainViewModel.State.AccountOverview
            R.id.menuitem_more -> MainViewModel.State.More
            R.id.menuitem_tokens -> MainViewModel.State.TokensOverview
            else -> null
        }
    }

    private fun replaceFragment(state: MainViewModel.State) {
        val fragment = when (state) {
            MainViewModel.State.AccountOverview -> AccountsOverviewFragment()
            MainViewModel.State.More -> MoreOverviewFragment()
            MainViewModel.State.TokensOverview -> ProvidersOverviewFragment()
        }
        replaceFragment(fragment)
    }

    private fun replaceFragment(fragment: Fragment?) {
        if (fragment == null) return
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun showAuthenticationOrContinueSetup() {
        if (viewModel.shouldShowPasswordSetup()) {
            finishAffinity()
            startActivity(Intent(this, WelcomeActivity::class.java))
        } else if (viewModel.shouldShowInitialSetup()) {
            finishAffinity()
            startActivity(Intent(this, WelcomePromoActivity::class.java))
        } else {
            val intent = Intent(this, AuthLoginActivity::class.java)
            startActivity(intent)
        }
    }

    override fun loggedOut() {
    }

    private fun handlePossibleImportFile(): Boolean {
        val uri = intent?.data
        if (uri != null) {
            if (!viewModel.canAcceptImportFiles) {
                Toast.makeText(
                    this,
                    R.string.import_not_possible_when_using_phrase,
                    Toast.LENGTH_LONG
                ).show()
                return false
            }

            finishAffinity()
            val intent = Intent(this, ImportActivity::class.java)
            intent.putExtra(ImportActivity.EXTRA_FILE_URI, uri)
            intent.putExtra(ImportActivity.EXTRA_GO_TO_ACCOUNTS_OVERVIEW_ON_SUCCESS, true)
            startActivity(intent)
            return true
        }
        return false
    }

    private fun handlePossibleWalletConnectUri(intent: Intent) {
        val walletConnectUri = intent.getStringExtra(EXTRA_WALLET_CONNECT_URI)
            ?.takeIf(String::isNotEmpty)
        if (walletConnectUri != null) {
            walletConnectViewModel.handleWcUri(walletConnectUri)
        }
    }

    private fun gotoIdentityProviderList() {
        viewModel.identityErrorLiveData.value?.let {
            val intent = Intent(this, IdentityProviderListActivity::class.java)
            startActivity(intent)
        }
    }

    private fun goToFirstIdentityCreation() {
        val intent = Intent(this, IdentityProviderListActivity::class.java)
        intent.putExtra(IdentityProviderListActivity.SHOW_FOR_FIRST_IDENTITY, true)
        startActivity(intent)
    }
    //endregion
}
