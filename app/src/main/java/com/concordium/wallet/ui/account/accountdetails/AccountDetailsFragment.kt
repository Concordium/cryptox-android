package com.concordium.wallet.ui.account.accountdetails

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityAccountDetailsBinding
import com.concordium.wallet.databinding.FragmentOnboardingBinding
import com.concordium.wallet.extension.collectWhenStarted
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.account.accountdetails.other.AccountDetailsErrorFragment
import com.concordium.wallet.ui.account.accountdetails.other.AccountDetailsPendingFragment
import com.concordium.wallet.ui.account.accountdetails.transfers.AccountDetailsTransfersActivity
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.account.accountslist.AccountsListActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.cis2.SendTokenActivity
import com.concordium.wallet.ui.cis2.TokenDetailsActivity
import com.concordium.wallet.ui.cis2.TokensFragment
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.concordium.wallet.ui.common.delegates.EarnDelegate
import com.concordium.wallet.ui.common.delegates.EarnDelegateImpl
import com.concordium.wallet.ui.onboarding.OnboardingFragment
import com.concordium.wallet.ui.onboarding.OnboardingSharedViewModel
import com.concordium.wallet.ui.onboarding.OnboardingState
import com.concordium.wallet.ui.onramp.CcdOnrampSitesActivity
import com.concordium.wallet.util.ImageUtil
import java.math.BigInteger

class AccountDetailsFragment : BaseFragment(), EarnDelegate by EarnDelegateImpl() {

    private lateinit var binding: ActivityAccountDetailsBinding
    private lateinit var mainViewModel: MainViewModel
    private lateinit var viewModelAccountDetails: AccountDetailsViewModel
    private lateinit var viewModelTokens: TokensViewModel
    private lateinit var onboardingViewModel: OnboardingSharedViewModel
    private lateinit var onboardingStatusCard: OnboardingFragment
    private lateinit var onboardingBinding: FragmentOnboardingBinding
    // parameter for dynamic calculation of tokensFragmentContainer height
    private var isFileWallet: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivityAccountDetailsBinding.inflate(inflater, container, false)
        onboardingBinding = FragmentOnboardingBinding.inflate(layoutInflater)
        onboardingStatusCard = binding.onboardingLayout

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTooltipBanner()
        initializeViewModels()
        initializeViewModelTokens()
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
        viewModelAccountDetails.populateTransferList()
        viewModelAccountDetails.updateState()
        viewModelAccountDetails.initiateFrequentUpdater()
    }

    override fun onPause() {
        super.onPause()
        viewModelAccountDetails.stopFrequentUpdater()
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
                    viewModelAccountDetails.updateAccount()
                    viewModelAccountDetails.initiateFrequentUpdater()
                    showStateDefault()
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
        viewModelAccountDetails.totalBalanceLiveData.observe(viewLifecycleOwner, ::showTotalBalance)

        viewModelAccountDetails.newAccount.collectWhenStarted(viewLifecycleOwner) { account ->
            viewModelTokens.tokenData.account = account
            viewModelTokens.loadTokens(account.address)
            initViews()
            (requireActivity() as BaseActivity).hideAccountSelector(
                isVisible = true,
                text = account.getAccountName(),
                icon = ImageUtil.getIconById(requireContext(), account.iconId)
            ) {
                gotoAccountsList()
            }
        }

        viewModelAccountDetails.accountUpdatedLiveData.observe(viewLifecycleOwner) {
            initViews()
        }

        viewModelAccountDetails.newFinalizedAccountFlow.collectWhenStarted(viewLifecycleOwner) {
            if (it.isNotEmpty())
                setFinalizedMode()
        }

        viewModelAccountDetails.fileWalletMigrationVisible.collectWhenStarted(viewLifecycleOwner) {
            binding.fileWalletMigrationDisclaimerLayout.isVisible = it
            isFileWallet = it
        }

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

    private fun initializeViewModelTokens() {
        viewModelTokens = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[TokensViewModel::class.java]

        viewModelTokens.chooseToken.observe(viewLifecycleOwner) { token ->
            showTokenDetailsDialog(token)
        }

//        viewModelTokens.tokenBalances.observe(this, object : Observer<Boolean> {
//            override fun onChanged(t: Boolean) {
//                // Open the requested token once, when balances are loaded.
//                if (tokenToOpenUid != null) {
//                    viewModelTokens.tokenBalances.removeObserver(this)
//                    viewModelTokens.tokens
//                        .find { it.uid == tokenToOpenUid }
//                        ?.also(viewModelTokens.chooseToken::postValue)
//                }
//            }
//        })
    }

    private fun initViews() {
        showWaiting(false)
        initializeAnimation()

        binding.accountRetryButton.setOnClickListener {
            requireActivity().setResult(RESULT_RETRY_ACCOUNT_CREATION)
        }
        binding.accountRemoveButton.setOnClickListener {
            viewModelAccountDetails.deleteAccountAndFinish()
        }

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
            onEarnClicked()
        }

        binding.activityBtn.setOnClickListener {
            onActivityClicked()
        }

        replaceTokensFragment(getTokensFragment())

        when (viewModelAccountDetails.account.transactionStatus) {
            TransactionStatus.ABSENT -> {
                setErrorMode()
            }

            TransactionStatus.FINALIZED -> {
                setFinalizedMode()
            }

            TransactionStatus.COMMITTED -> setPendingMode()
            TransactionStatus.RECEIVED -> setPendingMode()
            else -> {
            }
        }
        initContainer()
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

    private fun setFinalizedMode() {
        binding.apply {
            onrampBtn.isEnabled = true
            sendFundsBtn.isEnabled = !viewModelAccountDetails.account.readOnly
            receiveBtn.isEnabled = true
            earnBtn.isEnabled = !viewModelAccountDetails.account.readOnly
            activityBtn.isEnabled = true
        }
        setupOnrampBanner(active = true)
    }

    private fun showStateOnboarding() {
        activity?.invalidateOptionsMenu()
        binding.onboardingLayout.visibility = View.VISIBLE
        binding.tokensFragmentContainer.visibility = View.GONE
        showOnboarding(true)
    }

    private fun showStateDefault() {
        activity?.invalidateOptionsMenu()
        binding.onboardingLayout.visibility = View.GONE
        binding.tokensFragmentContainer.visibility = View.VISIBLE
        showOnboarding(false)
    }

    private fun showOnboarding(show: Boolean) {
        val state = if (show) View.VISIBLE else View.GONE
        binding.onboardingLayout.visibility = state
    }

    private fun setErrorMode() {
        setPendingMode()
        binding.accountRetryButton.visibility = View.VISIBLE
        binding.accountRemoveButton.visibility = View.VISIBLE
    }

    private fun setPendingMode() {
        binding.apply {
            onrampBtn.isEnabled = false
            sendFundsBtn.isEnabled = false
            receiveBtn.isEnabled = false
            earnBtn.isEnabled = false
            activityBtn.isEnabled = false
        }
        setupOnrampBanner(active = false)
    }

    private fun getTokensFragment(): Fragment {
        return when (viewModelAccountDetails.account.transactionStatus) {
            TransactionStatus.ABSENT -> AccountDetailsErrorFragment()
            TransactionStatus.COMMITTED -> AccountDetailsPendingFragment()
            TransactionStatus.RECEIVED -> AccountDetailsPendingFragment()
            else -> TokensFragment()
        }
    }

    private fun replaceTokensFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(binding.tokensFragmentContainer.id, fragment)
            .commit()
    }

    private fun initContainer() {
        var handledContainerHeight = -1
        binding.scrollView.viewTreeObserver.addOnGlobalLayoutListener {
            val containerHeight = binding.scrollView.measuredHeight
            val buttonsHeight = binding.buttonsBlock.measuredHeight
            val fileWalletDisclaimerHeight =
                binding.fileWalletMigrationDisclaimerLayout.measuredHeight
            val onRampHeight = binding.onrampBanner.root.measuredHeight
            val buttonsMargin =
                (binding.tokensFragmentContainer.layoutParams as MarginLayoutParams).topMargin
            val fileWalletDisclaimerMargin =
                (binding.fileWalletMigrationDisclaimerLayout.layoutParams as MarginLayoutParams).topMargin
            val onRampMargin =
                (binding.onrampBanner.root.layoutParams as MarginLayoutParams).topMargin

            if (handledContainerHeight != containerHeight) {
                handledContainerHeight = containerHeight

                val scrollContainerHeight = containerHeight - buttonsHeight - buttonsMargin -
                        onRampMargin - onRampHeight - fileWalletDisclaimerHeight -
                        if (isFileWallet) fileWalletDisclaimerMargin else 0

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
        intent.putExtra(SendTokenActivity.ACCOUNT, viewModelAccountDetails.account)
        intent.putExtra(
            SendTokenActivity.TOKEN,
            Token.ccd(viewModelAccountDetails.account)
        )
        intent.putExtra(SendTokenActivity.PARENT_ACTIVITY, this::class.java.canonicalName)

        (requireActivity() as BaseActivity).startActivityForResultAndHistoryCheck(intent)
    }

    private fun onReceiveClicked() {
        val intent = Intent(requireActivity(), AccountQRCodeActivity::class.java)
        intent.putExtra(AccountQRCodeActivity.EXTRA_ACCOUNT, viewModelAccountDetails.account)
        startActivity(intent)
    }

    private fun onEarnClicked() {
        gotoEarn(
            requireActivity() as MainActivity,
            viewModelAccountDetails.account,
            viewModelAccountDetails.hasPendingDelegationTransactions,
            viewModelAccountDetails.hasPendingBakingTransactions
        )
    }

    private fun onActivityClicked() {
        val intent = Intent(requireActivity(), AccountDetailsTransfersActivity::class.java)
        intent.putExtra(
            AccountDetailsTransfersActivity.EXTRA_ACCOUNT,
            viewModelAccountDetails.account
        )
        startActivity(intent)
    }

    private fun showTokenDetailsDialog(token: Token) {
        val intent = Intent(requireActivity(), TokenDetailsActivity::class.java).apply {
            putExtra(TokenDetailsActivity.ACCOUNT, viewModelAccountDetails.account)
            putExtra(TokenDetailsActivity.TOKEN, token)
            putExtra(
                TokenDetailsActivity.PENDING_DELEGATION,
                viewModelAccountDetails.hasPendingDelegationTransactions
            )
            putExtra(
                TokenDetailsActivity.PENDING_VALIDATION,
                viewModelAccountDetails.hasPendingBakingTransactions
            )
        }
        showTokenDetails.launch(intent)
    }

    private fun setupOnrampBanner(active: Boolean) {
        binding.onrampBanner.root.setOnClickListener {
            if (active)
                onOnrampClicked()
            else
                (requireActivity() as BaseActivity).showUnlockFeatureDialog()
        }
    }

    private val showTokenDetails =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val isChanged =
                    it.data?.getBooleanExtra(TokenDetailsActivity.CHANGED, false) == true
                if (isChanged) {
                    viewModelTokens.updateWithSelectedTokensDone.postValue(true)
                }
            }
        }

    private fun gotoAccountsList() {
        val intent = Intent(activity, AccountsListActivity::class.java)
        startActivity(intent)
    }

    companion object {
        const val RESULT_RETRY_ACCOUNT_CREATION = 2
    }
}