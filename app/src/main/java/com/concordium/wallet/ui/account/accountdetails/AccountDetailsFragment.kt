package com.concordium.wallet.ui.account.accountdetails

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.rating.ReviewHelper
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.FragmentAccountDetailsBinding
import com.concordium.wallet.databinding.FragmentOnboardingBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.account.accountsoverview.AccountBalanceCardView
import com.concordium.wallet.ui.account.accountsoverview.AccountBalanceViewModel
import com.concordium.wallet.ui.account.accountsoverview.SeedPhraseBackupNoticeDialog
import com.concordium.wallet.ui.account.accountsoverview.UnshieldingNoticeDialog
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.cis2.TokenDetailsActivity
import com.concordium.wallet.ui.more.notifications.NotificationsPermissionDialog
import com.concordium.wallet.ui.multiwallet.WalletsActivity
import com.concordium.wallet.ui.onboarding.OnboardingFragment
import com.concordium.wallet.ui.onboarding.OnboardingSharedViewModel
import com.concordium.wallet.ui.onboarding.OnboardingState
import com.concordium.wallet.ui.seed.reveal.SavedSeedPhraseRevealActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class AccountDetailsFragment : BaseFragment() {

    private lateinit var binding: FragmentAccountDetailsBinding
    private lateinit var onboardingViewModel: OnboardingSharedViewModel
    private lateinit var onboardingStatusCard: OnboardingFragment
    private lateinit var onboardingBinding: FragmentOnboardingBinding
    private lateinit var reviewHelper: ReviewHelper

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(requireActivity())[MainViewModel::class.java]
    }
    private val viewModelAccountDetails: AccountDetailsViewModel by activityViewModel {
        parametersOf(mainViewModel)
    }
    private val accountBalanceViewModel: AccountBalanceViewModel by viewModel {
        parametersOf(viewModelAccountDetails)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAccountDetailsBinding.inflate(inflater, container, false)
        onboardingBinding = FragmentOnboardingBinding.inflate(layoutInflater)
        onboardingStatusCard = binding.onboardingLayout

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModels()
        initViews()
        initToolbar()
    }

    override fun onResume() {
        super.onResume()
        updateWhenResumed()
    }

    override fun onPause() {
        super.onPause()
        resetWhenPaused()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.balanceCard.listener = null
    }

    private fun initToolbar() {
        val baseActivity = (activity as BaseActivity)

        baseActivity.hideQrScan(isVisible = true) {
            if (mainViewModel.hasCompletedOnboarding()) {
                baseActivity.startQrScanner()
            } else {
                baseActivity.showUnlockFeatureDialog()
            }
        }
        baseActivity.hideSettings(isVisible = false)
    }

    private fun initializeViewModels() {
        onboardingViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[OnboardingSharedViewModel::class.java]

        combine(
            viewModelAccountDetails
                .stateFlow,
            viewModelAccountDetails
                .activeAccount
                .map { it?.transactionStatus }
                .distinctUntilChanged(),
            transform = ::Pair,
        ).collectWhenStarted(viewLifecycleOwner) { (onboardingState, accountFinalizationStatus) ->

            if (onboardingState == OnboardingState.DONE) {
                viewModelAccountDetails.initiateFrequentUpdater()
                updateViews(accountFinalizationStatus)

                if (!viewModelAccountDetails.hasShownInitialAnimation()) {
                    binding.confettiAnimation.visibility = View.VISIBLE
                    binding.confettiAnimation.playAnimation()
                    // Do not collect state updates while confetti is flying.
                    delay(binding.confettiAnimation.duration)
                }
            } else {
                onboardingStatusCard.updateViewsByState(onboardingState)
                showStateOnboarding()
            }
        }

        viewModelAccountDetails.identityFlow.collectWhenStarted(viewLifecycleOwner) { identity ->
            onboardingViewModel.setIdentity(identity)
        }

        viewModelAccountDetails.waitingLiveData.observe(viewLifecycleOwner) { waiting ->
            waiting?.let {
                showWaiting(waiting)
            }
        }
        viewModelAccountDetails.errorLiveData.observe(
            viewLifecycleOwner,
            object : EventObserver<Int>() {
                override fun onUnhandledEvent(value: Int) {
                    (requireActivity() as BaseActivity).showError(value)
                }
            })
        viewModelAccountDetails.finishLiveData.observe(
            viewLifecycleOwner,
            object : EventObserver<Boolean>() {
                override fun onUnhandledEvent(value: Boolean) {
                    requireActivity().finish()
                }
            })
        viewModelAccountDetails.goToEarn.collectWhenStarted(viewLifecycleOwner) {
            mainViewModel.onEarnClicked()
        }
        viewModelAccountDetails.totalBalanceLiveData.observe(viewLifecycleOwner) {
            updateBannersVisibility()
        }

        viewModelAccountDetails.fileWalletMigrationVisible.collectWhenStarted(viewLifecycleOwner) {
            binding.fileWalletMigrationDisclaimerLayout.isVisible = it
        }

        viewModelAccountDetails.suspensionNotice.collectWhenStarted(viewLifecycleOwner) { notice ->
            binding.suspensionNotice.isVisible = notice != null

            if (notice == null) {
                return@collectWhenStarted
            }

            binding.suspensionNotice.text = when (notice) {
                is AccountDetailsViewModel.SuspensionNotice.BakerPrimedForSuspension ->
                    getString(R.string.account_details_suspension_notice_primed_for_suspension)

                is AccountDetailsViewModel.SuspensionNotice.BakerSuspended ->
                    getString(R.string.account_details_suspension_notice_baker_suspended)

                is AccountDetailsViewModel.SuspensionNotice.DelegatorsBakerSuspended ->
                    getString(R.string.account_details_suspension_notice_delegation_baker_suspended)
            }
        }

        viewModelAccountDetails.showDialogLiveData.observe(viewLifecycleOwner) { event ->
            if (mainViewModel.hasCompletedOnboarding() && viewModelAccountDetails.hasShownInitialAnimation()) {
                when (event.contentIfNotHandled) {
                    AccountDetailsViewModel.DialogToShow.UNSHIELDING -> {
                        UnshieldingNoticeDialog().showSingle(
                            childFragmentManager,
                            UnshieldingNoticeDialog.TAG
                        )
                    }

                    AccountDetailsViewModel.DialogToShow.NOTIFICATIONS_PERMISSION -> {
                        showNotificationDialog()
                    }

                    AccountDetailsViewModel.DialogToShow.SEED_PHRASE_BACKUP_NOTICE -> {
                        childFragmentManager.setFragmentResultListener(
                            SeedPhraseBackupNoticeDialog.CONFIRMATION_REQUEST,
                            viewLifecycleOwner,
                        ) { _, result ->
                            if (SeedPhraseBackupNoticeDialog.isHidingConfirmed(result)) {
                                updateBannersVisibility()
                            }
                            childFragmentManager.clearFragmentResultListener(
                                SeedPhraseBackupNoticeDialog.CONFIRMATION_REQUEST
                            )
                        }

                        SeedPhraseBackupNoticeDialog().showSingle(
                            childFragmentManager,
                            SeedPhraseBackupNoticeDialog.TAG
                        )
                    }

                    null -> {}
                }
            }
        }

        val reviewDialogObserver = object : EventObserver<Boolean>() {
            override fun onUnhandledEvent(value: Boolean) {
                if (mainViewModel.hasCompletedOnboarding()) {
                    reviewHelper.launchReviewFlow()
                }
            }
        }
        viewModelAccountDetails.showReviewDialog.observe(viewLifecycleOwner, reviewDialogObserver)
        mainViewModel.showReviewDialog.observe(viewLifecycleOwner, reviewDialogObserver)
        viewModelAccountDetails.showOnboardingNotificationDialog
            .collectWhenStarted(viewLifecycleOwner) {
                showNotificationDialog()
            }

        onboardingViewModel.identityFlow.collectWhenStarted(viewLifecycleOwner) { identity ->
            onboardingStatusCard.updateViewsByIdentityStatus(identity)
        }
        onboardingViewModel.updateState.collectWhenStarted(viewLifecycleOwner) { update ->
            if (update)
                viewModelAccountDetails.updateState()
        }
    }

    private fun initViews() {
        showWaiting(true)
        initializeAnimation()
        initSwipeToRefresh()
        initBalanceViews()
        initContainer()
        initBanners()

        binding.accountRetryButton.setOnClickListener {
            (activity as BaseActivity).showAccountsList()
        }
        binding.accountRemoveButton.setOnClickListener {
            viewModelAccountDetails.deleteAccountAndFinish()
        }
        binding.fileWalletMigrationDisclaimerLayout.setOnClickListener {
            startActivity(Intent(requireActivity(), WalletsActivity::class.java))
        }
        binding.suspensionNotice.setOnClickListener {
            viewModelAccountDetails.onSuspensionNoticeClicked()
        }
        reviewHelper = ReviewHelper(requireActivity())
    }

    private fun updateViews(accountFinalizationStatus: TransactionStatus?) {
        showWaiting(false)

        when (accountFinalizationStatus) {
            null,
            TransactionStatus.UNKNOWN,
            TransactionStatus.ABSENT,
                -> setErrorMode()

            TransactionStatus.FINALIZED
                -> setFinalizedMode()

            TransactionStatus.COMMITTED,
            TransactionStatus.RECEIVED,
                -> setPendingMode()
        }
    }

    private fun setFinalizedMode() {
        binding.apply {
            tokensFragmentContainer.visibility = View.VISIBLE
            pendingFragmentContainer.pendingLayout.visibility = View.GONE
            pendingFragmentContainer.errorLayout.visibility = View.GONE
            onboardingLayout.visibility = View.GONE
            accountRetryButton.visibility = View.GONE
            accountRemoveButton.visibility = View.GONE
        }
    }

    private fun showStateOnboarding() {
        binding.apply {
            tokensFragmentContainer.visibility = View.GONE
            pendingFragmentContainer.pendingLayout.visibility = View.GONE
            pendingFragmentContainer.errorLayout.visibility = View.GONE
            onboardingLayout.visibility = View.VISIBLE
        }
    }

    private fun setErrorMode() {
        setPendingMode()
        binding.apply {
            pendingFragmentContainer.pendingLayout.visibility = View.GONE
            pendingFragmentContainer.errorLayout.visibility = View.VISIBLE
            accountRetryButton.visibility = View.VISIBLE
            accountRemoveButton.visibility = View.VISIBLE
        }
    }

    private fun setPendingMode() {
        binding.apply {
            pendingFragmentContainer.pendingLayout.visibility = View.VISIBLE
            tokensFragmentContainer.visibility = View.GONE
            onboardingLayout.visibility = View.GONE
        }
    }

    private fun updateWhenResumed() {
        viewModelAccountDetails.updateState()
        viewModelAccountDetails.populateTransferList()
        viewModelAccountDetails.initiateFrequentUpdater()
        updateBannersVisibility()
    }

    private fun resetWhenPaused() {
        if (
            mainViewModel.activeAccountAddress.value.isNotEmpty() &&
            mainViewModel.notificationToken.value != null
        ) {
            mainViewModel.setNotificationData("", null)
        }
        viewModelAccountDetails.stopFrequentUpdater()
    }

    private fun initSwipeToRefresh() {
        binding.swipeLayout.setOnRefreshListener {
            updateWhenResumed()
            binding.swipeLayout.isRefreshing = false
        }
    }

    private fun initContainer() {
        var lastSetHeight = -1
        binding.scrollView.viewTreeObserver.addOnGlobalLayoutListener {
            val heightToSet = binding.scrollView.measuredHeight

            if (lastSetHeight != heightToSet) {
                binding.tokensFragmentContainer.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = heightToSet
                }

                lastSetHeight = heightToSet
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

    private fun cancelAnimation() {
        viewModelAccountDetails.setHasShownInitialAnimation()
        binding.confettiAnimation.visibility = View.GONE
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    private fun initBanners() {
        binding.includeOnrampBanner.root.setOnClickListener {
            mainViewModel.onOnrampClicked()
        }

        binding.includeOnrampBanner.closeImageView.setOnClickListener {
            binding.includeOnrampBanner.root.isVisible = false
            viewModelAccountDetails.onCloseOnrampBannerClicked()
        }

        binding.includeEarnBanner.root.setOnClickListener {
            mainViewModel.onEarnClicked()
        }

        binding.includeEarnBanner.closeImageView.setOnClickListener {
            binding.includeEarnBanner.root.isVisible = false
            viewModelAccountDetails.onCloseEarnBannerClicked()
        }

        binding.includeSeedPhraseBackupBanner.root.setOnClickListener {
            gotoSeedPhraseReveal()
        }

        binding.includeSeedPhraseBackupBanner.closeImageView.setOnClickListener {
            viewModelAccountDetails.onCloseSeedPhraseBackupBannerClicked()
        }

        updateBannersVisibility()
    }

    private fun initBalanceViews() {
        binding.balanceCard.bind(accountBalanceViewModel)
        binding.balanceCard.listener = object : AccountBalanceCardView.AccountBalanceCardListener {
            override fun onTokenDetailsClicked(token: CCDToken, account: Account) {
                gotoCCDTokenDetails(token, account)
            }

            override fun showUnlockFeatureDialog() {
                (requireActivity() as BaseActivity).showUnlockFeatureDialog()
            }
        }
    }

    private fun updateBannersVisibility() {
        binding.includeEarnBanner.root.isVisible =
            viewModelAccountDetails.isEarnBannerVisible

        binding.includeOnrampBanner.root.isVisible =
            viewModelAccountDetails.isOnrampBannerVisible

        binding.includeSeedPhraseBackupBanner.root.isVisible =
            viewModelAccountDetails.isSeedPhraseBackupBannerVisible
    }

    private fun showNotificationDialog() {
        NotificationsPermissionDialog().showSingle(
            childFragmentManager,
            NotificationsPermissionDialog.TAG
        )
    }

    private fun gotoSeedPhraseReveal() {
        val intent = Intent(activity, SavedSeedPhraseRevealActivity::class.java)
        startActivity(intent)
    }

    private fun gotoCCDTokenDetails(token: CCDToken, account: Account) {
        val intent = Intent(requireActivity(), TokenDetailsActivity::class.java).apply {
            putExtra(TokenDetailsActivity.TOKEN, token)
            putExtra(TokenDetailsActivity.ACCOUNT, account)
        }
        startActivity(intent)
    }
}
