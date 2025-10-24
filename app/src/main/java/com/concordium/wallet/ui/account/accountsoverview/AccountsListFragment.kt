package com.concordium.wallet.ui.account.accountsoverview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.concordium.wallet.R
import com.concordium.wallet.core.arch.EventObserver
import com.concordium.wallet.data.room.Account
import com.concordium.wallet.databinding.FragmentAccountsOverviewBinding
import com.concordium.wallet.uicore.popup.Popup
import com.concordium.wallet.util.Log
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AccountsListFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentAccountsOverviewBinding
    private lateinit var viewModel: AccountsOverviewViewModel

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

        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
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
        binding.progress.progressLayout.visibility = View.VISIBLE
        initializeList()
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

    companion object Companion {
        const val TAG = "AccountsListFragment"
    }
}
