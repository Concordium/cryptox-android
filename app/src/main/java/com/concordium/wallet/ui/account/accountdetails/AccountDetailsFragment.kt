package com.concordium.wallet.ui.account.accountdetails

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.model.TransactionStatus
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.ActivityAccountDetailsBinding
import com.concordium.wallet.ui.MainActivity
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity.Companion.RESULT_RETRY_ACCOUNT_CREATION
import com.concordium.wallet.ui.account.accountdetails.other.AccountDetailsErrorFragment
import com.concordium.wallet.ui.account.accountdetails.other.AccountDetailsPendingFragment
import com.concordium.wallet.ui.account.accountdetails.transfers.AccountDetailsTransfersActivity
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.account.accountslist.AccountsListActivity
import com.concordium.wallet.ui.account.accountsoverview.AccountsOverviewListItem
import com.concordium.wallet.ui.account.accountsoverview.AccountsOverviewViewModel
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.cis2.SendTokenActivity
import com.concordium.wallet.ui.cis2.TokenDetailsActivity
import com.concordium.wallet.ui.cis2.TokensFragment
import com.concordium.wallet.ui.cis2.TokensViewModel
import com.concordium.wallet.ui.common.delegates.EarnDelegate
import com.concordium.wallet.ui.common.delegates.EarnDelegateImpl
import com.concordium.wallet.ui.onramp.CcdOnrampSitesActivity
import java.math.BigInteger

class AccountDetailsFragment : BaseFragment(), EarnDelegate by EarnDelegateImpl() {

    private lateinit var binding: ActivityAccountDetailsBinding
    private lateinit var mainViewModel: MainViewModel
    private lateinit var viewModelAccountDetails: AccountDetailsViewModel
    private lateinit var viewModelTokens: TokensViewModel
    private lateinit var viewModelOverview: AccountsOverviewViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ActivityAccountDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModels()
        initializeViewModelTokens()

        val baseActivity = (activity as BaseActivity)

        baseActivity.hideLeftPlus(isVisible = true) {
            gotoAccountsList()
        }

        baseActivity.hideQrScan(isVisible = true) {
            if (mainViewModel.hasCompletedOnboarding()) {
                baseActivity.startQrScanner()
            } else {
                baseActivity.showUnlockFeatureDialog()
            }
        }
    }

//    override fun onResume() {
//        super.onResume()
//        viewModelAccountDetails.populateTransferList()
//        viewModelAccountDetails.initiateFrequentUpdater()
//    }

    override fun onPause() {
        super.onPause()
        viewModelAccountDetails.stopFrequentUpdater()
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
        viewModelOverview = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[AccountsOverviewViewModel::class.java]

        viewModelOverview.listItemsLiveData.observe(viewLifecycleOwner) {
            viewModelTokens.tokenData.account = viewModelAccountDetails.account
            viewModelTokens.loadTokensBalances()
            initViews()
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

//        viewModelAccountDetails.accountUpdatedLiveData.observe(viewLifecycleOwner) {
            // Update balances in the title and on the Tokens tab.
//            initTitle()
//            println("AccountDetailsFragment, initializeViewModels account: ${viewModelAccountDetails.account}")
//        }
    }

    private fun initializeViewModelTokens() {
        viewModelTokens = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[TokensViewModel::class.java]

//        viewModelTokens.tokenData.account = viewModelAccountDetails.account
//        viewModelTokens.loadTokensBalances()

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
        mainViewModel.setTitle("")
        showWaiting(false)
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
        initContainer()
    }

    private fun setFinalizedMode() {
        binding.onrampBtn.isEnabled = true
        binding.sendFundsBtn.isEnabled = !viewModelAccountDetails.account.readOnly
        binding.receiveBtn.isEnabled = true
        binding.earnBtn.isEnabled = !viewModelAccountDetails.account.readOnly
        binding.walletInfoCard.readonlyDesc.visibility =
            if (viewModelAccountDetails.account.readOnly) View.VISIBLE else View.GONE

        binding.walletInfoCard.accountsOverviewTotalDetailsBakerId.visibility = View.VISIBLE
        binding.walletInfoCard.disposalBlock.visibility = View.VISIBLE

        viewModelAccountDetails.account.isBaking().also { isBaking ->
            binding.walletInfoCard.stakedLabel.isVisible = isBaking
            binding.walletInfoCard.accountsOverviewTotalDetailsStaked.isVisible = isBaking
            binding.walletInfoCard.bakerIdLabel.isVisible = isBaking
            binding.walletInfoCard.accountsOverviewTotalDetailsBakerId.isVisible = isBaking
        }
        binding.walletInfoCard.accountsOverviewTotalDetailsStaked.text =
            CurrencyUtil.formatGTU(viewModelAccountDetails.account.stakedAmount)
        binding.walletInfoCard.accountsOverviewTotalDetailsBakerId.text =
            viewModelAccountDetails.account.baker?.bakerId?.toString()

        viewModelAccountDetails.account.isDelegating().also { isDelegating ->
            binding.walletInfoCard.delegatingLabel.isVisible = isDelegating
            binding.walletInfoCard.accountsOverviewTotalDetailsDelegating.isVisible = isDelegating
        }
        binding.walletInfoCard.accountsOverviewTotalDetailsDelegating.text =
            CurrencyUtil.formatGTU(viewModelAccountDetails.account.delegatedAmount)

        viewModelAccountDetails.account.hasCooldowns().also { hasCooldowns ->
            binding.walletInfoCard.cooldownLabel.isVisible = hasCooldowns
            binding.walletInfoCard.accountsOverviewTotalDetailsCooldown.isVisible = hasCooldowns
        }
        binding.walletInfoCard.accountsOverviewTotalDetailsCooldown.text =
            CurrencyUtil.formatGTU(viewModelAccountDetails.account.cooldownAmount)
        setupOnrampBanner(active = true)
    }

    private fun setErrorMode() {
        setPendingMode()
        binding.accountRetryButton.visibility = View.VISIBLE
        binding.accountRemoveButton.visibility = View.VISIBLE
    }

    private fun setPendingMode() {
        binding.onrampBtn.isEnabled = false
        binding.sendFundsBtn.isEnabled = false
        binding.receiveBtn.isEnabled = false
        binding.earnBtn.isEnabled = false
        binding.activityBtn.isEnabled = false
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
            val onRampHeight = binding.onrampBanner.root.measuredHeight
            val buttonsMargin =
                (binding.tokensFragmentContainer.layoutParams as MarginLayoutParams).topMargin
            val onRampMargin =
                (binding.onrampBanner.root.layoutParams as MarginLayoutParams).topMargin

            if (handledContainerHeight != containerHeight) {
                handledContainerHeight = containerHeight

                binding.tokensFragmentContainer.updateLayoutParams<ViewGroup.LayoutParams> {
                    height =
                        containerHeight - buttonsHeight - buttonsMargin - onRampMargin - onRampHeight
                }
            }
        }
    }


    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

//    private fun goToAccountSettings() {
//        startActivity(Intent(requireActivity(), AccountSettingsActivity::class.java).apply {
//            putExtra(AccountSettingsActivity.EXTRA_ACCOUNT, viewModelAccountDetails.account)
//        })
//    }

    private fun showTotalBalance(totalBalance: BigInteger) {
        binding.walletInfoCard.totalBalanceTextview.text = getString(
            R.string.account_details_total_balance,
            CurrencyUtil.formatAndRoundGTU(
                value = totalBalance,
                roundDecimals = 2
            )
        )
        if (viewModelAccountDetails.account.balanceAtDisposal != totalBalance) {
            binding.walletInfoCard.atDisposalLabel.visibility = View.VISIBLE
            binding.walletInfoCard.atDisposalLabel.text = getString(
                R.string.account_details_balance_at_disposal,
                CurrencyUtil.formatGTU(viewModelAccountDetails.account.balanceAtDisposal)
            )
        } else {
            binding.walletInfoCard.atDisposalLabel.visibility = View.GONE
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
        val intent = Intent(requireActivity(), TokenDetailsActivity::class.java)
        intent.putExtra(TokenDetailsActivity.ACCOUNT, viewModelAccountDetails.account)
        intent.putExtra(TokenDetailsActivity.TOKEN, token)
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
}