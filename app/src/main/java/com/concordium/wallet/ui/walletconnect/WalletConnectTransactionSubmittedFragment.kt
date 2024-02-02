package com.concordium.wallet.ui.walletconnect

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentWalletConnectTransactionSubmittedFragmentBinding

class WalletConnectTransactionSubmittedFragment :
    Fragment(R.layout.fragment_wallet_connect_transaction_submitted_fragment) {

    val createdView: MutableLiveData<CreatedView> = MutableLiveData()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentWalletConnectTransactionSubmittedFragmentBinding.bind(view)
        binding.includeTransactionSubmittedHeader.transactionSubmitted.isVisible = true
        createdView.postValue(
            CreatedView(
                view = binding,
                lifecycleOwner = viewLifecycleOwner
            )
        )
    }

    data class CreatedView(
        val view: FragmentWalletConnectTransactionSubmittedFragmentBinding,
        val lifecycleOwner: LifecycleOwner,
    )
}
