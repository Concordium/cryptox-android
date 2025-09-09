package com.concordium.wallet.ui.account.accountdetails

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.AccelerateInterpolator
import android.widget.ListPopupWindow.WRAP_CONTENT
import android.widget.PopupWindow
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.core.rating.ReviewHelper
import com.concordium.wallet.data.model.CCDToken
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityAccountDetailsBinding
import com.concordium.wallet.databinding.FragmentOnboardingBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.account.accountdetails.transfers.AccountDetailsTransfersActivity
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.account.accountslist.AccountsListActivity
import com.concordium.wallet.ui.account.accountsoverview.UnshieldingNoticeDialog
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.cis2.send.SendTokenActivity
import com.concordium.wallet.ui.common.delegates.EarnDelegate
import com.concordium.wallet.ui.common.delegates.EarnDelegateImpl
import com.concordium.wallet.ui.multiwallet.WalletsActivity
import com.concordium.wallet.ui.onboarding.OnboardingFragment
import com.concordium.wallet.ui.onboarding.OnboardingSharedViewModel
import com.concordium.wallet.ui.onboarding.OnboardingState
import com.concordium.wallet.ui.onramp.CcdOnrampSitesActivity
import com.concordium.wallet.util.ImageUtil
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import java.math.BigInteger

class AccountDetailsFragment : BaseFragment(), EarnDelegate by EarnDelegateImpl() {

    private lateinit var binding: ActivityAccountDetailsBinding
    private lateinit var mainViewModel: MainViewModel
    private lateinit var viewModelAccountDetails: AccountDetailsViewModel
    private lateinit var onboardingViewModel: OnboardingSharedViewModel
    private lateinit var onboardingStatusCard: OnboardingFragment
    private lateinit var onboardingBinding: FragmentOnboardingBinding
    private lateinit var reviewHelper: ReviewHelper

    // parameters for dynamic calculation of tokensFragmentContainer height
    private var isFileWallet: Boolean = false
    private var isZeroBalance: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = ActivityAccountDetailsBinding.inflate(inflater, container, false)
        onboardingBinding = FragmentOnboardingBinding.inflate(layoutInflater)
        onboardingStatusCard = binding.onboardingLayout

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTooltipBanner()
        initViews()
        initializeViewModels()
        mainViewModel.setTitle("")

        val baseActivity = (activity as BaseActivity)

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
        updateWhenResumed()
    }

    override fun onPause() {
        super.onPause()
        resetWhenPaused()
    }

    private fun initTooltipBanner() {
        binding.tooltipButton.setOnClickListener {
            showTooltip(it)
        }
        listOf(binding.totalBalanceTextview, binding.atDisposalLabel).forEach {
            it.setOnClickListener {
                App.appCore.tracker.homeTotalBalanceClicked()
            }
        }
    }

    private fun initializeViewModels() {
        mainViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[MainViewModel::class.java]

        viewModelAccountDetails = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[AccountDetailsViewModel::class.java]

        onboardingViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[OnboardingSharedViewModel::class.java]

        viewModelAccountDetails.stateFlow.collectWhenStarted(viewLifecycleOwner) { state ->
            when (state) {
                OnboardingState.DONE -> {
                    viewModelAccountDetails.initiateFrequentUpdater()
                    updateViews(viewModelAccountDetails.activeAccount.first())

                    if (!viewModelAccountDetails.hasShownInitialAnimation()) {
                        binding.confettiAnimation.visibility = View.VISIBLE
                        binding.confettiAnimation.playAnimation()
                    }
                }

                else -> {
                    onboardingStatusCard.updateViewsByState(state)
                    showStateOnboarding()
                }
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
        viewModelAccountDetails.goToEarnLiveData.observe(
            viewLifecycleOwner,
            object : EventObserver<AccountDetailsViewModel.GoToEarnPayload>() {
                override fun onUnhandledEvent(value: AccountDetailsViewModel.GoToEarnPayload) {
                    gotoEarn(
                        activity = requireActivity() as BaseActivity,
                        account = value.account,
                        hasPendingDelegationTransactions = value.hasPendingDelegationTransactions,
                        hasPendingBakingTransactions = value.hasPendingBakingTransactions,
                    )
                }
            })
        viewModelAccountDetails.totalBalanceLiveData.observe(viewLifecycleOwner) {
            showTotalBalance(it)
            updateBannersVisibility(viewModelAccountDetails.account)
        }

        viewModelAccountDetails.activeAccount.collectWhenStarted(viewLifecycleOwner) { account ->
            updateViews(account)
            (requireActivity() as BaseActivity).hideAccountSelector(
                isVisible = true,
                text = account.getAccountName(),
                icon = ImageUtil.getIconById(requireContext(), account.iconId)
            ) {
                gotoAccountsList()
            }
            isZeroBalance = account.balance == BigInteger.ZERO
        }

        viewModelAccountDetails.fileWalletMigrationVisible.collectWhenStarted(viewLifecycleOwner) {
            binding.fileWalletMigrationDisclaimerLayout.isVisible = it
            isFileWallet = it
        }

        viewModelAccountDetails.suspensionNotice.collectWhenStarted(viewLifecycleOwner) { notice ->
            binding.earnBtnNotice.isVisible = notice != null && notice.isCurrentAccountAffected
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

        combine(
            mainViewModel.activeAccountAddress,
            mainViewModel.notificationToken,
            viewModelAccountDetails.activeAccount
        ) { notificationAddress, token, currentAccount ->
            if (notificationAddress == currentAccount.address && token != null) {
                viewModelAccountDetails.updateNotificationToken(token)
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)

        onboardingViewModel.identityFlow.collectWhenStarted(viewLifecycleOwner) { identity ->
            onboardingStatusCard.updateViewsByIdentityStatus(identity)
        }
        onboardingViewModel.updateState.collectWhenStarted(viewLifecycleOwner) { update ->
            if (update)
                viewModelAccountDetails.updateState()
        }
        onboardingViewModel.showLoading.collectWhenStarted(viewLifecycleOwner) { show ->
            showWaiting(show)
        }
    }

    private fun initViews() {
        showWaiting(true)
        initializeAnimation()
        initSwipeToRefresh()
        binding.accountRetryButton.setOnClickListener {
            gotoAccountsList()
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

    private fun updateViews(account: Account) {
        showWaiting(false)

        when (account.transactionStatus) {
            TransactionStatus.ABSENT -> setErrorMode()
            TransactionStatus.FINALIZED -> setFinalizedMode(account)
            TransactionStatus.COMMITTED,
            TransactionStatus.RECEIVED,
            -> setPendingMode()

            else -> {
                showStateDefault(account)
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showTooltip(anchorView: View) {
        val inflater = LayoutInflater.from(anchorView.context)
        val popupView = inflater.inflate(R.layout.tooltip_layout, null)

        val popupWindow = PopupWindow(
            popupView,
            binding.rootLayout.width / 2,
            WRAP_CONTENT,
            true
        )

        anchorView.doOnLayout {
            popupWindow.showAsDropDown(anchorView)
        }
    }

    private fun setFinalizedMode(account: Account) {
        setActiveButtons()
        binding.apply {
            onrampBanner.isVisible = viewModelAccountDetails.isShowOnrampBanner() &&
                    account.balance == BigInteger.ZERO
            tokensFragmentContainer.visibility = View.VISIBLE
            pendingFragmentContainer.pendingLayout.visibility = View.GONE
            pendingFragmentContainer.errorLayout.visibility = View.GONE
            onboardingLayout.visibility = View.GONE
            onrampBtn.isEnabled = true
            sendFundsBtn.isEnabled = !account.readOnly
            receiveBtn.isEnabled = true
            earnBtn.isEnabled = !account.readOnly
            activityBtn.isEnabled = true
        }
        setupOnrampBanner(active = true)
        setupEarnBanner(account)
    }

    private fun showStateOnboarding() {
        setPendingButtons()
        binding.apply {
            onrampBanner.isVisible = viewModelAccountDetails.isShowOnrampBanner()
            tokensFragmentContainer.visibility = View.GONE
            pendingFragmentContainer.pendingLayout.visibility = View.GONE
            pendingFragmentContainer.errorLayout.visibility = View.GONE
            onboardingLayout.visibility = View.VISIBLE
        }
    }

    private fun showStateDefault(account: Account) {
        setActiveButtons()
        setupEarnBanner(account)
        binding.apply {
            onrampBanner.isVisible = viewModelAccountDetails.isShowOnrampBanner() &&
                    account.balance == BigInteger.ZERO
            onboardingLayout.visibility = View.GONE
            tokensFragmentContainer.visibility = View.VISIBLE
            pendingFragmentContainer.pendingLayout.visibility = View.GONE
            pendingFragmentContainer.errorLayout.visibility = View.GONE
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
        setPendingButtons()
        binding.apply {
            onrampBanner.isVisible = viewModelAccountDetails.isShowOnrampBanner()
            includeEarnBanner.earnBanner.isVisible = false
            pendingFragmentContainer.pendingLayout.visibility = View.VISIBLE
            tokensFragmentContainer.visibility = View.GONE
            onboardingLayout.visibility = View.GONE
        }
        setupOnrampBanner(active = false)
    }

    private fun setPendingButtons() {
        binding.apply {
            listOf(
                onrampBtn,
                sendFundsBtn,
                receiveBtn,
                earnBtn,
                activityBtn
            ).forEach {
                it.setOnClickListener {
                    (activity as BaseActivity).showUnlockFeatureDialog()
                }
            }
        }
    }

    private fun setActiveButtons() {
        binding.onrampBtn.setOnClickListener {
            onOnrampClicked()
        }

        binding.sendFundsBtn.setOnClickListener {
            onSendFundsClicked()
        }

        binding.receiveBtn.setOnClickListener {
            onReceiveClicked()
        }

        binding.earnBtn.setOnClickListener {
            viewModelAccountDetails.onEarnClicked()
        }

        binding.activityBtn.setOnClickListener {
            onActivityClicked()
        }
    }

    private fun updateWhenResumed() {
        viewModelAccountDetails.updateState()
        viewModelAccountDetails.populateTransferList()
        viewModelAccountDetails.initiateFrequentUpdater()
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

    private fun initContainer(isEarning: Boolean = false) {
        var handledContainerHeight = -1
        binding.scrollView.viewTreeObserver.addOnGlobalLayoutListener {
            val containerHeight = binding.scrollView.measuredHeight
            val buttonsHeight = binding.buttonsBlock.measuredHeight
            val fileWalletDisclaimerHeight =
                binding.fileWalletMigrationDisclaimerLayout.measuredHeight
            val onRampHeight = if (viewModelAccountDetails.isShowOnrampBanner() && isZeroBalance)
                binding.onrampBanner.measuredHeight
            else 0
            val buttonsMargin =
                (binding.tokensFragmentContainer.layoutParams as MarginLayoutParams).topMargin
            val fileWalletDisclaimerMargin = if (isFileWallet)
                (binding.fileWalletMigrationDisclaimerLayout.layoutParams as MarginLayoutParams).topMargin
            else 0
            val onRampMargin = if (viewModelAccountDetails.isShowOnrampBanner() && isZeroBalance)
                (binding.onrampBanner.layoutParams as MarginLayoutParams).topMargin
            else 0
            val earnBannerHeight = if (viewModelAccountDetails.isShowEarnBanner() && !isZeroBalance)
                binding.includeEarnBanner.earnBanner.measuredHeight
            else 0
            val earnBannerMargin = if (viewModelAccountDetails.isShowEarnBanner() &&
                !isZeroBalance && !isEarning
            )
                (binding.includeEarnBanner.earnBanner.layoutParams as MarginLayoutParams).topMargin
            else 0

            if (handledContainerHeight != containerHeight) {
                handledContainerHeight = containerHeight

                val scrollContainerHeight = containerHeight - buttonsHeight - buttonsMargin -
                        fileWalletDisclaimerHeight - fileWalletDisclaimerMargin -
                        onRampHeight - onRampMargin - earnBannerHeight - earnBannerMargin

                binding.tokensFragmentContainer.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = scrollContainerHeight
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

    private fun showTotalBalance(totalBalance: BigInteger) {
        binding.totalBalanceTextview.text = getString(
            R.string.account_details_total_balance,
            CurrencyUtil.formatAndRoundGTU(
                value = totalBalance,
                roundDecimals = 2
            )
        )
        if (viewModelAccountDetails.account.balanceAtDisposal != totalBalance) {
            binding.atDisposalLabel.visibility = View.VISIBLE
            binding.atDisposalLabel.text = getString(
                R.string.account_details_balance_at_disposal,
                CurrencyUtil.formatAndRoundGTU(
                    value = viewModelAccountDetails.account.balanceAtDisposal,
                    roundDecimals = 2
                )
            )
        } else {
            binding.atDisposalLabel.visibility = View.GONE
        }
    }

    private fun onOnrampClicked() {
        val intent = Intent(requireActivity(), CcdOnrampSitesActivity::class.java)
        intent.putExtras(
            CcdOnrampSitesActivity.getBundle(
                accountAddress = viewModelAccountDetails.account.address,
            )
        )
        startActivity(intent)
    }

    private fun onSendFundsClicked() {
        val intent = Intent(requireActivity(), SendTokenActivity::class.java)
        intent.putExtra(
            SendTokenActivity.ACCOUNT,
            viewModelAccountDetails.account
        )
        intent.putExtra(
            SendTokenActivity.TOKEN,
            CCDToken(
                account = viewModelAccountDetails.account,
                eurPerMicroCcd = null,
            )
        )
        intent.putExtra(SendTokenActivity.PARENT_ACTIVITY, this::class.java.canonicalName)

        (requireActivity() as BaseActivity).startActivityForResultAndHistoryCheck(intent)
    }

    private fun onReceiveClicked() {
        val intent = Intent(requireActivity(), AccountQRCodeActivity::class.java)
        intent.putExtra(AccountQRCodeActivity.EXTRA_ACCOUNT, viewModelAccountDetails.account)
        startActivity(intent)
    }

    private fun onActivityClicked() {
        val intent = Intent(requireActivity(), AccountDetailsTransfersActivity::class.java)
        intent.putExtra(
            AccountDetailsTransfersActivity.EXTRA_ACCOUNT,
            viewModelAccountDetails.account
        )
        startActivity(intent)
    }

    private fun setupOnrampBanner(active: Boolean) {
        binding.onrampBanner.setOnClickListener {
            if (active)
                onOnrampClicked()
            else
                (requireActivity() as BaseActivity).showUnlockFeatureDialog()
        }
        binding.closeImageView.setOnClickListener {
            if (active) {
                closeOnrampBanner()
            }
        }
    }

    private fun closeOnrampBanner() {
        binding.onrampBanner.visibility = View.GONE
        viewModelAccountDetails.setShowOnrampBanner(false)
        setupEarnBanner()
    }

    private fun setupEarnBanner(account: Account = viewModelAccountDetails.account) {
        binding.includeEarnBanner.earnBanner.isVisible =
            viewModelAccountDetails.isEarnBannerVisible()
        binding.includeEarnBanner.earnBanner.setOnClickListener {
            viewModelAccountDetails.onEarnClicked()
        }
        binding.includeEarnBanner.closeImageView.setOnClickListener {
            closeEarnBanner()
        }
        updateScrollContainerView(isEarning = (account.isBaking() || account.isDelegating()))
    }

    private fun closeEarnBanner() {
        binding.includeEarnBanner.earnBanner.visibility = View.GONE
        viewModelAccountDetails.setShowEarnBanner(false)
        updateScrollContainerView()
    }

    private fun updateScrollContainerView(isEarning: Boolean = false) {
        binding.rootLayout.invalidate()
        binding.rootLayout.requestLayout()
        initContainer(isEarning)
    }

    private fun updateBannersVisibility(account: Account) {
        binding.includeEarnBanner.earnBanner.isVisible = (
                viewModelAccountDetails.isShowEarnBanner() &&
                        account.balance > BigInteger.ZERO &&
                        account.isDelegating().not() &&
                        account.isBaking().not()
                )

        binding.onrampBanner.isVisible = (
                viewModelAccountDetails.isShowOnrampBanner() &&
                        account.balance == BigInteger.ZERO
                )

        updateScrollContainerView(account.isDelegating() || account.isBaking())
    }

    private fun gotoAccountsList() {
        val intent = Intent(activity, AccountsListActivity::class.java)
        startActivity(intent)
    }
}
