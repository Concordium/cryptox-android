package com.concordium.wallet.ui.account.accountsoverview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.animation.AccelerateInterpolator
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.preferences.Preferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.FragmentAccountsOverviewBinding
import com.concordium.wallet.databinding.FragmentOnboardingBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.account.newaccountname.NewAccountNameActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.cis2.SendTokenActivity
import com.concordium.wallet.ui.more.export.ExportActivity
import com.concordium.wallet.ui.multiwallet.FileWalletCreationLimitationDialog
import com.concordium.wallet.ui.multiwallet.WalletsActivity
import com.concordium.wallet.ui.onboarding.OnboardingFragment
import com.concordium.wallet.ui.onboarding.OnboardingSharedViewModel
import com.concordium.wallet.ui.onboarding.OnboardingState
import com.concordium.wallet.ui.onramp.CcdOnrampSitesActivity
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigInteger

class AccountsOverviewFragment : BaseFragment() {

    companion object {
        private const val REQUEST_CODE_ACCOUNT_DETAILS = 2000
    }

    private var eventListener: Preferences.Listener? = null
    private lateinit var binding: FragmentAccountsOverviewBinding
    private lateinit var onboardingBinding: FragmentOnboardingBinding
    private lateinit var viewModel: AccountsOverviewViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var onboardingViewModel: OnboardingSharedViewModel
    private lateinit var onboardingStatusCard: OnboardingFragment

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAccountsOverviewBinding.inflate(inflater, container, false)
        onboardingBinding = FragmentOnboardingBinding.inflate(layoutInflater)
        onboardingStatusCard = binding.onboardingLayout

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
        initializeViews()

        val baseActivity = (activity as BaseActivity)

        baseActivity.hideLeftPlus(
            isVisible = true,
            hasNotice = viewModel.isCreationLimitedForFileWallet,
        ) {
            if (viewModel.isCreationLimitedForFileWallet) {
                showFileWalletCreationLimitation()
            } else if (mainViewModel.hasCompletedOnboarding()) {
                gotoCreateAccount()
            } else {
                baseActivity.showUnlockFeatureDialog()
            }
        }

        baseActivity.hideQrScan(isVisible = true) {
            if (mainViewModel.hasCompletedOnboarding()) {
                baseActivity.startQrScanner()
            } else {
                baseActivity.showUnlockFeatureDialog()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateState()
        viewModel.initiateUpdater()
    }

    override fun onDestroy() {
        super.onDestroy()
        eventListener?.let {
            App.appCore.session.removeAccountsBackedUpListener(it)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopUpdater()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_ACCOUNT_DETAILS) {
            if (resultCode == AccountDetailsActivity.RESULT_RETRY_ACCOUNT_CREATION) {
                gotoCreateAccount()
            }
        }
    }
    //endregion

    //region Initialize
    // ************************************************************

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[AccountsOverviewViewModel::class.java]
        mainViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[MainViewModel::class.java]

        onboardingViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[OnboardingSharedViewModel::class.java]

        viewModel.waitingLiveData.observe(viewLifecycleOwner) { waiting ->
            waiting?.let {
                Log.d("waiting:$waiting")
                showWaiting(waiting)
            }
        }
        viewModel.errorLiveData.observe(viewLifecycleOwner, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
        viewModel.totalBalanceLiveData.observe(viewLifecycleOwner) { totalBalance ->
            showTotalBalance(totalBalance.totalBalanceForAllAccounts)
            showDisposalBalance(totalBalance.totalAtDisposalForAllAccounts)
        }
        viewModel.showDialogLiveData.observe(viewLifecycleOwner) { event ->
            if (mainViewModel.hasCompletedOnboarding()) {
                when (event.contentIfNotHandled) {
                    AccountsOverviewViewModel.DialogToShow.UNSHIELDING -> {
                        UnshieldingNoticeDialog().showSingle(
                            childFragmentManager,
                            UnshieldingNoticeDialog.TAG
                        )
                    }

                    null -> {}
                }
            }
        }
        viewModel.stateFlow.collectWhenStarted(viewLifecycleOwner) { state ->
            when (state) {
                OnboardingState.DONE -> {
                    showStateDefault()
                }

                else -> {
                    onboardingStatusCard.updateViewsByState(state)
                    showStateOnboarding()
                }
            }
        }
        viewModel.onrampActive.collectWhenStarted(viewLifecycleOwner) { active ->
            if (active) {
                if (!viewModel.hasShownInitialAnimation()) {
                    binding.confettiAnimation.visibility = View.VISIBLE
                    binding.confettiAnimation.playAnimation()
                }
            }

            binding.onrampBanner.root.setOnClickListener {
                App.appCore.tracker.homeOnRampBannerClicked()

                if (active) {
                    val intent = Intent(requireContext(), CcdOnrampSitesActivity::class.java)
                    intent.putExtras(
                        CcdOnrampSitesActivity.getBundle(
                            accountAddress = null,
                        )
                    )
                    startActivity(intent)
                } else {
                    (activity as BaseActivity).showUnlockFeatureDialog()
                }
            }
        }

        viewModel.identityFlow.collectWhenStarted(viewLifecycleOwner) { identity ->
            onboardingViewModel.setIdentity(identity)
        }

        viewModel.fileWalletMigrationVisible.collectWhenStarted(
            viewLifecycleOwner,
            binding.fileWalletMigrationDisclaimerLayout::isVisible::set
        )

        onboardingViewModel.identityFlow.collectWhenStarted(viewLifecycleOwner) { identity ->
            onboardingStatusCard.updateViewsByIdentityStatus(identity)
        }
        onboardingViewModel.updateState.collectWhenStarted(viewLifecycleOwner) { update ->
            if (update)
                viewModel.updateState(true)
        }
        onboardingViewModel.showLoading.collectWhenStarted(viewLifecycleOwner) { show ->
            showWaiting(show)
        }
    }

    private fun initializeViews() {
        mainViewModel.setTitle(getString(R.string.accounts_overview_title))
        binding.progress.progressLayout.visibility = View.VISIBLE
        binding.onboardingLayout.visibility = View.GONE

        binding.missingBackup.setOnClickListener {
            gotoExport()
        }

        listOf(binding.totalBalanceLayout, binding.totalDetailsDisposalLayout).forEach {
            it.setOnClickListener {
                App.appCore.tracker.homeTotalBalanceClicked()
            }
        }

        eventListener = object : Preferences.Listener {
            override fun onChange() {
                updateMissingBackup()
            }
        }

        updateMissingBackup()

        eventListener?.let {
            App.appCore.session.addAccountsBackedUpListener(it)
        }

        binding.fileWalletMigrationDisclaimerLayout.setOnClickListener {
            startActivity(Intent(requireActivity(), WalletsActivity::class.java))
        }

        initializeAnimation()
        initializeList()
        updateMissingBackup()
    }

    private fun updateMissingBackup() = viewModel.viewModelScope.launch(Dispatchers.Main) {
        binding.missingBackup.isVisible = App.appCore.session.run {
            isAccountsBackupPossible() && !areAccountsBackedUp()
        }
    }

    private fun initializeList() {
        val adapter = AccountsOverviewItemAdapter(
            accountViewClickListener = object : AccountView.OnItemClickListener {
                override fun onCardClicked(account: Account) {
                    gotoAccountDetails(account)
                }

                override fun onOnrampClicked(account: Account) {
                    gotoCcdOnramp(account)
                }

                override fun onSendClicked(account: Account) {
                    val parentActivity = requireActivity() as BaseActivity
                    val intent = Intent(parentActivity, SendTokenActivity::class.java)
                    intent.putExtra(SendTokenActivity.ACCOUNT, account)
                    intent.putExtra(
                        SendTokenActivity.TOKEN,
                        Token.ccd(account)
                    )
                    intent.putExtra(
                        SendTokenActivity.PARENT_ACTIVITY,
                        parentActivity::class.java.canonicalName
                    )
                    parentActivity.startActivityForResultAndHistoryCheck(intent)
                }

                override fun onAddressClicked(account: Account) {
                    val intent = Intent(requireContext(), AccountQRCodeActivity::class.java)
                    intent.putExtra(AccountQRCodeActivity.EXTRA_ACCOUNT, account)
                    startActivity(intent)
                }
            },
        )
        binding.accountRecyclerview.adapter = adapter
        viewModel.listItemsLiveData.observe(viewLifecycleOwner, adapter::setData)

        // Make the list height match the container height
        // to enable full hide of the header by scrolling.
        var handledContainerHeight = -1
        binding.scrollView.viewTreeObserver.addOnGlobalLayoutListener {
            val containerHeight = binding.scrollView.measuredHeight

            if (handledContainerHeight != containerHeight) {
                handledContainerHeight = containerHeight

                binding.accountRecyclerview.updateLayoutParams<LayoutParams> {
                    height = containerHeight
                }
            }
        }
    }

    private fun initializeAnimation() {
        val handler = Handler(Looper.getMainLooper())
        binding.confettiAnimation.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(p0: Animator) {
                handler.postDelayed({
                    binding.confettiAnimation.animate()
                        .setInterpolator(AccelerateInterpolator())
                        .alpha(0f)
                        .scaleXBy(0.3f)
                        .scaleYBy(0.3f)
                        .setDuration(900)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                cancelAnimation()
                            }
                        })
                }, 800)
            }

            override fun onAnimationEnd(p0: Animator) {
                cancelAnimation()
            }
        })

        binding.confettiAnimation.addAnimatorPauseListener(object : AnimatorListenerAdapter() {
            override fun onAnimationPause(p0: Animator) {
                cancelAnimation()
            }
        })
    }

    //endregion

    //region Control/UI
    // ************************************************************

    private fun gotoExport() {
        val intent = Intent(activity, ExportActivity::class.java)
        startActivity(intent)
    }

    private fun gotoCreateAccount() {
        val intent = Intent(activity, NewAccountNameActivity::class.java)
        startActivity(intent)
    }

    private fun gotoAccountDetails(item: Account) {
        val intent = Intent(activity, AccountDetailsActivity::class.java)
        intent.putExtra(AccountDetailsActivity.EXTRA_ACCOUNT, item)
        startActivityForResult(intent, REQUEST_CODE_ACCOUNT_DETAILS)
    }

    private fun gotoCcdOnramp(item: Account) {
        val intent = Intent(activity, CcdOnrampSitesActivity::class.java)
        intent.putExtras(
            CcdOnrampSitesActivity.getBundle(
                accountAddress = item.address,
            )
        )
        startActivity(intent)
    }

    private fun showFileWalletCreationLimitation() {
        FileWalletCreationLimitationDialog().showSingle(
            childFragmentManager,
            FileWalletCreationLimitationDialog.TAG
        )
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    private fun showError(stringRes: Int) {
        popup.showSnackbar(binding.root, stringRes)
    }

    private fun showStateOnboarding() {
        activity?.invalidateOptionsMenu()
        binding.onboardingLayout.visibility = View.VISIBLE
        binding.accountRecyclerview.visibility = View.GONE
        showOnboarding(true)
    }

    private fun showStateDefault() {
        activity?.invalidateOptionsMenu()
        binding.onboardingLayout.visibility = View.GONE
        binding.accountRecyclerview.visibility = View.VISIBLE
        showOnboarding(false)
    }

    private fun showOnboarding(show: Boolean) {
        val state = if (show) View.VISIBLE else View.GONE
        binding.onboardingLayout.visibility = state
    }

    private fun showTotalBalance(totalBalance: BigInteger) {
        binding.totalBalanceTextview.text =
            CurrencyUtil.formatAndRoundGTU(
                value = totalBalance,
                roundDecimals = 2
            )
        updateTotalBalanceView()
    }

    private fun showDisposalBalance(atDisposal: BigInteger) {
        binding.accountsOverviewTotalDetailsDisposal.text =
            CurrencyUtil.formatAndRoundGTU(
                value = atDisposal,
                roundDecimals = 2
            )
        updateBalanceAtDisposalView()
    }

    private fun cancelAnimation() {
        viewModel.setHasShownInitialAnimation()
        binding.confettiAnimation.visibility = View.GONE
    }

    // update balance TextView directly if balance value is too long
    private fun updateTotalBalanceView() {
        val availableWidth = binding.totalBalanceLayout.width - binding.totalBalanceSuffix.width

        if (binding.totalBalanceTextview.width > availableWidth) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(binding.totalBalanceLayout)
            constraintSet.apply {
                connect(
                    binding.totalBalanceTextview.id,
                    ConstraintSet.END,
                    binding.totalBalanceSuffix.id,
                    ConstraintSet.START
                )
                connect(
                    binding.totalBalanceSuffix.id,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END
                )
                constrainWidth(binding.totalBalanceTextview.id, ConstraintSet.MATCH_CONSTRAINT)
                setHorizontalChainStyle(
                    binding.totalBalanceLayout.id,
                    ConstraintSet.CHAIN_SPREAD_INSIDE
                )
                setMargin(binding.totalBalanceSuffix.id, ConstraintSet.BOTTOM, 0)
            }
            constraintSet.applyTo(binding.totalBalanceLayout)
        }
    }

    // update disposable balance TextView directly if balance value is too long
    private fun updateBalanceAtDisposalView() {
        val availableWidth =
            binding.totalDetailsDisposalLayout.width - binding.accountsOverviewTotalDetailsDisposalSuffix.width

        if (binding.accountsOverviewTotalDetailsDisposal.width > availableWidth) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(binding.totalDetailsDisposalLayout)
            constraintSet.apply {
                connect(
                    binding.accountsOverviewTotalDetailsDisposal.id,
                    ConstraintSet.END,
                    binding.accountsOverviewTotalDetailsDisposalSuffix.id,
                    ConstraintSet.START
                )
                connect(
                    binding.accountsOverviewTotalDetailsDisposalSuffix.id,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END
                )
                constrainWidth(
                    binding.accountsOverviewTotalDetailsDisposal.id,
                    ConstraintSet.MATCH_CONSTRAINT
                )
                setHorizontalChainStyle(
                    binding.totalDetailsDisposalLayout.id,
                    ConstraintSet.CHAIN_SPREAD_INSIDE
                )
            }
            constraintSet.applyTo(binding.totalDetailsDisposalLayout)
        }
    }

    //endregion
}
