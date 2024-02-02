package com.concordium.wallet.ui.walletconnect

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentWalletConnectProgressBinding

class WalletConnectProgressFragment : Fragment(R.layout.fragment_wallet_connect_progress) {

    val createdView: MutableLiveData<CreatedView> = MutableLiveData()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createdView.postValue(
            CreatedView(
                view = FragmentWalletConnectProgressBinding.bind(view),
                lifecycleOwner = viewLifecycleOwner
            )
        )
    }

    data class CreatedView(
        val view: FragmentWalletConnectProgressBinding,
        val lifecycleOwner: LifecycleOwner,
    )
}
