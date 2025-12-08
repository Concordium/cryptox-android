package com.concordium.wallet.ui.account.accountsoverview

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.FragmentAccountsOverviewBinding
import com.concordium.wallet.extension.showSingle
import com.concordium.wallet.ui.account.accountdetails.AccountSettingsActivity
import com.concordium.wallet.ui.account.newaccountname.NewAccountNameActivity
import com.concordium.wallet.ui.multiwallet.FileWalletCreationLimitationDialog
import com.concordium.wallet.uicore.popup.Popup
import com.concordium.wallet.uicore.toast.ToastType
import com.concordium.wallet.uicore.toast.showCustomToast
import com.concordium.wallet.util.ClipboardUtil
import com.concordium.wallet.util.Log
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AccountsListFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentAccountsOverviewBinding
    private lateinit var viewModel: AccountsOverviewViewModel
    private val isFileWallet: Boolean by lazy {
        arguments?.getBoolean(IS_FILE_WALLET) ?: false
    }

    private val bottomSheetDialog
        get() = (dialog as? BottomSheetDialog)

    override fun getTheme() = R.style.CCX_BottomSheetDialog

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

        bottomSheetDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog?.behavior?.skipCollapsed = true
        initializeViewModel()
        initializeViews()
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateState()
        viewModel.initiateUpdater()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopUpdater()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[AccountsOverviewViewModel::class.java]

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
    }

    private fun initializeViews() {
        initializeList()
        binding.createAccountButton.setOnClickListener {
            gotoCreateAccount()
        }
    }

    private fun initializeList() {
        val adapter = AccountsOverviewItemAdapter(
            accountViewClickListener = object : AccountView.OnItemClickListener {
                override fun onCardClicked(account: Account) {
                    selectAccount(account)
                }

                override fun onSettingsClicked(account: Account) {
                    gotoAccountSettings(account)
                }

                override fun onLongClick(address: String) {
                    copyAddress(address)
                }
            })
        binding.accountRecyclerview.adapter = adapter
        viewModel.listItemsLiveData.observe(viewLifecycleOwner, adapter::setData)
    }

    private fun selectAccount(item: Account) {
        viewModel.activateAccount(item.address)
        dialog?.dismiss()
    }

    private fun showWaiting(waiting: Boolean) {
        if (waiting) {
            binding.progress.progressLayout.visibility = View.VISIBLE
        } else {
            binding.progress.progressLayout.visibility = View.GONE
        }
    }

    private fun showError(stringRes: Int) {
        Popup().showSnackbar(binding.root, stringRes)
    }

    private fun copyAddress(address: String) {
        ClipboardUtil.copyToClipboard(
            context = requireContext(),
            text = address
        )

        requireActivity().showCustomToast(
            title = getString(R.string.account_settings_address_copied),
            toastType = ToastType.INFO
        )
    }

    private fun gotoCreateAccount() {
        if (isFileWallet) {
            FileWalletCreationLimitationDialog().showSingle(
                parentFragmentManager,
                FileWalletCreationLimitationDialog.TAG
            )
        } else {
            val intent = Intent(requireActivity(), NewAccountNameActivity::class.java)
            startActivity(intent)
        }
    }

    private fun gotoAccountSettings(account: Account) {
        val intent = Intent(requireActivity(), AccountSettingsActivity::class.java)
        intent.putExtra(AccountSettingsActivity.EXTRA_ACCOUNT, account)
        startActivity(intent)
    }

    companion object {
        const val TAG = "AccountsListFragment"
        private const val IS_FILE_WALLET = "is_file_wallet"

        fun newInstance(bundle: Bundle) = AccountsListFragment().apply {
            arguments = bundle
        }

        fun setBundle(isFileWallet: Boolean = false) = Bundle().apply {
            putBoolean(IS_FILE_WALLET, isFileWallet)
        }
    }
}
