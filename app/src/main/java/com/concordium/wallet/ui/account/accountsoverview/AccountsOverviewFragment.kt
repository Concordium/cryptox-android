package com.concordium.wallet.ui.account.accountsoverview

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.concordium.wallet.App
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.preferences.Preferences
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.FragmentAccountsOverviewBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.MainViewModel
import com.concordium.wallet.ui.account.accountdetails.AccountDetailsActivity
import com.concordium.wallet.ui.account.accountslist.AccountsListActivity
import com.concordium.wallet.ui.base.BaseFragment
import com.concordium.wallet.ui.more.export.ExportActivity
import com.concordium.wallet.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AccountsOverviewFragment : BaseFragment() {

    companion object {
        private const val REQUEST_CODE_ACCOUNT_DETAILS = 2000
    }

    private var eventListener: Preferences.Listener? = null
    private lateinit var binding: FragmentAccountsOverviewBinding
    private lateinit var viewModel: AccountsOverviewViewModel
    private lateinit var mainViewModel: MainViewModel

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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
        initializeViews()
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

        viewModel.waitingLiveData.observe(viewLifecycleOwner) { waiting ->
            waiting?.let {
                Log.d("waiting:$waiting")
                showWaiting(waiting)
                binding.accountRecyclerview.isClickable = waiting
            }
        }
        viewModel.errorLiveData.observe(viewLifecycleOwner, object : EventObserver<Int>() {
            override fun onUnhandledEvent(value: Int) {
                showError(value)
            }
        })
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
    }

    private fun initializeViews() {
        binding.progress.progressLayout.visibility = View.VISIBLE

        binding.missingBackup.setOnClickListener {
            gotoExport()
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

    private fun updateMissingBackup() = viewModel.viewModelScope.launch(Dispatchers.Main) {
        binding.missingBackup.isVisible = App.appCore.session.run {
            isAccountsBackupPossible() && !areAccountsBackedUp()
        }
    }

    private fun initializeList() {
        val adapter = AccountsOverviewItemAdapter(
            accountViewClickListener = object : AccountView.OnItemClickListener {
                override fun onCardClicked(account: Account) {
                    selectAccount(account)
                }
            },
        )
        binding.accountRecyclerview.adapter = adapter
        viewModel.listItemsLiveData.observe(viewLifecycleOwner, adapter::setData)
    }
    //endregion

    //region Control/UI
    // ************************************************************

    private fun gotoExport() {
        val intent = Intent(activity, ExportActivity::class.java)
        startActivity(intent)
    }

    private fun gotoCreateAccount() {
        val intent = Intent(activity, AccountsListActivity::class.java)
        startActivity(intent)
    }

    private fun selectAccount(item: Account) {
        viewModel.activateAccount(item.address)
        requireActivity().finish()
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
}
