package com.concordium.wallet.ui.walletconnect

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentWalletConnectIdentityProofRequestReviewBinding

class WalletConnectIdentityProofRequestReviewFragment :
    Fragment(R.layout.fragment_wallet_connect_identity_proof_request_review) {

    val createdView: MutableLiveData<CreatedView> = MutableLiveData()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createdView.postValue(
            CreatedView(
                view = FragmentWalletConnectIdentityProofRequestReviewBinding.bind(view),
                lifecycleOwner = viewLifecycleOwner
            )
        )
    }

    data class CreatedView(
        val view: FragmentWalletConnectIdentityProofRequestReviewBinding,
        val lifecycleOwner: LifecycleOwner,
    )
}
