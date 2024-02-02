package com.concordium.wallet.ui.walletconnect

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentWalletConnectAccountSelectionBinding

class WalletConnectAccountSelectionFragment :
    Fragment(R.layout.fragment_wallet_connect_account_selection) {

    val createdView: MutableLiveData<CreatedView> = MutableLiveData()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createdView.postValue(
            CreatedView(
                view = FragmentWalletConnectAccountSelectionBinding.bind(view),
                lifecycleOwner = viewLifecycleOwner
            )
        )
    }

    data class CreatedView(
        val view: FragmentWalletConnectAccountSelectionBinding,
        val lifecycleOwner: LifecycleOwner,
    )
}
