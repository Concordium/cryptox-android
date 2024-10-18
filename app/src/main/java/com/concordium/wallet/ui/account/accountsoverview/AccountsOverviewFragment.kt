package com.concordium.wallet.ui.account.accountsoverview

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.model.Token
import com.concordium.wallet.data.preferences.AuthPreferences
import com.concordium.wallet.data.preferences.Preferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.data.util.CurrencyUtil
import com.concordium.wallet.databinding.FragmentAccountsOverviewBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.account.accountqrcode.AccountQRCodeActivity
import com.concordium.wallet.ui.account.newaccountname.NewAccountNameActivity
import com.concordium.wallet.ui.base.BaseActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.cis2.SendTokenActivity
import com.concordium.wallet.ui.identity.identityproviderlist.IdentityProviderListActivity
import com.concordium.wallet.ui.more.export.ExportActivity
import com.concordium.wallet.ui.more.notifications.NotificationsPermissionDialog
import com.concordium.wallet.ui.onramp.CcdOnrampSitesActivity
import com.concordium.wallet.uicore.dialog.UnlockFeatureDialog
import com.concordium.wallet.util.KeyCreationVersion
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigInteger

class AccountsOverviewFragment : BaseFragment() {

    companion object {
        private const val REQUEST_CODE_ACCOUNT_DETAILS = 2000
    }

    private var eventListener: Preferences.Listener? = null

    private lateinit var binding: FragmentAccountsOverviewBinding
    private lateinit var viewModel: AccountsOverviewViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var keyCreationVersion: KeyCreationVersion

    //region Lifecycle
    // ************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
        keyCreationVersion = KeyCreationVersion(AuthPreferences(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccountsOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
        viewModel.initialize()
        initializeViews()

        (requireActivity() as BaseActivity).hideLeftPlus(isVisible = true) {
            gotoCreateAccount()
        }

        (requireActivity() as BaseActivity).hideQrScan(isVisible = true)

    }

    override fun onResume() {
        super.onResume()
        viewModel.updateState()
        viewModel.initiateFrequentUpdater()
        animateProgressBar(0, 66)
    }

    override fun onDestroy() {
        super.onDestroy()
        eventListener?.let {
            App.appCore.session.removeAccountsBackedUpListener(it)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopFrequentUpdater()
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

        viewModel.stateLiveData.observe(viewLifecycleOwner) { state ->
            when (state) {
                AccountsOverviewViewModel.State.NO_IDENTITIES -> showStateNoIdentities()
                AccountsOverviewViewModel.State.NO_ACCOUNTS -> showStateNoAccounts()
                AccountsOverviewViewModel.State.DEFAULT -> showStateDefault()
                else -> {
                }
            }
        }
        viewModel.totalBalanceLiveData.observe(viewLifecycleOwner) { totalBalance ->
            showTotalBalance(totalBalance.totalBalanceForAllAccounts)
            showDisposalBalance(totalBalance.totalAtDisposalForAllAccounts)
        }
        viewModel.showDialogLiveData.observe(viewLifecycleOwner) { event ->
            when (event.contentIfNotHandled) {
                AccountsOverviewViewModel.DialogToShow.UNSHIELDING -> {
                    UnshieldingNoticeDialog().showSingle(
                        childFragmentManager,
                        UnshieldingNoticeDialog.TAG
                    )
                }

                AccountsOverviewViewModel.DialogToShow.NOTIFICATIONS_PERMISSION -> {
                    NotificationsPermissionDialog().showSingle(
                        childFragmentManager,
                        NotificationsPermissionDialog.TAG,
                    )
                }

                null -> {}
            }
        }
    }

    private fun initializeViews() {
        mainViewModel.setTitle(getString(R.string.accounts_overview_title))
        binding.progress.progressLayout.visibility = View.VISIBLE
        binding.noIdentitiesLayout.visibility = View.GONE
        binding.noAccountsTextview.visibility = View.GONE
        binding.createAccountButton.visibility = View.GONE

        binding.createIdentityButton.setOnClickListener {
            viewModel.checkUsingV1KeyCreation()
            goToFirstIdentityCreation()
        }

        binding.createAccountButton.setOnClickListener {
            viewModel.checkUsingV1KeyCreation()
            gotoCreateAccount()
        }

        binding.missingBackup.setOnClickListener {
            gotoExport()
        }

        listOf(
            binding.onboardingStatusCard.onrampBtn,
            binding.onboardingStatusCard.sendFundsBtn,
            binding.onboardingStatusCard.addressBtn,
        ).forEach {
            it.setOnClickListener {
                UnlockFeatureDialog().showSingle(
                    childFragmentManager,
                    UnlockFeatureDialog.TAG
                )
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

        initializeList()

        updateMissingBackup()
    }

    private fun animateProgressBar(startValue: Int, newValue: Int) {
        lifecycleScope.launch {
            delay(500)

            // Create ObjectAnimator to animate the progress from the start value to the new value
            val progressAnimator = ObjectAnimator.ofInt(
                binding.onboardingStatusCard.onboardingStatusProgressBar,
                "progress",
                startValue,
                newValue
            )

            // Set animation duration to 1 second
            progressAnimator.duration = 1000

            // Set ease-in and ease-out interpolator
            progressAnimator.interpolator = AccelerateDecelerateInterpolator()
            progressAnimator.start()
        }
    }

    private fun updateMissingBackup() = viewModel.viewModelScope.launch(Dispatchers.Main) {
        binding.missingBackup.isVisible = App.appCore.session.run {
            isAccountsBackupPossible() && !isAccountsBackedUp()
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
            onCcdOnrampBannerClicked = {
                val intent = Intent(requireContext(), CcdOnrampSitesActivity::class.java)
                intent.putExtras(
                    CcdOnrampSitesActivity.getBundle(
                        accountAddress = null,
                    )
                )
                startActivity(intent)
            }
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

    //endregion

    //region Control/UI
    // ************************************************************

    private fun gotoExport() {
        val intent = Intent(activity, ExportActivity::class.java)
        startActivity(intent)
    }

    private fun goToFirstIdentityCreation() {
        val intent = Intent(requireContext(), IdentityProviderListActivity::class.java)
        intent.putExtra(IdentityProviderListActivity.SHOW_FOR_FIRST_IDENTITY, true)
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

    private fun showStateNoIdentities() {
        activity?.invalidateOptionsMenu()
        binding.noAccountsLayout.visibility = View.VISIBLE
        binding.accountRecyclerview.visibility = View.GONE
        showNoIdentities(true)
        showNoAccounts(false)
    }

    private fun showStateNoAccounts() {
        activity?.invalidateOptionsMenu()
        binding.noAccountsLayout.visibility = View.VISIBLE
        binding.accountRecyclerview.visibility = View.GONE
        showNoIdentities(false)
        showNoAccounts(true)
    }

    private fun showStateDefault() {
        activity?.invalidateOptionsMenu()
        binding.noAccountsLayout.visibility = View.GONE
        binding.accountRecyclerview.visibility = View.VISIBLE
        showNoIdentities(false)
        showNoAccounts(false)
    }

    private fun showNoIdentities(show: Boolean) {
        val state = if (show) View.VISIBLE else View.GONE
        binding.noIdentitiesLayout.visibility = state
    }

    private fun showNoAccounts(show: Boolean) {
        val state = if (show) View.VISIBLE else View.GONE
        binding.noAccountsTextview.visibility = state
        binding.createAccountButton.visibility = state
    }

    private fun showTotalBalance(totalBalance: BigInteger) {
        binding.totalBalanceTextview.text = CurrencyUtil.formatGTU(totalBalance)
    }

    private fun showDisposalBalance(atDisposal: BigInteger) {
        binding.accountsOverviewTotalDetailsDisposal.text = CurrencyUtil.formatGTU(atDisposal, true)
    }

    //endregion
}
