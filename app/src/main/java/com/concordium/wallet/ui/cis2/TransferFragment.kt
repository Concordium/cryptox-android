package com.concordium.wallet.ui.cis2

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.concordium.wallet.R
import com.concordium.wallet.databinding.FragmentTransferContainerBinding
import com.concordium.wallet.ui.account.accountqrcode.ReceiveFragment
import com.concordium.wallet.ui.cis2.send.SendTokenFragment
import com.concordium.wallet.uicore.view.GradientTabsView

class TransferFragment: Fragment() {

    private lateinit var binding: FragmentTransferContainerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransferContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.transferTabs.clearAll()
        binding.transferTabs.addControl(
            label = getString(R.string.account_details_send),
            icon = requireContext().getDrawable(R.drawable.mw24_ic_send_btn),
            clickListener = object : GradientTabsView.OnItemClickListener {
                override fun onItemClicked() {
                    replaceFragment(SendTokenFragment())
                }
            },
            initiallySelected = true,
        )
        binding.transferTabs.addControl(
            label = getString(R.string.account_details_receive),
            icon = requireContext().getDrawable(R.drawable.mw24_ic_receive_btn),
            clickListener = object : GradientTabsView.OnItemClickListener {
                override fun onItemClicked() {
                    replaceFragment(ReceiveFragment())
                }
            },
            initiallySelected = false,
        )
    }

    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }
}