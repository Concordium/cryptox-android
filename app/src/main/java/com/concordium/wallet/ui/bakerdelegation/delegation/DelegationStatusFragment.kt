package com.concordium.wallet.ui.bakerdelegation.delegation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.concordium.wallet.databinding.FragmentDelegationStatusBinding

class DelegationStatusFragment: Fragment() {

    private lateinit var binding: FragmentDelegationStatusBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDelegationStatusBinding.inflate(inflater, container, false)
        return binding.root
    }
}