package com.concordium.wallet.ui

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.databinding.ActivityMainBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsFragment
import com.concordium.wallet.ui.account.accountdetails.transfers.AccountDetailsTransfersFragment
import com.concordium.wallet.ui.account.accountslist.AccountsListActivity
import com.concordium.wallet.ui.account.earn.EarnFragment
import com.concordium.wallet.ui.auth.login.AuthLoginActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.cis2.TransferFragment
import com.concordium.wallet.ui.common.delegates.AuthDelegate
import com.concordium.wallet.ui.common.delegates.AuthDelegateImpl
import com.concordium.wallet.ui.common.delegates.IdentityStatusDelegate
import com.concordium.wallet.ui.common.delegates.IdentityStatusDelegateImpl
import com.concordium.wallet.ui.more.import.ImportActivity
import com.concordium.wallet.ui.more.moreoverview.MenuSettingsFragment
import com.concordium.wallet.ui.multiwallet.WalletSwitchViewModel
import com.concordium.wallet.ui.onboarding.OnboardingSharedViewModel
import com.concordium.wallet.ui.onramp.CcdOnrampSitesFragment
import com.concordium.wallet.ui.walletconnect.WalletConnectView
import com.concordium.wallet.ui.walletconnect.WalletConnectViewModel
import com.concordium.wallet.ui.welcome.WelcomeActivity
import com.concordium.wallet.ui.welcome.WelcomeRecoverWalletActivity
import com.concordium.wallet.util.ImageUtil
import com.concordium.wallet.util.getOptionalSerializable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlin.math.abs

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
        const val DRAWER_TAG = "DRAWER_TAG"
    }

    private val binding by lazy {
        ActivityMainBinding.bind(findViewById(R.id.root_layout))
    }
    private lateinit var viewModel: MainViewModel
    private lateinit var onboardingViewModel: OnboardingSharedViewModel
    private lateinit var walletConnectViewModel: WalletConnectViewModel
    private lateinit var walletSwitchViewModel: WalletSwitchViewModel
    private var hasHandledPossibleImportFile = false
    private lateinit var gestureDetector: GestureDetectorCompat

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set theme to default to remove launcher theme.
        setTheme(R.style.MW24_MainScreen)

        super.onCreate(savedInstanceState)

        initializeViewModel()
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
        initGestureDetector()
        initObservers()
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
            replaceFragment(state)
        }
        viewModel.activeAccount.collectWhenStarted(this) { account ->
            account?.let {
                hideAccountSelector(
                    isVisible = true,
                    text = it.getAccountName(),
                    icon = ImageUtil.getIconById(this, it.iconId)
                ) {
                    gotoAccountsList()
                }
            }
        }

        viewModel.launchEarn.collectWhenStarted(this) {
            binding.bottomNavigationView.selectedItemId = R.id.menuitem_earn
        }

        viewModel.launchOnRamp.collectWhenStarted(this) {
            binding.bottomNavigationView.selectedItemId = R.id.menuitem_buy
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

    @Suppress("DEPRECATION")
    private fun initializeViews() {
        window.navigationBarColor =
            ContextCompat.getColor(this, R.color.mw24_black_60)

        binding.bottomNavigationView.apply {
            itemIconTintList = null
            setOnItemSelectedListener(::onNavigationItemSelected)
        }

        hideActionBarBack(false)

        hideMenuDrawer(isVisible = true) {
            showDrawer()
        }

        WalletConnectView(
            activity = this,
            fragmentManager = supportFragmentManager,
            authDelegate = this,
            viewModel = walletConnectViewModel,
        ).init()

        binding.walletSwitchView.bind(walletSwitchViewModel)
    }

    private fun initGestureDetector() {
        gestureDetector = GestureDetectorCompat(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                private val SWIPE_THRESHOLD = 100
                private val SWIPE_VELOCITY_THRESHOLD = 200

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false

                    val deltaX = e2.x - e1.x
                    val deltaY = e2.y - e1.y

                    if (abs(deltaX) > abs(deltaY)) {
                        if (deltaX > SWIPE_THRESHOLD && velocityX > SWIPE_VELOCITY_THRESHOLD) {
                            // Swipe right to open drawer
                            showDrawer()
                            return true
                        } else if (deltaX < -SWIPE_THRESHOLD && velocityX < -SWIPE_VELOCITY_THRESHOLD) {
                            // Swipe left to close drawer
                            hideDrawer()
                            return true
                        }
                    }
                    return false
                }
            }
        )
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }

    private fun initObservers() {
        supportFragmentManager.setFragmentResultListener(
            MenuSettingsFragment.CLOSE_ACTION,
            this
        ) { _, bundle ->
            if (MenuSettingsFragment.getResult(bundle))
                hideDrawer()
        }

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
            R.id.menuitem_accounts -> MainViewModel.State.Home
            R.id.menuitem_transfer -> MainViewModel.State.Transfer
            R.id.menuitem_buy -> MainViewModel.State.Buy
            R.id.menuitem_earn -> MainViewModel.State.Earn
            R.id.menuitem_activity -> MainViewModel.State.Activity
            else -> null
        }
    }

    private fun replaceFragment(state: MainViewModel.State) {
        val existingFragment = supportFragmentManager.findFragmentByTag(state.name)

        val fragment = existingFragment ?: when (state) {
            MainViewModel.State.Home -> AccountDetailsFragment()
            MainViewModel.State.Transfer -> TransferFragment()
            MainViewModel.State.Buy -> CcdOnrampSitesFragment()
            MainViewModel.State.Activity -> AccountDetailsTransfersFragment()
            MainViewModel.State.Earn -> EarnFragment()
        }

        if (existingFragment == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, state.name)
                .commit()
        }
    }

    private fun showDrawer() {
        val fragment = supportFragmentManager.findFragmentByTag(DRAWER_TAG)
            ?: MenuSettingsFragment()

        if (fragment.isAdded.not()) {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_left
                )
                .replace(R.id.drawer_container, fragment, DRAWER_TAG)
                .commit()
        }

        binding.drawerContainer.apply {
            visibility = View.VISIBLE
            isClickable = true
        }
    }

    private fun hideDrawer() {
        val fragment = supportFragmentManager.findFragmentByTag(DRAWER_TAG)
        if (fragment != null) {
            val anim = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)
            val animDuration = anim.duration

            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    0,
                    R.anim.slide_out_left
                )
                .remove(fragment)
                .commit()

            binding.drawerContainer.postDelayed({
                binding.drawerContainer.apply {
                    visibility = View.GONE
                    isClickable = false
                }
            }, animDuration)
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
        intent.putExtras(
            WelcomeRecoverWalletActivity.getBundle(
                showFileOptions = false,
            )
        )
        startActivity(intent)
    }

    private fun gotoAccountsList() {
        val intent = Intent(this, AccountsListActivity::class.java)
        startActivity(intent)
    }
}
