package com.concordium.wallet.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.App
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.ActivityMainBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsFragment
import com.concordium.wallet.ui.auth.login.AuthLoginActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.common.delegates.IdentityStatusDelegate
import com.concordium.wallet.ui.common.delegates.IdentityStatusDelegateImpl
import com.concordium.wallet.ui.more.import.ImportActivity
import com.concordium.wallet.ui.more.moreoverview.MoreOverviewFragment
import com.concordium.wallet.ui.multiwallet.WalletSwitchViewModel
import com.concordium.wallet.ui.news.NewsOverviewFragment
import com.concordium.wallet.ui.onboarding.OnboardingSharedViewModel
import com.concordium.wallet.ui.walletconnect.WalletConnectView
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel
import com.concordium.wallet.ui.welcome.WelcomeActivity
import com.concordium.wallet.ui.welcome.WelcomeRecoverWalletActivity
import com.concordium.wallet.util.getOptionalSerializable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : BaseActivity(R.layout.activity_main, R.string.accounts_overview_title),
    AuthDelegate by AuthDelegateImpl(),
    IdentityStatusDelegate by IdentityStatusDelegateImpl() {

    companion object {
        const val EXTRA_IMPORT_FROM_FILE = "EXTRA_IMPORT_FROM_FILE"
        const val EXTRA_IMPORT_FROM_SEED = "EXTRA_IMPORT_FROM_SEED"
        const val EXTRA_WALLET_CONNECT_URI = "wc_uri"
        const val EXTRA_ACTIVATE_ACCOUNT = "EXTRA_ACTIVATE_ACCOUNT"
        const val EXTRA_ACCOUNT_ADDRESS = "EXTRA_ACCOUNT_ADDRESS"
        const val EXTRA_NOTIFICATION_TOKEN = "EXTRA_NOTIFICATION_TOKEN"
        const val EXTRA_SHOW_REVIEW_POPUP = "EXTRA_SHOW_REVIEW_POPUP"
    }

    private val binding by lazy {
        ActivityMainBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var viewModel: MainViewModel
    private lateinit var onboardingViewModel: OnboardingSharedViewModel
    private lateinit var walletConnectViewModel: WalletConnectViewModel
    private lateinit var walletSwitchViewModel: WalletSwitchViewModel
    private var hasHandledPossibleImportFile = false

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set theme to default to remove launcher theme.
        setTheme(R.style.MW24_MainScreen)

        super.onCreate(savedInstanceState)

        // Make the navigation bar color match the bottom navigation bar.
        @Suppress("DEPRECATION")
        window.navigationBarColor =
            ContextCompat.getColor(this, R.color.cryptox_black_main)

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
        handleReviewPopup(intent)

        if (intent.getBooleanExtra(EXTRA_IMPORT_FROM_FILE, false)) {
            goToImportFromFile()
        } else if (intent.getBooleanExtra(EXTRA_IMPORT_FROM_SEED, false)) {
            goToImportFromSeed()
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
            if (viewModel.shouldShowPasswordSetup() || viewModel.shouldShowInitialSetup()) {
                finishAffinity()
                startActivity(Intent(this, WelcomeActivity::class.java))
            } else if (viewModel.shouldShowAuthentication()) {
                val intent = Intent(this, AuthLoginActivity::class.java)
                startActivity(intent)
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
                if (!viewModel.hasCompletedOnboarding()) {
                    startCheckForPendingIdentity(this, null, true) { identity ->
                        lifecycleScope.launch {
                            onboardingViewModel.setIdentity(identity)
                        }
                    }
                } else {
                    startCheckForPendingIdentity(this, null, false) {}
                }

                App.appCore.tracker.homeScreen()
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
        handleNotificationReceived(newIntent)
        handlePossibleWalletConnectUri(newIntent)
        handleReviewPopup(newIntent)
    }

    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        val viewModelProvider = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )

        viewModel = viewModelProvider.get()
        viewModel.titleLiveData.observe(this, this::setActionBarTitle)
        viewModel.stateLiveData.observe(this) { state ->
            checkNotNull(state)

            hideLeftPlus(false)
            hideRightPlus(false)
            hideQrScan(false)
            hideAccountSelector(false, "", null)
            replaceFragment(state)
        }

        walletConnectViewModel = viewModelProvider.get()
        onboardingViewModel = viewModelProvider.get()

        walletSwitchViewModel = viewModelProvider.get()
        walletSwitchViewModel.switchesFlow.collectWhenStarted(this) {
            // Force restart the activity with recreation of view models.
            finishAffinity()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun initializeViews() {
        binding.bottomNavigationView.menu.findItem(R.id.menuitem_news).isVisible =
            BuildConfig.SHOW_NEWSFEED
        binding.bottomNavigationView.setOnItemSelectedListener {
            onNavigationItemSelected(it)
        }
        hideActionBarBack(false)

        WalletConnectView(
            activity = this,
            fragmentManager = supportFragmentManager,
            authDelegate = this,
            viewModel = walletConnectViewModel,
        ).init()

        binding.walletSwitchView.bind(walletSwitchViewModel)
    }

    private fun handleNotificationReceived(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_ACTIVATE_ACCOUNT, false)) {
            val address = intent.getStringExtra(EXTRA_ACCOUNT_ADDRESS)
            val token = intent.getOptionalSerializable(EXTRA_NOTIFICATION_TOKEN, Token::class.java)
            address?.let {
                viewModel.activateAccount(it)
                token?.let {
                    viewModel.setNotificationData(address, token)
                }
            }
        }
    }

    private fun handleReviewPopup(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_SHOW_REVIEW_POPUP, false)) {
            viewModel.showReviewDialog()
        }
    }

    //endregion

    //region Menu Navigation
    // ************************************************************

    private fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        menuItem.isChecked = true

        val state = getState(menuItem) ?: return false

        if (viewModel.stateLiveData.value == state) return false
        viewModel.setState(state)
        return true
    }

    private fun getState(menuItem: MenuItem): MainViewModel.State? {
        return when (menuItem.itemId) {
            R.id.menuitem_accounts -> MainViewModel.State.AccountOverview
            R.id.menuitem_news -> MainViewModel.State.NewsOverview
            R.id.menuitem_more -> MainViewModel.State.More
            else -> null
        }
    }

    private fun replaceFragment(state: MainViewModel.State) {
        val existingFragment = supportFragmentManager.findFragmentByTag(state.name)

        val fragment = existingFragment ?: when (state) {
            MainViewModel.State.AccountOverview -> AccountDetailsFragment()
            MainViewModel.State.NewsOverview -> NewsOverviewFragment()
            MainViewModel.State.More -> MoreOverviewFragment()
        }

        if (existingFragment == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, state.name)
                .commit()
        }
    }

    //endregion

    //region Control/UI
    // ************************************************************

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

    private fun goToImportFromFile() {
        val intent = Intent(this, ImportActivity::class.java)
        startActivity(intent)
    }

    private fun goToImportFromSeed() {
        val intent = Intent(this, WelcomeRecoverWalletActivity::class.java)
        intent.putExtras(WelcomeRecoverWalletActivity.getBundle(
            showFileOptions = false,
        ))
        startActivity(intent)
    }
    //endregion
}
